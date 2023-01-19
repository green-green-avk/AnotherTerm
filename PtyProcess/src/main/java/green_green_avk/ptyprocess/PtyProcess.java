package green_green_avk.ptyprocess;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.nio.ByteBuffer;
import java.util.Map;

@Keep
public final class PtyProcess extends Process {

    // It seems all Android architectures have the same values...
    // Valid for: aarch64, armv7a-eabi, x86_64, i686.
    // Actual before API 21 only.
    // TODO: A bit tricky, refactor...
    public static final int EPERM = 1;
    public static final int ENOENT = 2;
    public static final int ESRCH = 3;
    public static final int EINTR = 4;
    public static final int EIO = 5;
    public static final int ENXIO = 6;
    public static final int E2BIG = 7;
    public static final int ENOEXEC = 8;
    public static final int EBADF = 9;
    public static final int ECHILD = 10;
    public static final int EAGAIN = 11;
    public static final int ENOMEM = 12;
    public static final int EACCES = 13;
    public static final int EFAULT = 14;
    public static final int ENOTBLK = 15;
    public static final int EBUSY = 16;
    public static final int EEXIST = 17;
    public static final int EXDEV = 18;
    public static final int ENODEV = 19;
    public static final int ENOTDIR = 20;
    public static final int EISDIR = 21;
    public static final int EINVAL = 22;
    public static final int ENFILE = 23;
    public static final int EMFILE = 24;
    public static final int ENOTTY = 25;
    public static final int ETXTBSY = 26;
    public static final int EFBIG = 27;
    public static final int ENOSPC = 28;
    public static final int ESPIPE = 29;
    public static final int EROFS = 30;
    public static final int EMLINK = 31;
    public static final int EPIPE = 32;
    public static final int EDOM = 33;
    public static final int ERANGE = 34;
    public static final int EDEADLK = 35;
    public static final int ENAMETOOLONG = 36;
    public static final int ENOLCK = 37;
    public static final int ENOSYS = 38;
    public static final int ENOTEMPTY = 39;
    public static final int ELOOP = 40;
    public static final int EWOULDBLOCK = 11;
    public static final int ENOMSG = 42;
    public static final int EIDRM = 43;
    public static final int ECHRNG = 44;
    public static final int EL2NSYNC = 45;
    public static final int EL3HLT = 46;
    public static final int EL3RST = 47;
    public static final int ELNRNG = 48;
    public static final int EUNATCH = 49;
    public static final int ENOCSI = 50;
    public static final int EL2HLT = 51;
    public static final int EBADE = 52;
    public static final int EBADR = 53;
    public static final int EXFULL = 54;
    public static final int ENOANO = 55;
    public static final int EBADRQC = 56;
    public static final int EBADSLT = 57;
    public static final int EDEADLOCK = 35;
    public static final int EBFONT = 59;
    public static final int ENOSTR = 60;
    public static final int ENODATA = 61;
    public static final int ETIME = 62;
    public static final int ENOSR = 63;
    public static final int ENONET = 64;
    public static final int ENOPKG = 65;
    public static final int EREMOTE = 66;
    public static final int ENOLINK = 67;
    public static final int EADV = 68;
    public static final int ESRMNT = 69;
    public static final int ECOMM = 70;
    public static final int EPROTO = 71;
    public static final int EMULTIHOP = 72;
    public static final int EDOTDOT = 73;
    public static final int EBADMSG = 74;
    public static final int EOVERFLOW = 75;
    public static final int ENOTUNIQ = 76;
    public static final int EBADFD = 77;
    public static final int EREMCHG = 78;
    public static final int ELIBACC = 79;
    public static final int ELIBBAD = 80;
    public static final int ELIBSCN = 81;
    public static final int ELIBMAX = 82;
    public static final int ELIBEXEC = 83;
    public static final int EILSEQ = 84;
    public static final int ERESTART = 85;
    public static final int ESTRPIPE = 86;
    public static final int EUSERS = 87;
    public static final int ENOTSOCK = 88;
    public static final int EDESTADDRREQ = 89;
    public static final int EMSGSIZE = 90;
    public static final int EPROTOTYPE = 91;
    public static final int ENOPROTOOPT = 92;
    public static final int EPROTONOSUPPORT = 93;
    public static final int ESOCKTNOSUPPORT = 94;
    public static final int EOPNOTSUPP = 95;
    public static final int EPFNOSUPPORT = 96;
    public static final int EAFNOSUPPORT = 97;
    public static final int EADDRINUSE = 98;
    public static final int EADDRNOTAVAIL = 99;
    public static final int ENETDOWN = 100;
    public static final int ENETUNREACH = 101;
    public static final int ENETRESET = 102;
    public static final int ECONNABORTED = 103;
    public static final int ECONNRESET = 104;
    public static final int ENOBUFS = 105;
    public static final int EISCONN = 106;
    public static final int ENOTCONN = 107;
    public static final int ESHUTDOWN = 108;
    public static final int ETOOMANYREFS = 109;
    public static final int ETIMEDOUT = 110;
    public static final int ECONNREFUSED = 111;
    public static final int EHOSTDOWN = 112;
    public static final int EHOSTUNREACH = 113;
    public static final int EALREADY = 114;
    public static final int EINPROGRESS = 115;
    public static final int ESTALE = 116;
    public static final int EUCLEAN = 117;
    public static final int ENOTNAM = 118;
    public static final int ENAVAIL = 119;
    public static final int EISNAM = 120;
    public static final int EREMOTEIO = 121;
    public static final int EDQUOT = 122;
    public static final int ENOMEDIUM = 123;
    public static final int EMEDIUMTYPE = 124;
    public static final int ECANCELED = 125;
    public static final int ENOKEY = 126;
    public static final int EKEYEXPIRED = 127;
    public static final int EKEYREVOKED = 128;
    public static final int EKEYREJECTED = 129;
    public static final int EOWNERDEAD = 130;
    public static final int ENOTRECOVERABLE = 131;
    public static final int ERFKILL = 132;
    public static final int EHWPOISON = 133;
    public static final int ENOTSUP = 95;

