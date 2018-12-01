//
// Created by alex on 12/25/18.
//

#if __ANDROID_API__ < 23

#include <fcntl.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <unistd.h>
#include <string.h>

#include "pty_compat.h"

extern "C"
pid_t forkpty(int *const master, char *const slave_name, const struct termios *const termp,
              const struct winsize *const winp) {
    const int ptm = getpt();
    if (ptm < 0) return -1;
    fcntl(ptm, F_SETFD, FD_CLOEXEC);
    char devname[PATH_MAX];
    if (grantpt(ptm) || unlockpt(ptm) || ptsname_r(ptm, devname, sizeof(devname)) != 0) {
        close(ptm);
        return -1;
    }
    const int pts = open(devname, O_RDWR);
    if (pts < 0) {
        close(ptm);
        return -1;
    }
    if (termp) tcsetattr(pts, TCSANOW, termp);
    if (winp) ioctl(pts, TIOCSWINSZ, winp);
    const pid_t pid = fork();
    if (pid < 0) {
        close(pts);
        close(ptm);
        return -1;
    }
    if (pid == 0) {
        close(ptm);
        setsid();
        if (ioctl(pts, TIOCSCTTY, (char *) NULL) == -1) _exit(127);
        dup2(pts, STDIN_FILENO);
        dup2(pts, STDOUT_FILENO);
        dup2(pts, STDERR_FILENO);
        if (pts > 2) close(pts);
    } else {
        close(pts);
        *master = ptm;
        if (slave_name) strcpy(slave_name, devname);
    }
    return pid;
}

#endif
