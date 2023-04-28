package green_green_avk.anotherterm.backends.local;

import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.utils.LogMessage;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.ptyprocess.PtyProcess;

public final class LocalModule extends BackendModule {

    public static final Meta meta =
            new Meta(LocalModule.class, "local-terminal") {
                @Override
                public int getDisconnectionReasonTypes() {
                    return DisconnectionReason.PROCESS_EXIT;
                }
            };

    private final Object connectionLock = new Object();

    private volatile PtyProcess proc = null;
    private volatile Integer exitStatus = null;
    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) throws IOException {
            final PtyProcess p = proc;
            if (p == null) return;
            p.getOutputStream().write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            final PtyProcess p = proc;
            if (p == null) return;
            p.getOutputStream().write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            final PtyProcess p = proc;
            if (p == null) return;
            p.getOutputStream().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            final PtyProcess p = proc;
            if (p == null) return;
            p.getOutputStream().flush();
        }

        @Override
        public void close() throws IOException {
            final PtyProcess p = proc;
            if (p == null) return;
            p.getOutputStream().close();
        }
    };
    private OutputStream output = null;
    private OnMessageListener onMessageListener = null;
    private volatile ReaderThread readerThread = null;

    private void reportMessage(@NonNull final String m) {
        if (onMessageListener != null)
            onMessageListener.onMessage(m);
    }

    private void reportState(@NonNull final StateMessage m) {
        if (onMessageListener != null)
            onMessageListener.onMessage(m);
    }

    private void reportError(@NonNull final Throwable e) {
        if (onMessageListener != null)
            onMessageListener.onMessage(e);
    }

    private final class ReaderThread extends Thread {
        private final byte[] buf = new byte[8192];
        @NonNull
        private final PtyProcess.InterruptableFileInputStream stream;

        ReaderThread(@NonNull final PtyProcess p) throws IOException {
            super();
            stream = new PtyProcess.InterruptableFileInputStream(
                    ParcelFileDescriptor.fromFd(p.getPtm()));
        }

        public void close() {
            try {
                stream.close();
            } catch (final IOException ignored) {
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final int len = stream.read(buf);
                    if (len < 0)
                        return;
                    output.write(buf, 0, len);
                } catch (final IOException e) {
                    reportError(e);
                    return;
                }
            }
        }
    }

    public static final class SessionData {

        public static final class PermMeta {
            public final long bits;
            @StringRes
            public final int titleRes;

            private PermMeta(final long bits, final int titleRes) {
                this.bits = bits;
                this.titleRes = titleRes;
            }
        }

        public static final long PERM_FAVMGMT = 1;
        public static final long PERM_PLUGINEXEC = 2;
        public static final long PERM_CLIPBOARD_COPY = 4;
        public static final Map<String, PermMeta> permByName = new HashMap<>();

        static {
            permByName.put("favmgmt", new PermMeta(PERM_FAVMGMT,
                    R.string.label_favorites_management));
            permByName.put("pluginexec", new PermMeta(PERM_PLUGINEXEC,
                    R.string.label_plugins_execution));
            permByName.put("clipboard-copy", new PermMeta(PERM_CLIPBOARD_COPY,
                    R.string.label_copy_to_clipboard));
        }

        public final AtomicLong permissions = new AtomicLong(0);

        public BackendUiInteraction ui;

        @NonNull
        public final WakeLockRef wakeLock;

        public SessionData(@NonNull final BackendModule self) {
            this.wakeLock = self.getWakeLock();
        }
    }

    private static final Map<Long, SessionData> sessionDataMap = new ConcurrentHashMap<>();
    private static final SecureRandom rng = new SecureRandom();

    public static final long NO_SESSION = 0;

    private static long generateToken() {
        return rng.nextLong();
    }

    private static long generateSessionToken() {
        final long v = generateToken();
        return (v == NO_SESSION) ? generateToken() : v;
    }

    @Nullable
    public static SessionData getSessionData(final long token) {
        if (token == NO_SESSION) return null;
        final SessionData sd = sessionDataMap.get(token);
        if (sd == null) throw new IllegalArgumentException();
        return sd;
    }

    @Nullable
    public static SessionData getSessionData(@Nullable final String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            return getSessionData(Long.parseLong(token, 16));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Synchronize on it when changing anything inside it if reasonable.
     */
    private final SessionData sessionData = new SessionData(this);
    private final long sessionToken = generateSessionToken();

    {
        sessionDataMap.put(sessionToken, sessionData);
    }

    @Override
    public void setUi(final BackendUiInteraction ui) {
        super.setUi(ui);
        sessionData.ui = ui;
    }

    private String terminalString = "xterm";
    private String execute = "";

    // If the process died but some other process still keeps the pipe open...
    private static final int PIPE_TIMEOUT = 3000; // [ms]

    private static final String ENV_INPUT_PREFIX = "$input.";
    private final Map<String, String> envInput = new HashMap<>();

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        terminalString = pp.getString("terminal_string", terminalString);
        execute = pp.getString("execute", execute);
        long _perms = 0;
        for (final Map.Entry<String, SessionData.PermMeta> m : SessionData.permByName.entrySet())
            if (pp.getBoolean("perm_" + m.getKey(), false))
                _perms |= m.getValue().bits;
        sessionData.permissions.set(_perms);
        envInput.clear();
        for (final Map.Entry<String, ?> p : params.entrySet())
            if (p.getKey() != null && p.getKey().startsWith(ENV_INPUT_PREFIX) &&
                    p.getValue() instanceof String)
                envInput.put(
                        p.getKey().substring(ENV_INPUT_PREFIX.length()).toUpperCase(Locale.ROOT),
                        (String) p.getValue());
    }

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        output = stream;
    }

    @Override
    @NonNull
    public OutputStream getOutputStream() {
        return input;
    }

    @Override
    public void setOnMessageListener(@Nullable final OnMessageListener l) {
        onMessageListener = l;
    }

    @Override
    public boolean isConnected() {
        final PtyProcess p = proc;
        if (p != null) try {
            p.exitValue();
            return false;
        } catch (final IllegalThreadStateException e) {
            return true;
        }
        return false;
    }

    @Override
    public void connect() {
        final Map<String, String> env = new HashMap<>(System.getenv());
        env.put(BuildConfig.SHELL_SESSION_TOKEN_VAR, Long.toHexString(sessionToken).toUpperCase());
        env.put("TERM", terminalString);
        env.put("DATA_DIR", context.getApplicationInfo().dataDir);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            env.put("PROTECTED_DATA_DIR", context.getApplicationInfo().deviceProtectedDataDir);
        }
        final File extDataDir = context.getExternalFilesDir(null);
        if (extDataDir != null) {
            env.put("EXTERNAL_DATA_DIR", extDataDir.getAbsolutePath());
            env.put("SHARED_DATA_DIR", extDataDir.getAbsolutePath());
        }
        env.put("PUBLIC_DATA_DIR", Environment.getExternalStorageDirectory().getAbsolutePath());
        env.put("LIB_DIR", context.getApplicationInfo().nativeLibraryDir);
        env.put("APP_APK", context.getApplicationInfo().sourceDir);
        env.put("APP_ID", BuildConfig.APPLICATION_ID);
        env.put("APP_VERSION", BuildConfig.VERSION_NAME);
        env.put("APP_TARGET_SDK", Integer.toString(BuildConfig.TARGET_SDK_VERSION));
        env.put("MY_DEVICE_ABIS", TextUtils.join(" ", Misc.getAbis()));
        env.put("MY_ANDROID_SDK", Integer.toString(Build.VERSION.SDK_INT));
        // Input URIs
        for (final Map.Entry<String, String> ei : envInput.entrySet()) {
            env.put("INPUT_" + ei.getKey(), ei.getValue());
        }
        // ==========
        synchronized (connectionLock) {
            final PtyProcess p = PtyProcess.system(execute, env);
            try {
                readerThread = new ReaderThread(p);
            } catch (final IOException e) {
                throw new BackendException(e);
            }
            proc = p;
            exitStatus = null;
            readerThread.setDaemon(true);
            readerThread.start();
            final Thread keeper = new Thread(() -> {
                int status;
                while (true) {
                    try {
                        status = p.waitFor();
                    } catch (final InterruptedException e) {
                        continue;
                    }
                    break;
                }
                exitStatus = status;
                reportMessage(context.getString(
                        R.string.msg_process_exited_with_status_d,
                        status));
                boolean hasEof;
                try {
                    hasEof = p.waitForPtyEof(PIPE_TIMEOUT);
                } catch (final IOException e) {
                    hasEof = true;
                    if (BuildConfig.DEBUG)
                        reportMessage(e.toString());
                }
                if (!hasEof) {
                    reportMessage(context.getString(R.string.msg_pty_timeout_reached));
                }
                disconnect(hasEof);
                reportState(new DisconnectStateMessage(context.getString(
                        R.string.msg_pty_connection_closed)));
            });
            keeper.setDaemon(true);
            keeper.start();
        }
        if (isAcquireWakeLockOnConnect())
            acquireWakeLock();
    }

    @Override
    public void disconnect() {
        disconnect(false);
    }

    public void disconnect(final boolean soft) {
        try {
            synchronized (connectionLock) {
                final Process p = proc;
                if (p == null)
                    return;
                proc = null;
                p.destroy();
                if (!soft)
                    readerThread.close();
                try {
                    readerThread.join();
                } catch (final InterruptedException ignored) {
                }
                if (soft)
                    readerThread.close();
                readerThread = null;
            }
        } finally {
            if (isReleaseWakeLockOnDisconnect())
                releaseWakeLock();
        }
    }

    @Override
    public void stop() {
        sessionDataMap.remove(sessionToken);
        super.stop();
    }

    @Override
    protected void finalize() throws Throwable {
        sessionDataMap.remove(sessionToken);
        disconnect();
        super.finalize();
    }

    @Override
    public void resize(final int col, final int row, final int wp, final int hp) {
        final PtyProcess p = proc;
        if (p == null) return;
        try {
            p.resize(col, row, wp, hp);
        } catch (final IOException ignored) {
        }
    }

    @Override
    @Nullable
    public List<LogMessage> getLog() {
        return null;
    }

    @Override
    @NonNull
    public String getConnDesc() {
        return "Local Terminal";
    }

    @Override
    @Nullable
    public DisconnectionReason getDisconnectionReason() {
        final Integer es = exitStatus;
        return es != null ? new ProcessExitDisconnectionReason(es) : null;
    }

    /**
     * If the terminal is in a mode when it does not intercept some control bytes,
     * the functions below can be used to send appropriate signals to the foreground process group.
     * <a href="https://www.win.tue.nl/~aeb/linux/lk/lk-10.html">The Linux kernel - 10. Processes</a>
     */
    @ExportedUIMethod(titleRes = R.string.action_send_signal,
            longTitleRes = R.string.action_send_signal_to_fg_pg, order = 1)
    public void sendSignal(@ExportedUIMethodIntEnum(values = {
            PtyProcess.SIGHUP, PtyProcess.SIGINT, PtyProcess.SIGQUIT, PtyProcess.SIGABRT,
            PtyProcess.SIGKILL, PtyProcess.SIGALRM, PtyProcess.SIGTERM
    }, titleRes = {
            R.string.label_sighup, R.string.label_sigint, R.string.label_sigquit,
            R.string.label_sigabrt,
            R.string.label_sigkill, R.string.label_sigalrm, R.string.label_sigterm,
    }) final int signal) {
        final PtyProcess p = proc;
        if (p == null) return;
        p.sendSignalToForeground(signal);
    }

    /**
     * Tools (only `termsh' now) for interaction with the Android environment are supposed to be
     * controlled on what they can do.
     * <p>
     * TODO: Reactive?
     */
    @ExportedUIMethod(titleRes = R.string.action_session_permissions, order = 2)
    @ExportedUIMethodFlags(values = {
            SessionData.PERM_FAVMGMT, SessionData.PERM_PLUGINEXEC,
            SessionData.PERM_CLIPBOARD_COPY
    }, titleRes = {
            R.string.label_favorites_management, R.string.label_plugins_execution,
            R.string.label_copy_to_clipboard
    })
    public long changePermissions(final long permissions, final long mask) {
        return Misc.getAndUpdate(sessionData.permissions,
                v -> (permissions & mask) | (v & ~mask));
    }
}
