#pragma clang diagnostic push
#pragma ide diagnostic ignored "misc-misplaced-const"
//
// Created by alex on 12/23/18.
//

#include <jni.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <sys/mman.h>
#include <signal.h>
#include "pty_compat.h"
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <poll.h>

#include <android/log.h>

#include "common.h"

#define REQ_JNI_VERSION JNI_VERSION_1_6

#define CLASS_NAME PKG_NAME "/PtyProcess"

static const int IS_ERROR = -1;
static const int IS_RUNNING = -2;
static const int IS_EINTR = -3;

static jclass g_OOMEC;
static jclass g_runtimeEC;
static jclass g_IOEC;
static jclass g_IntIOEC;
static jclass g_illegalArgumentEC;

class cStrError {
private:
    HIDDEN char buf[1024] = "Error while obtaining error ;)";

public:
    HIDDEN inline cStrError(const char *const msg) {
        char *const b = (char *) memccpy(buf, msg, '\0', sizeof(buf));
        if (b == nullptr) buf[sizeof(buf) - 1] = '\0';
        else strerror_r(errno, b - 1, sizeof(buf) - (b - 1 - buf));
    }

    HIDDEN inline operator const char *() const { return buf; }
};

#define strError(M) cStrError(M ": ")

static jclass g_class;
static jmethodID g_ctrId;
static jfieldID g_fdPtmId;
static jfieldID g_pidId;
static jfieldID g_exitStatusId;

static void releaseStrings(const char *const *const strings) {
    if (strings == nullptr) return;
    for (const char *const *ss = strings; *ss; ++ss)
        free((void *) *ss);
    free((void *) strings);
}

// Java and pointless copying... Forever...
static char *const *getStrings(JNIEnv *const env, const jobjectArray array) {
    if (array == nullptr) return nullptr;
    const jsize len = env->GetArrayLength(array);
    if (env->ExceptionCheck() == JNI_TRUE) return nullptr;
    char **const ret = (char **) malloc(sizeof(char *) * (len + 1));
    if (ret == nullptr) {
        env->ThrowNew(g_OOMEC, "Out of memory");
        return nullptr;
    }
    jsize l = 0;
    for (jsize i = 0; i < len; ++i) {
        const jobject o = env->GetObjectArrayElement(array, i);
        if (env->ExceptionCheck() == JNI_TRUE) {
            ret[l] = nullptr;
            releaseStrings(ret);
            return nullptr;
        }
        if (o == nullptr) continue;
        const char *s = env->GetStringUTFChars((jstring) o, nullptr);
        if (env->ExceptionCheck() == JNI_TRUE) {
            ret[l] = nullptr;
            releaseStrings(ret);
            env->DeleteLocalRef(o);
            return nullptr;
        }
        if (s == nullptr) { // There is no clear spec on the GetStringUTFChars() behavior...
            ret[l] = nullptr;
            releaseStrings(ret);
            env->DeleteLocalRef(o);
            env->ThrowNew(g_OOMEC, "Out of memory");
            return nullptr;
        }
        ret[l] = strdup(s);
        if (ret[l] == nullptr) {
            releaseStrings(ret);
            env->ReleaseStringUTFChars((jstring) o, s);
            env->DeleteLocalRef(o);
            env->ThrowNew(g_OOMEC, "Out of memory");
            return nullptr;
        }
        env->ReleaseStringUTFChars((jstring) o, s);
        env->DeleteLocalRef(o);
        ++l;
    }
    ret[l] = nullptr;
    return ret;
}