    public static final int WNOHANG = 0x00000001;

    public static final int O_RDONLY = 00000000;
    public static final int O_WRONLY = 00000001;
    public static final int O_RDWR = 00000002;
    public static final int O_CREAT = 00000100;
    public static final int O_PATH = 010000000;

    public static final int SIGHUP = 1;
    public static final int SIGINT = 2;
    public static final int SIGQUIT = 3;
    public static final int SIGABRT = 6;
    public static final int SIGKILL = 9;
    public static final int SIGALRM = 14;
    public static final int SIGTERM = 15;

    public static final int IS_ERROR = -1;
    public static final int IS_RUNNING = -2;
    public static final int IS_EINTR = -3;

    public static final int SHUT_RD = 0;
    public static final int SHUT_WR = 1;
    public static final int SHUT_RDWR = 2;

    public static final int PROT_NONE = 0;
    public static final int PROT_READ = 1;
    public static final int PROT_WRITE = 2;
    public static final int PROT_EXEC = 4;

    public static final int MAP_TYPE = 0x0f;
    public static final int MAP_SHARED = 0x01;
    public static final int MAP_PRIVATE = 0x02;
    public static final int MAP_SHARED_VALIDATE = 0x03;
    public static final int MAP_ANONYMOUS = 0x20;
    public static final int MAP_ANON = MAP_ANONYMOUS;
    public static final int MAP_POPULATE = 0x008000;
    public static final int MAP_NONBLOCK = 0x010000;
    public static final int MAP_SYNC = 0x080000;

    static {
        System.loadLibrary("ptyprocess");
    }

    @Keep
    private volatile int fdPtm;
    @Keep
    private final int pid;
    @Keep
    private volatile int exitStatus = IS_RUNNING;

    @Keep
    private PtyProcess(final int fdPtm, final int pid) {
        this.fdPtm = fdPtm;
        this.pid = pid;
    }

    public int getPtm() {
        return fdPtm;
    }

    public int getPid() {
        return pid;
    }

    @NonNull
    @Keep
    public static native PtyProcess execve(@NonNull final String filename, final String[] args,
                                           final String[] env);

    @NonNull
    public static PtyProcess execve(@NonNull final String filename, final String[] args,
                                    final Map<String, String> env) {
        if (env == null)
            return execve(filename, args, (String[]) null);
        final String[] _env = new String[env.size()];
        int i = 0;
        for (final Map.Entry<String, String> elt : env.entrySet()) {
            _env[i++] = elt.getKey() + "=" + elt.getValue();
        }
        return execve(filename, args, _env);
    }

    @NonNull
    public static PtyProcess execv(@NonNull final String filename, final String[] args) {
        return execve(filename, args, (String[]) null);
    }

    @NonNull
    public static PtyProcess execl(@NonNull final String filename,
                                   final Map<String, String> env, final String... args) {
        return execve(filename, args, env);
    }

    @NonNull
    public static PtyProcess execl(@NonNull final String filename, final String... args) {
        return execv(filename, args);
    }

