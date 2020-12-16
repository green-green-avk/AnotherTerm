//
// Created by alex on 4/16/19.
//

#include <sys/socket.h>
#include <sys/un.h>
#include <sys/user.h>
#include <sys/stat.h>
#include <errno.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <termios.h>
#include <signal.h>
#include <fcntl.h>

#include <android/log.h>

typedef struct {
    bool raw;
} options_t;

typedef struct {
    const size_t field_offset;

    void (*const field_setter)(void *const field_ptr, const char *const value);

    const char *const arg;
    const size_t arg_len;
} option_desc_t;

static void field_bool(void *const field_ptr, const char *const value) {
    *(bool *) field_ptr = true;
}

#define OPT_DESC(F, S, A) {offsetof(options_t, F), (S), (A), sizeof(A) - 1}

static const option_desc_t options_desc[] = {
        OPT_DESC(raw, field_bool, "-r"),
        OPT_DESC(raw, field_bool, "--raw")
};

static size_t getOpts(options_t *const options, const int argc, const char *const *const argv) {
    size_t i = 0;
    for (; i < argc; ++i) {
        for (int oi = 0; oi < (sizeof(options_desc) / sizeof(options_desc[0])); ++oi) {
            const option_desc_t *od = options_desc + oi;
            const char *const arg = argv[i];
            if (od->arg[od->arg_len - 1] == '=' ?
                strncmp(od->arg, arg, od->arg_len) == 0 :
                strcmp(od->arg, arg) == 0) {
                od->field_setter(options + od->field_offset, arg + od->arg_len);
                goto next;
            }
        }
        break;
        next:;
    }
    return i;
}

static ssize_t sendFds(const int sockfd, const void *const data, const size_t len,
                       const int *const fds, const size_t fdsc) {
    const size_t cmsg_space = CMSG_SPACE(sizeof(int) * fdsc);
    const size_t cmsg_len = CMSG_LEN(sizeof(int) * fdsc);
    if (cmsg_space >= PAGE_SIZE) {
        errno = ENOMEM;
        return -1;
    }
    alignas(struct cmsghdr) char cmsg_buf[cmsg_space];
    iovec iov = {.iov_base = (void *) data, .iov_len = len};
    const msghdr msg = {
            .msg_name = nullptr,
            .msg_namelen = 0,
            .msg_iov = &iov,
            .msg_iovlen = 1,
            .msg_control = cmsg_buf,
            .msg_controllen = sizeof(cmsg_buf),
            .msg_flags = 0
    };
    struct cmsghdr *const cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = cmsg_len;
    int *const cmsg_fds = reinterpret_cast<int *>(CMSG_DATA(cmsg));
    for (size_t i = 0; i < fdsc; ++i) {
        cmsg_fds[i] = fds[i];
    }
    return TEMP_FAILURE_RETRY(sendmsg(sockfd, &msg, MSG_NOSIGNAL));
}

static void readAllOrExit(const int sock, void *const buf, const size_t len) {
    if (len == 0) return;
    size_t offset = 0;
    while (true) {
        const ssize_t r = read(sock, (char *) buf + offset, len - offset);
        if (r <= 0) {
            close(sock);
            perror("Error receiving data from termsh server");
            exit(1);
        }
        offset += r;
        if (offset >= len) return;
    }
}

static void writeAllOrExit(const int sock, const void *const buf, const size_t len) {
    if (len == 0) return;
    size_t offset = 0;
    while (true) {
        const ssize_t r = write(sock, (char *) buf + offset, len - offset);
        if (r <= 0) {
            close(sock);
            perror("Error sending data to termsh server");
            exit(1);
        }
        offset += r;
        if (offset >= len) return;
    }
}

static void sendFdsOrExit(const int sock, const void *const buf, const size_t len,
                          const int *const fds, const size_t fdsc) {
    const ssize_t r = sendFds(sock, buf, len, fds, fdsc);
    if (r <= 0) {
        close(sock);
        perror("Error sending data with fds to termsh server");
        exit(1);
    }
}

static bool saved_in = false;
static struct termios def_mode_in;
static bool saved_out = false;
static struct termios def_mode_out;

static void saveMode() {
    if (tcgetattr(STDIN_FILENO, &def_mode_in) == 0) saved_in = true;
    if (tcgetattr(STDOUT_FILENO, &def_mode_out) == 0) saved_out = true;
}

static void setRawMode() {
    if (saved_in) {
        struct termios mode_in = def_mode_in;
        cfmakeraw(&mode_in);
        tcsetattr(STDIN_FILENO, TCSANOW, &mode_in);
    }
    if (saved_out) {
        struct termios mode_out = def_mode_out;
        cfmakeraw(&mode_out);
        tcsetattr(STDOUT_FILENO, TCSANOW, &mode_out);
    }
}

static void restoreMode() {
    if (saved_in) tcsetattr(STDIN_FILENO, TCSANOW, &def_mode_in);
    if (saved_out) tcsetattr(STDOUT_FILENO, TCSANOW, &def_mode_out);
}

static void _onExit() {
    restoreMode();
}

static void _onSignalExit(int s) {
    exit(1);
}

#define CMD_EXIT 0
#define CMD_OPEN 1

static uint64_t getenvuqOrExit(const char *const name) {
    const char *const vs = getenv(name);
    if (vs == nullptr || *vs == '\0') return 0;
    char *p;
    uint64_t v = strtoull(vs, &p, 16);
    if (*p != '\0') {
        fprintf(stderr, "Suspiciously bad formatted %s env variable...\n", name);
        __android_log_print(ANDROID_LOG_ERROR, "termsh",
                            "Suspiciously bad formatted %s env variable...\n", name);
        exit(1);
    }
    return v;
}