static jobject JNICALL
m_execve(JNIEnv *const env, const jobject jthis,
         const jstring cmd_filename, const jobjectArray cmd_args, const jobjectArray cmd_env) {
    const char *const filename = env->GetStringUTFChars(cmd_filename, nullptr);
    if (env->ExceptionCheck() == JNI_TRUE) return nullptr;
    char *const *const args = getStrings(env, cmd_args);
    if (env->ExceptionCheck() == JNI_TRUE) {
        env->ReleaseStringUTFChars(cmd_filename, filename);
        return nullptr;
    }
    char *const *const envp = getStrings(env, cmd_env);
    if (env->ExceptionCheck() == JNI_TRUE) {
        env->ReleaseStringUTFChars(cmd_filename, filename);
        releaseStrings(args);
        return nullptr;
    }
    int fdPtm;
    const int pid = forkpty(&fdPtm, nullptr, nullptr, nullptr);
    if (pid < 0) {
        env->ReleaseStringUTFChars(cmd_filename, filename);
        releaseStrings(args);
        releaseStrings(envp);
        env->ThrowNew(g_runtimeEC, strError("Cannot fork into PTY"));
        return nullptr;
    }
    if (pid > 0) {
        env->ReleaseStringUTFChars(cmd_filename, filename);
        releaseStrings(args);
        releaseStrings(envp);
        return env->NewObject(g_class, g_ctrId, fdPtm, pid);
    }
    {
        /*
         * https://man7.org/linux/man-pages/man2/execve.2.html
         * ...
         * The dispositions of any signals that are being caught are reset to
         * the default (signal(7)).
         * ...
         *
         * It seems not exactly true for Android. (API >= 28 Samsung only?)
         */
        int sig = 1;
        while (sig <= SIGUNUSED) {
            const sighandler_t sh = signal(sig, SIG_DFL);
#ifdef DEBUG
            if (sh != SIG_DFL)
                __android_log_print(ANDROID_LOG_DEBUG, CLASS_NAME,
                                    "Signal %u was 0x%016lX", sig, (unsigned long) sh);
#endif
            sig++;
        }
        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, nullptr);

        // TODO: /proc/<pid>/fd seems better
        const int fdlimit = (int) (sysconf(_SC_OPEN_MAX));
        int fd = 3;
        while (fd < fdlimit) close(fd++);
    }
    if (envp == nullptr)
        execv(filename, args);
    else
        execve(filename, args, envp);
    fprintf(stderr, "[errno: %d] %s [%s]\n", errno, strError("Exec failed"), filename);
    _exit(127);
}

static jint
getExitStatus(JNIEnv *const env, const jobject jthis, const int pid, const int options) {
    jint exitStatus = env->GetIntField(jthis, g_exitStatusId);
    if (env->ExceptionCheck() == JNI_TRUE) return IS_ERROR;
    if (exitStatus >= 0) return exitStatus;
    int status = 0;
    const int r = waitpid(pid, &status, options);
    if (r == -1) exitStatus = (errno == EINTR) ? IS_EINTR : IS_ERROR;
    else if (r == 0) exitStatus = IS_RUNNING;
    else exitStatus = WEXITSTATUS(status);
    env->SetIntField(jthis, g_exitStatusId, exitStatus);
    return exitStatus;
}

static void JNICALL m_destroy(JNIEnv *const env, const jobject jthis) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (fdPtm < 0) return;
    const int pgid = tcgetpgrp(fdPtm);
    if (pgid > 0) killpg(pgid, SIGHUP);
    env->SetIntField(jthis, g_fdPtmId, -1);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    close(fdPtm);
}

static jint JNICALL m_waitForExit(JNIEnv *const env, const jobject jthis, const jint options) {
    const jint pid = env->GetIntField(jthis, g_pidId);
    if (env->ExceptionCheck() == JNI_TRUE) return IS_ERROR;
    return getExitStatus(env, jthis, pid, options & WNOHANG);
}

// https://www.win.tue.nl/~aeb/linux/lk/lk-10.html
static void JNICALL m_sendSignalToForeground(JNIEnv *const env, const jobject jthis,
                                             const jint sig) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (fdPtm < 0) return;
    const int pgid = tcgetpgrp(fdPtm);
    if (pgid < 1) return;
    killpg(pgid, sig);
}

static void JNICALL m_resize(JNIEnv *const env, const jobject jthis,
                             const jint w, const jint h, const jint wp, const jint hp) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (fdPtm < 0) {
        env->ThrowNew(g_IOEC, "fd < 0");
        return;
    }
    const struct winsize winp = {
            .ws_col = (unsigned short) w,
            .ws_row = (unsigned short) h,
            .ws_xpixel = (unsigned short) wp,
            .ws_ypixel = (unsigned short) hp
    };
    if (ioctl(fdPtm, TIOCSWINSZ, &winp) != 0)
        env->ThrowNew(g_IOEC, strError("Cannot set PTY size"));
}

static jint JNICALL m_readByte(JNIEnv *const env, const jobject jthis) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    if (fdPtm < 0) return -1;
    unsigned char b;
    const ssize_t r = read(fdPtm, &b, 1);
    if (r < 0) {
        env->ThrowNew(g_IOEC, strError("Cannot read from PTY"));
        return -1;
    }
    if (r == 0) return -1;
    return b;
}