    private static final String shPath = "/system/bin/sh";
    private static final String shLoginArg =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN ? null : "-l";

    @NonNull
    public static PtyProcess system(@Nullable final String command,
                                    @Nullable final Map<String, String> env) {
        if (command == null || command.isEmpty())
            return execl(shPath, env, "-sh", shLoginArg);
        return execl(shPath, env, "-sh", shLoginArg, "-c", command);
    }

    @NonNull
    public static PtyProcess system(@Nullable final String command) {
        return system(command, null);
    }

    @Override
    public OutputStream getOutputStream() {
        return input;
    }

    @Override
    public InputStream getInputStream() {
        return output;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public int waitFor() throws InterruptedException {
        final int r = waitForExit(0);
        if (r == IS_EINTR)
            throw new InterruptedException();
        return r;
    }

    @Override
    public int exitValue() {
        final int r = waitForExit(WNOHANG);
        if (r == IS_RUNNING)
            throw new IllegalThreadStateException("The process is still running");
        return r;
    }

    @Override
    @Keep
    public native void destroy();

    @IntDef(flag = true, value = WNOHANG)
    @Retention(SOURCE)
    public @interface WaitForExitOptions {
    }

    @Keep
    public native int waitForExit(@WaitForExitOptions int options);

    @Keep
    public boolean waitForPtyEof(@IntRange(from = -1) final int millis) throws IOException {
        return pollForEof(getPtm(), millis);
    }

    @Keep
    public native void sendSignalToForeground(int signal);

    @Keep
    public native void resize(int width, int height, int widthPx, int heightPx) throws IOException;

    // TODO: Or ParcelFileDescriptor / File Streams?

    @Keep
    private native int readByte() throws IOException;

    @Keep
    private native int readBuf(byte[] buf, int off, int len) throws IOException;

    @Keep
    private native void writeByte(int b) throws IOException;

    @Keep
    private native void writeBuf(byte[] buf, int off, int len) throws IOException;

    private final InputStream output = new InputStream() {
        @Override
        public int read() throws IOException {
            return readByte();
        }

        @Override
        public int read(@NonNull final byte[] b, final int off, final int len)
                throws IOException {
            if (b == null)
                throw new NullPointerException();
            if (off < 0 || len < 0 || off + len > b.length)
                throw new IndexOutOfBoundsException();
            return readBuf(b, off, len);
        }
    };

    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) throws IOException {
            writeByte(b);
        }