/*
 * It seems, tty descriptors cannot be passed via local domain sockets if O_APPEND is set.
 * GNU Make sets O_APPEND onto stdout, so trying not to break it but resolve it...
 * It seems, we can't just dup() here: O_APPEND is shared by all the duplicated descriptors
 * in all processes...
 * TODO: review this hack
 */
static int fixFd(const int fd) {
    if (isatty(fd)) {
        char fn[PATH_MAX];
        snprintf(fn, sizeof(fn), "/proc/self/fd/%u", fd);
        const int r = open(fn, O_RDWR);
        if (r != -1) return r;
    }
    return fd;
}

int main(const int argc, const char *const *const argv) {

    // Another Term shell session token associated with related data
    // including termsh permissions.
    const uint64_t shellSessionToken = getenvuqOrExit(SHELL_SESSION_TOKEN_VAR);

    options_t options = {.raw = false};

    int c_argc = argc - 1;
    const char *const *c_argv = argv + 1;
    const size_t args_offset = getOpts(&options, c_argc, c_argv);
    c_argc -= args_offset;
    c_argv += args_offset;

    if (c_argc > 127) {
        fprintf(stderr, "Too many arguments\n");
        exit(1);
    }

    saveMode();
    atexit(_onExit);
    signal(SIGTERM, _onSignalExit);
    signal(SIGINT, _onSignalExit);
    signal(SIGQUIT, _onSignalExit);
    signal(SIGHUP, _onSignalExit);
    signal(SIGPIPE, _onSignalExit);
    signal(SIGALRM, _onSignalExit);
    signal(SIGUSR1, _onSignalExit);
    signal(SIGUSR2, _onSignalExit);

    if (options.raw) setRawMode();

    const char socketName[] = "\0" APPLICATION_ID ".termsh";
    const int sock = socket(AF_LOCAL, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (sock < 0) {
        perror("Can't create socket");
        exit(1);
    }
    struct sockaddr_un sockAddr;
    sockAddr.sun_family = AF_LOCAL;
    memcpy(sockAddr.sun_path, socketName, sizeof(socketName));
    if (connect(sock, (struct sockaddr *) &sockAddr,
                sizeof(socketName) - 1 + offsetof(struct sockaddr_un, sun_path)) < 0) {
        close(sock);
        perror("Can't connect to termsh server");
        exit(1);
    }
    struct ucred cr;
    socklen_t cr_len = sizeof(cr);
    if (getsockopt(sock, SOL_SOCKET, SO_PEERCRED, &cr, &cr_len) != 0) {
        close(sock);
        perror("Can't check termsh server");
        exit(1);
    }
    const char *const uid_s = getenv("TERMSH_UID");
    int uid;
    if (!uid_s || (uid = atoi(uid_s)) == 0) uid = getuid();
    if (cr.uid != uid) {
        close(sock);
        fprintf(stderr, "Spoofing detected!\n");
        __android_log_write(ANDROID_LOG_ERROR, "termsh", "Spoofing detected!");
        exit(1);
    }
    {
        const uint64_t _st = htobe64(shellSessionToken);
        writeAllOrExit(sock, &_st, sizeof(_st));
    }
    {
        char buf[PATH_MAX];
        if (getcwd(buf, sizeof(buf)) == nullptr) {
            close(sock);
            perror("Error getting CWD");
            exit(1);
        }
        const size_t l = strlen(buf);
        const uint32_t _l = htonl(l); // always big-endian
        writeAllOrExit(sock, &_l, 4);
        writeAllOrExit(sock, buf, l);
    }
    // Android LocalSocket is good but still poorly implemented:
    // there is no poll() method and raw FD is also inaccessible...
    // This pipe is only for signals / exit tracking now:
    // the local socket is used purely to request this tool in order to resolve
    // anything sandbox (PRoot, fakechroot etc.) related.
    int ctlFds[2];
    if (pipe(ctlFds) < 0) {
        close(sock);
        perror("Error creating control pipe");
        exit(1);
    }
    const int fds[] = {fixFd(STDIN_FILENO), fixFd(STDOUT_FILENO), fixFd(STDERR_FILENO), ctlFds[0]};
    const int8_t _argc = (char) c_argc;
    sendFdsOrExit(sock, &_argc, sizeof(_argc), fds, sizeof(fds) / sizeof(fds[0]));
    for (int i = 0; i < c_argc; ++i) {
        const size_t l = strlen(c_argv[i]);
        const uint32_t _l = htonl(l); // always big-endian
        writeAllOrExit(sock, &_l, sizeof(_l));
        writeAllOrExit(sock, c_argv[i], l);
    }
    close(ctlFds[0]);
    while (true) {
        char cmd;
        readAllOrExit(sock, &cmd, sizeof(cmd));
        switch (cmd) {
            case CMD_EXIT: {
                char result;
                readAllOrExit(sock, &result, sizeof(result));
                close(sock);
                exit(result);
            }
            case CMD_OPEN: {
                int32_t flags;
                readAllOrExit(sock, &flags, sizeof(flags));
                flags = ntohl(flags);
                uint32_t name_len;
                readAllOrExit(sock, &name_len, sizeof(name_len));
                name_len = ntohl(name_len);
                char name[name_len + 1];
                readAllOrExit(sock, name, name_len);
                name[name_len] = '\0';
                const int r = open(name, flags, 00600);
                if (r == -1) {
                    const char result = -1;
                    writeAllOrExit(sock, &result, sizeof(result));
                    const int32_t err = htonl(errno);
                    writeAllOrExit(sock, &err, sizeof(err));
                } else {
                    const char result = 0;
                    sendFdsOrExit(sock, &result, sizeof(result), &r, 1);
                    close(r);
                }
                break;
            }
            default:
                fprintf(stderr, "Termsh server protocol error\n");
                exit(1);
        }
    }
}