static jint JNICALL m_readBuf(JNIEnv *const env, const jobject jthis,
                              const jbyteArray buf, const jint off, const jint len) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    if (fdPtm < 0) return -1;
    jbyte *const b = env->GetByteArrayElements(buf, nullptr);
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    const ssize_t r = read(fdPtm, b + off, len);
    if (r < 0) {
        env->ReleaseByteArrayElements(buf, b, JNI_ABORT);
        env->ThrowNew(g_IOEC, strError("Cannot read from PTY"));
        return -1;
    }
    if (r == 0) {
        env->ReleaseByteArrayElements(buf, b, JNI_ABORT);
        return -1;
    }
    env->ReleaseByteArrayElements(buf, b, 0);
    return (jint) r;
}

static void JNICALL m_writeByte(JNIEnv *const env, const jobject jthis, const jint b) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (fdPtm < 0) return;
    const ssize_t r = write(fdPtm, &b, 1);
    if (r < 0) {
        env->ThrowNew(g_IOEC, strError("Cannot write to PTY"));
    } else if (r < 1) {
        env->ThrowNew(g_IOEC, "Write has been interrupted");
    }
}

static void JNICALL m_writeBuf(JNIEnv *const env, const jobject jthis,
                               const jbyteArray buf, const jint off, const jint len) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (fdPtm < 0) return;
    jbyte *const b = env->GetByteArrayElements(buf, nullptr);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    const ssize_t r = write(fdPtm, b + off, len);
    env->ReleaseByteArrayElements(buf, b, JNI_ABORT);
    if (r < 0) {
        env->ThrowNew(g_IOEC, strError("Cannot write to PTY"));
    } else if (r < len) {
        env->ThrowNew(g_IOEC, "Write has been interrupted");
    }
}

static jboolean JNICALL m_pollForEof(JNIEnv *const env, const jobject jthis,
                                     const jint fd, const jint millis) {
    if (env->ExceptionCheck() == JNI_TRUE) return JNI_TRUE;
    if (fd < 0) return JNI_TRUE;
    struct pollfd pfds[] = {
            {fd, 0, 0}
    };
    const int r = poll(pfds, 1, millis);
#ifdef DEBUG
    __android_log_print(ANDROID_LOG_DEBUG, CLASS_NAME,
                        "pollForEof(): 0x%04hX", pfds[0].revents);
#endif
    if (r < 0) {
        switch (errno) {
            case EINTR:
                env->ThrowNew(g_IntIOEC, strError("pollForEof"));
                break;
            case ENOMEM:
                env->ThrowNew(g_OOMEC, strError("pollForEof"));
                break;
            default:
                env->ThrowNew(g_IOEC, strError("pollForEof"));
        }
        return JNI_TRUE;
    }
    return r == 0 ? JNI_FALSE : JNI_TRUE;
}

static jboolean JNICALL m_pollForRead(JNIEnv *const env, const jobject jthis,
                                      const jint fd, const jint intFd) {
    struct pollfd pfds[] = {
            {fd,    POLLIN, 0},
            {intFd, POLLIN, 0}
    };
    if (poll(pfds, 2, -1) < 0) {
        switch (errno) {
            case EINTR:
                env->ThrowNew(g_IntIOEC, strError("pollForRead"));
                break;
            case ENOMEM:
                env->ThrowNew(g_OOMEC, strError("pollForRead"));
                break;
            default:
                env->ThrowNew(g_IOEC, strError("pollForRead"));
        }
        return JNI_TRUE;
    }
    return (jboolean) ((pfds[0].revents & POLLIN) ? JNI_FALSE : JNI_TRUE);
}

static jboolean JNICALL m_isatty(JNIEnv *const env, const jobject jthis, const jint fd) {
    return (jboolean) (isatty(fd) ? JNI_TRUE : JNI_FALSE);
}

static void JNICALL m_getSize(JNIEnv *const env, const jobject jthis,
                              const jint fd, const jintArray result) {
    if (result == nullptr) {
        env->ThrowNew(g_illegalArgumentEC, "Result array must not be null");
        return;
    }
    const jsize l = env->GetArrayLength(result);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (l < 4) {
        env->ThrowNew(g_illegalArgumentEC, "Result array must be at least 4 elements long");
        return;
    }
    if (fd < 0) {
        env->ThrowNew(g_IOEC, "fd < 0");
        return;
    }
    struct winsize winp = {0, 0, 0, 0};
    if (ioctl(fd, TIOCGWINSZ, &winp) != 0) {
        env->ThrowNew(g_IOEC, strError("Cannot get PTY size"));
        return;
    }
    jint *const res = (jint *const) env->GetPrimitiveArrayCritical(result, nullptr);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    res[0] = winp.ws_col;
    res[1] = winp.ws_row;
    res[2] = winp.ws_xpixel;
    res[3] = winp.ws_ypixel;
    env->ReleasePrimitiveArrayCritical(result, res, 0);
}

