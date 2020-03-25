//
// Created by alex on 12/23/18.
//

#include <jni.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <signal.h>
#include "pty_compat.h"
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <poll.h>

#include <android/log.h>

#include "common.h"

#define REQ_JNI_VERSION JNI_VERSION_1_6

#define CLASS_NAME PKG_NAME "/PtyProcess"

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
        env->ThrowNew(g_runtimeEC, strError("Cannot fork into pty"));
        return nullptr;
    }
    if (pid > 0) {
        env->ReleaseStringUTFChars(cmd_filename, filename);
        releaseStrings(args);
        releaseStrings(envp);
        return env->NewObject(g_class, g_ctrId, fdPtm, pid);
    }
    { // TODO: /proc/<pid>/fd seems better
        const int fdlimit = (int) (sysconf(_SC_OPEN_MAX));
        int fd = 3;
        while (fd < fdlimit) close(fd++);

        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, nullptr);
    }
    if (envp == nullptr) execv(filename, args);
    else execve(filename, args, envp);
    __android_log_print(ANDROID_LOG_ERROR, CLASS_NAME,
            "[errno: %d] %s [%s]", errno, strError("Exec failed"), filename);
    _exit(127);
}

static void JNICALL m_destroy(JNIEnv *const env, const jobject jthis) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (fdPtm < 0) return;
    const jint pid = env->GetIntField(jthis, g_pidId);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    const int pgid = tcgetpgrp(fdPtm);
    if (pgid > 0) killpg(pgid, SIGHUP);
    kill(pid, SIGTERM);
    env->SetIntField(jthis, g_fdPtmId, -1);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    close(fdPtm);
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
        env->ThrowNew(g_IOEC, strError("Cannot set pty size"));
}

static jint JNICALL m_readByte(JNIEnv *const env, const jobject jthis) {
    const jint fdPtm = env->GetIntField(jthis, g_fdPtmId);
    if (env->ExceptionCheck() == JNI_TRUE) return -1;
    if (fdPtm < 0) return -1;
    unsigned char b;
    const ssize_t r = read(fdPtm, &b, 1);
    if (r < 0) {
        env->ThrowNew(g_IOEC, strError("Cannot read from pty"));
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
        env->ThrowNew(g_IOEC, strError("Cannot read from pty"));
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
        env->ThrowNew(g_IOEC, strError("Cannot write to pty"));
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
        env->ThrowNew(g_IOEC, strError("Cannot write to pty"));
    } else if (r < len) {
        env->ThrowNew(g_IOEC, "Write has been interrupted");
    }
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
        env->ThrowNew(g_IOEC, strError("Cannot get pty size"));
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
    struct stat st;
    const char *const _path = env->GetStringUTFChars(path, nullptr);
    const jboolean r = (jboolean) ((lstat(_path, &st) == 0 &&
                                    (st.st_mode & S_IFMT) == S_IFLNK) ? JNI_TRUE
                                                                      : JNI_FALSE);
    env->ReleaseStringUTFChars(path, _path);
    return r;
}

static const JNINativeMethod methodTable[] = {
        {"execve",                 "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)L" CLASS_NAME ";",
        (void *) m_execve},
        {"destroy",                "()V",                   (void *) m_destroy},
        {"sendSignalToForeground", "(I)V",                  (void *) m_sendSignalToForeground},
        {"resize",                 "(IIII)V",               (void *) m_resize},
        {"readByte",               "()I",                   (void *) m_readByte},
        {"readBuf",                "([BII)I",               (void *) m_readBuf},
        {"writeByte",              "(I)V",                  (void *) m_writeByte},
        {"writeBuf",               "([BII)V",               (void *) m_writeBuf},
        {"pollForRead",            "(II)Z",                 (void *) m_pollForRead},
        {"isatty",                 "(I)Z",                  (void *) m_isatty},
        {"getSize",                "(I[I)V",                (void *) m_getSize},
        {"getArgMax",              "()J",                   (void *) m_getArgMax},
        {"isSymlink",              "(Ljava/lang/String;)Z", (void *) m_isSymlink}
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
