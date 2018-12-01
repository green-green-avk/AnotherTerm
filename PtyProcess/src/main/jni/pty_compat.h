//
// Created by alex on 12/25/18.
//

#pragma once

#include <pty.h>

#if __ANDROID_API__ < 23

extern "C"
int forkpty(int *__master_fd, char *__slave_name, const struct termios *__termios_ptr,
            const struct winsize *__winsize_ptr);

#endif