static jlong JNICALL m_getArgMax(JNIEnv *const env, const jobject jthis) {
    return sysconf(_SC_ARG_MAX);
}

static jboolean JNICALL m_isSymlink(JNIEnv *const env, const jobject jthis, const jstring path) {
    const char *const _path = env->GetStringUTFChars(path, nullptr);
    if (env->ExceptionCheck() == JNI_TRUE) return JNI_FALSE;
    struct stat st;
    const jboolean r = (jboolean) ((lstat(_path, &st) == 0 &&
                                    (st.st_mode & S_IFMT) == S_IFLNK) ? JNI_TRUE
                                                                      : JNI_FALSE);
    env->ReleaseStringUTFChars(path, _path);
    return r;
}

static int _pathByFd(char *const realPath, const size_t realPathSize, const char *const procPath) {
    struct stat st1;
    if (stat(procPath, &st1) != 0) return -1;
    const ssize_t r = readlink(procPath, realPath, realPathSize - 1);
    if (r < 0) return -1;
    realPath[r] = '\0';
    struct stat st2;
    if (stat(realPath, &st2) != 0 || st1.st_dev != st2.st_dev || st1.st_ino != st2.st_ino)
        return -1;
    return 0;
}

#define REALPATH_SIZE (PATH_MAX + 1)

static jstring JNICALL m_pathByFd(JNIEnv *const env, const jobject jthis, const jint fd) {
    if (fd == -1) return nullptr;
    char procPath[64];
    sprintf(procPath, "/proc/self/fd/%u", fd);
    {
        char realPath[REALPATH_SIZE];
        if (_pathByFd(realPath, REALPATH_SIZE, procPath) == 0)
            return env->NewStringUTF(realPath);
    }
    {
        // Old (API < 21 of some vendors) release mode?
        // Huh... A little (ugly) step outside the umbrella...
//        __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
//                "pathByFd failed [%s] : plan B", procPath);
        char *const realPath =
                (char *) mmap(nullptr, REALPATH_SIZE,
                              PROT_READ | PROT_WRITE, MAP_ANON | MAP_SHARED,
                              -1, 0);
        if (realPath == MAP_FAILED) {
            __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
                                "[errno: %d] %s [%s]", errno,
                                strError("pathByFd mmap() failed"), procPath);
            return nullptr;
        }
        realPath[0] = '\0';
        const pid_t pid = fork();
        if (pid == -1) {
            __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
                                "[errno: %d] %s [%s]", errno,
                                strError("pathByFd fork() failed"), procPath);
            munmap(realPath, REALPATH_SIZE);
            return nullptr;
        }
        if (pid != 0) {
            int st;
            while (true) {
                const int r = waitpid(pid, &st, 0);
                if (r == -1) {
                    if (errno == EINTR) continue;
                    __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
                                        "[errno: %d] %s [%s]", errno,
                                        strError("pathByFd waitpid() failed"), procPath);
                    munmap(realPath, REALPATH_SIZE);
                    return nullptr;
                }
                break;
            }
            if (!WIFEXITED(st) || (WEXITSTATUS(st) != 0) || (realPath[0] == '\0')) {
                __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
                                    "pathByFd child failed [%s]", procPath);
                munmap(realPath, REALPATH_SIZE);
                return nullptr;
            }
            const jstring ret = env->NewStringUTF(realPath);
            munmap(realPath, REALPATH_SIZE);
            return ret;
        } else {
            if (_pathByFd(realPath, REALPATH_SIZE, procPath) == 0) _exit(0);
            __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
                                "forked pathByFd failed [%s]", procPath);
            _exit(127);
        }
    }
}

static void JNICALL m_shutdown(JNIEnv *const env, const jobject jthis,
                               const jint fd, const jint how) {
    if (shutdown(fd, how) != 0)
        env->ThrowNew(g_IOEC, strError("shutdown() failed"));
}

static jobject JNICALL m_asByteBuffer(JNIEnv *const env, const jobject jthis,
                                      const jlong address, const jlong byteCount) {
    return env->NewDirectByteBuffer((void *) address, byteCount);
}

static jlong JNICALL m_getByteBufferAddress(JNIEnv *const env, const jobject jthis,
                                            const jobject buffer) {
    return (jlong) env->GetDirectBufferAddress(buffer);
}