        @Override
        public void write(@NonNull final byte[] b, final int off, final int len)
                throws IOException {
            if (b == null)
                throw new NullPointerException();
            if (off < 0 || len < 0 || off + len > b.length)
                throw new IndexOutOfBoundsException();
            writeBuf(b, off, len);
        }
    };

    // Asynchronous close during read (FileChannel class) seems having some issues
    // before Lollipop...
    // So, let eat bees.
    public static final class InterruptableFileInputStream
            extends ParcelFileDescriptor.AutoCloseInputStream {
        private final Object readLock = new Object();
        private final Object closeLock = new Object();
        private volatile boolean closed = false;
        private final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        @NonNull
        public final ParcelFileDescriptor pfd;

        public InterruptableFileInputStream(@NonNull final ParcelFileDescriptor pfd)
                throws IOException {
            super(pfd);
            this.pfd = pfd;
        }

        private void interrupt() throws IOException {
            pipe[1].close();
        }

        private void interruptQuiet() {
            try {
                interrupt();
            } catch (final IOException ignored) {
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (closeLock) {
                if (closed)
                    return;
                interruptQuiet();
                synchronized (readLock) {
                    closed = true;
                    super.close();
                }
            }
        }

        private boolean check() throws IOException {
            if (closed)
                return true;
            try {
                return pollForRead(pfd.getFd(), pipe[0].getFd());
            } catch (final IllegalStateException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        @Override
        public int read() throws IOException {
            synchronized (readLock) {
                if (check())
                    return -1;
                return super.read();
            }
        }

        @Override
        public int read(final byte[] b) throws IOException {
            synchronized (readLock) {
                if (check())
                    return -1;
                return super.read(b);
            }
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            synchronized (readLock) {
                if (check())
                    return -1;
                return super.read(b, off, len);
            }
        }
    }

    public static final class PfdFileOutputStream
            extends ParcelFileDescriptor.AutoCloseOutputStream {
        @NonNull
        public final ParcelFileDescriptor pfd;

        public PfdFileOutputStream(@NonNull final ParcelFileDescriptor pfd) {
            super(pfd);
            this.pfd = pfd;
        }
    }

    public static boolean isatty(@NonNull final ParcelFileDescriptor pfd) {
        return isatty(pfd.getFd());
    }

    public static void getSize(final OutputStream s, @NonNull final int[] result)
            throws IOException {
        if (s instanceof PfdFileOutputStream) {
            getSize(((PfdFileOutputStream) s).pfd.getFd(), result);
            return;
        }
        throw new IllegalArgumentException("Unsupported stream type");
    }

    @NonNull
    public static String getPathByFd(final int fd) throws IOException {
        // See https://github.com/green-green-avk/AnotherTerm/issues/7
        final String pp = pathByFd(fd);
        if (pp == null)
            throw new IOException("Failed getting path by fd " + fd);
        return pp;
    }

    // Actual before API 21 only
    @Keep
    public static native boolean pollForEof(int fd, @IntRange(from = -1) int millis)
            throws IOException;

    public static boolean pollForEof(final int fd) throws IOException {
        return pollForEof(fd, -1);
    }

    @Keep
    public static native boolean pollForRead(int fd, int intFd) throws IOException;

    @Keep
    public static native boolean isatty(int fd);

    @Keep
    public static native void getSize(int fd, @NonNull int[] result) throws IOException;

    @Keep
    public static native long getArgMax();

    @Keep
    public static native boolean isSymlink(@NonNull String path);

    @Keep
    @Nullable
    private static native String pathByFd(int fd);

    private static int getFdIntHelper(@NonNull final FileDescriptor fd,
                                      @NonNull final String msg) throws IOException {
        try {
            return (int) FileDescriptor.class.getMethod("getInt$").invoke(fd);
        } catch (final Exception e) {
            throw new IOException(msg);
        }
    }

    /*
     * It seems, android.system.Os class is trying to be linked by Dalvik even when inside
     * appropriate if statement and raises java.lang.VerifyError on the constructor call...
     * API 19 is affected at least.
     * Moving to separate class to work it around.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static final class Utils21 {
        private Utils21() {
        }

        private static void close(@NonNull final FileDescriptor fd) throws IOException {
            try {
                Os.close(fd);
            } catch (final ErrnoException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        private static void shutdown(@NonNull final FileDescriptor fd, final int how)
                throws IOException {
            try {
                Os.shutdown(fd, how);
            } catch (final ErrnoException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        private static long mmap(final long address, final long byteCount,
                                 final int prot, final int flags,
                                 @Nullable final FileDescriptor fd, final long offset)
                throws IOException {
            try {
                return Os.mmap(address, byteCount, prot, flags, fd, offset);
            } catch (final ErrnoException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        private static void munmap(final long address, final long byteCount) throws IOException {
            try {
                Os.munmap(address, byteCount);
            } catch (final ErrnoException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    private static final String sCloseWaError = "Cannot close socket: workaround failed";

    public static void close(@Nullable final FileDescriptor fd) throws IOException {
        if (fd == null || !fd.valid())
            throw new IOException("Bad FD");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Utils21.close(fd);
        } else {
            ParcelFileDescriptor.adoptFd(getFdIntHelper(fd, sCloseWaError)).close();
        }
    }

    private static final String sShutdownWaError = "Cannot shutdown socket: workaround failed";

    public static void shutdown(@Nullable final FileDescriptor fd, final int how)
            throws IOException {
        if (fd == null || !fd.valid())
            throw new IOException("Bad FD");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Utils21.shutdown(fd, how);
        } else {
            shutdown(getFdIntHelper(fd, sShutdownWaError), how);
        }
    }

    @Keep
    private static native void shutdown(int fd, int how) throws IOException;

    @Keep
    public static native ByteBuffer asByteBuffer(long address, long byteCount);

    @Keep
    public static native long getAddress(@NonNull ByteBuffer buffer);

    private static final String sMmapWaError = "Cannot mmap: workaround failed";

    public static long mmap(final long address, final long byteCount,
                            final int prot, final int flags,
                            @Nullable final FileDescriptor fd, final long offset)
            throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Utils21.mmap(address, byteCount, prot, flags, fd, offset);
        } else {
            return mmapCompat(address, byteCount, prot, flags,
                    fd != null ? getFdIntHelper(fd, sMmapWaError) : -1, offset);
        }
    }

    @Keep
    private static native long mmapCompat(long address, long byteCount, int prot, int flags,
                                          int fd, long offset) throws IOException;

    public static void munmap(final long address, final long byteCount) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Utils21.munmap(address, byteCount);
        } else {
            munmapCompat(address, byteCount);
        }
    }

    @Keep
    private static native void munmapCompat(long address, long byteCount) throws IOException;
}