static jlong JNICALL m_mmap(JNIEnv *const env, const jobject jthis,
                            const jlong address, const jlong byteCount,
                            const jint prot, const jint flags,
                            const jint fd, const jlong offset) {
    void *const r = mmap((void *) address, byteCount, prot, flags, fd, offset);
    if (r == MAP_FAILED)
        env->ThrowNew(g_IOEC, strError("mmap() failed"));
    return (jlong) r;
}

static void JNICALL m_munmap(JNIEnv *const env, const jobject jthis,
                             const jlong address, const jlong byteCount) {
    const int r = munmap((void *) address, byteCount);
    if (r != 0)
        env->ThrowNew(g_IOEC, strError("munmap() failed"));
}

static const JNINativeMethod methodTable[] = {
        {"execve",                 "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)L" CLASS_NAME ";",
                                                                (void *) m_execve},
        {"destroy",                "()V",                       (void *) m_destroy},
        {"waitForExit",            "(I)I",                      (void *) m_waitForExit},
        {"sendSignalToForeground", "(I)V",                      (void *) m_sendSignalToForeground},
        {"resize",                 "(IIII)V",                   (void *) m_resize},
        {"readByte",               "()I",                       (void *) m_readByte},
        {"readBuf",                "([BII)I",                   (void *) m_readBuf},
        {"writeByte",              "(I)V",                      (void *) m_writeByte},
        {"writeBuf",               "([BII)V",                   (void *) m_writeBuf},
        {"pollForEof",             "(II)Z",                     (void *) m_pollForEof},
        {"pollForRead",            "(II)Z",                     (void *) m_pollForRead},
        {"shutdown",               "(II)V",                     (void *) m_shutdown},
        {"isatty",                 "(I)Z",                      (void *) m_isatty},
        {"getSize",                "(I[I)V",                    (void *) m_getSize},
        {"getArgMax",              "()J",                       (void *) m_getArgMax},
        {"isSymlink",              "(Ljava/lang/String;)Z",     (void *) m_isSymlink},
        {"pathByFd",               "(I)Ljava/lang/String;",     (void *) m_pathByFd},
        {"asByteBuffer",           "(JJ)Ljava/nio/ByteBuffer;", (void *) m_asByteBuffer},
        {"getAddress",             "(Ljava/nio/ByteBuffer;)J",  (void *) m_getByteBufferAddress},
        {"mmapCompat",             "(JJIIIJ)J",                 (void *) m_mmap},
        {"munmapCompat",           "(JJ)V",                     (void *) m_munmap}
};

extern "C"
JNIEXPORT jint JNI_OnLoad(JavaVM *const vm, void *const reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), REQ_JNI_VERSION) != JNI_OK) {
        return -1;
    }

    g_OOMEC = (jclass) env->NewGlobalRef(env->FindClass("java/lang/OutOfMemoryError"));
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_runtimeEC = (jclass) env->NewGlobalRef(env->FindClass("java/lang/RuntimeException"));
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_IOEC = (jclass) env->NewGlobalRef(env->FindClass("java/io/IOException"));
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_IntIOEC = (jclass) env->NewGlobalRef(env->FindClass("java/io/InterruptedIOException"));
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_illegalArgumentEC = (jclass) env->NewGlobalRef(
            env->FindClass("java/lang/IllegalArgumentException"));
    if (env->ExceptionCheck() == JNI_TRUE) return -1;

    g_class = (jclass) env->NewGlobalRef(env->FindClass(CLASS_NAME));
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_ctrId = env->GetMethodID(g_class, "<init>", "(II)V");
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_fdPtmId = env->GetFieldID(g_class, "fdPtm", "I");
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_pidId = env->GetFieldID(g_class, "pid", "I");
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    g_exitStatusId = env->GetFieldID(g_class, "exitStatus", "I");
    if (env->ExceptionCheck() == JNI_TRUE) return -1;

    env->RegisterNatives(g_class, methodTable, SIZEOFTBL(methodTable));

    return REQ_JNI_VERSION;
}

extern "C"
JNIEXPORT void JNI_OnUnload(JavaVM *const vm, void *const reserved) {
    JNIEnv *env;
    vm->GetEnv(reinterpret_cast<void **>(&env), REQ_JNI_VERSION);

    env->DeleteGlobalRef(g_class);

    env->DeleteGlobalRef(g_illegalArgumentEC);
    env->DeleteGlobalRef(g_IntIOEC);
    env->DeleteGlobalRef(g_IOEC);
    env->DeleteGlobalRef(g_runtimeEC);
    env->DeleteGlobalRef(g_OOMEC);
}

#pragma clang diagnostic pop
