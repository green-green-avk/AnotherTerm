package green_green_avk.anotherterm.backends.local;

import android.os.Build;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.ptyprocess.PtyProcess;

public final class LocalModule extends BackendModule {

    @Keep
    public static final Meta meta = new Meta(LocalModule.class, "local-terminal");

    private final Object connectionLock = new Object();

    private PtyProcess proc = null;
    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) throws IOException {
            if (proc == null) return;
            proc.getOutputStream().write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            if (proc == null) return;
            proc.getOutputStream().write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (proc == null) return;
            proc.getOutputStream().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (proc == null) return;
            proc.getOutputStream().flush();
        }

        @Override
        public void close() throws IOException {
            if (proc == null) return;
            proc.getOutputStream().close();
        }
    };
    private OutputStream output = null;
    private OnMessageListener onMessageListener = null;
    private Thread readerThread = null;

    private void reportError(@NonNull final Throwable e) {
        if (onMessageListener != null) onMessageListener.onMessage(e);
    }

    private final class ProcOutputR implements Runnable {
        private final byte[] buf = new byte[8192];
        private final InputStream stream;

        ProcOutputR(final InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final int len = stream.read(buf);
                    if (len < 0) return;
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
        public static final Map<String, PermMeta> permByName = new HashMap<>();

        static {
            permByName.put("favmgmt", new PermMeta(PERM_FAVMGMT, R.string.label_favorites_management));
            permByName.put("pluginexec", new PermMeta(PERM_PLUGINEXEC, R.string.label_plugins_execution));
        }

        public volatile long permissions = 0;

        public BackendUiInteraction ui;
    }

    private static final Map<Long, SessionData> sessionDataMap = new ConcurrentHashMap<>();
    private static SecureRandom rng = new SecureRandom();

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

    private SessionData sessionData = new SessionData();
    private long sessionToken = generateSessionToken();

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

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        terminalString = pp.getString("terminal_string", terminalString);
        execute = pp.getString("execute", execute);
        sessionData.permissions = 0;
        for (final Map.Entry<String, SessionData.PermMeta> m : SessionData.permByName.entrySet())
            if (pp.getBoolean("perm_" + m.getKey(), false))
                sessionData.permissions |= m.getValue().bits;
    }

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        output = stream;
    }

    @NonNull
    @Override
    public OutputStream getOutputStream() {
        return input;
    }

    @Override
    public void setOnMessageListener(final OnMessageListener l) {
        onMessageListener = l;
    }

    @Override
    public boolean isConnected() {
        return proc != null;
    }

    @Override
    public void connect() {
        final Map<String, String> env = new HashMap<>(System.getenv());
        env.put(BuildConfig.SHELL_SESSION_TOKEN_VAR, Long.toHexString(sessionToken).toUpperCase());
        env.put("TERM", terminalString);
        env.put("DATA_DIR", context.getApplicationInfo().dataDir);
        if (Build.VERSION.SDK_INT >= 24)
            env.put("PROTECTED_DATA_DIR", context.getApplicationInfo().deviceProtectedDataDir);
        final File extDataDir = context.getExternalFilesDir(null);
        if (extDataDir != null) {
            env.put("EXTERNAL_DATA_DIR", extDataDir.getAbsolutePath());
            env.put("SHARED_DATA_DIR", extDataDir.getAbsolutePath());
        }
        env.put("LIB_DIR", context.getApplicationInfo().nativeLibraryDir);
        env.put("APP_ID", BuildConfig.APPLICATION_ID);
        env.put("APP_VERSION", BuildConfig.VERSION_NAME);
        env.put("MY_DEVICE_ABIS", StringUtils.joinWith(" ", Misc.getAbis()));
        env.put("MY_ANDROID_SDK", Integer.toString(Build.VERSION.SDK_INT));
        synchronized (connectionLock) {
            proc = PtyProcess.system(execute, env);
            readerThread = new Thread(new ProcOutputR(proc.getInputStream()));
            readerThread.setDaemon(true);
            readerThread.start();
        }
    }

    @Override
    public void disconnect() {
        synchronized (connectionLock) {
            final Process p = proc;
            if (p == null) return;
            proc = null;
            p.destroy();
            try {
                readerThread.join();
            } catch (final InterruptedException ignored) {
            }
            readerThread = null;
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
        try {
            proc.resize(col, row, wp, hp);
        } catch (final IOException ignored) {
        }
    }

    @NonNull
    @Override
    public String getConnDesc() {
        return "Local Terminal";
    }

    /*
     * If the terminal is in a mode when it does not intercept some control bytes,
     * the functions below can be used to send appropriate signals to the foreground process group.
     * https://www.win.tue.nl/~aeb/linux/lk/lk-10.html
     */

    @Keep
    @ExportedUIMethod(titleRes = R.string.action_send_sigint, order = 0)
    public void sendSigInt() {
        proc.sendSignalToForeground(PtyProcess.SIGINT);
    }

    @Keep
    @ExportedUIMethod(titleRes = R.string.action_send_sighup, order = 1)
    public void sendSigHup() {
        proc.sendSignalToForeground(PtyProcess.SIGHUP);
    }

    /*
     * Tools (only `termsh' now) for interaction with the Android environment are supposed to be
     * controlled on what they can do. This function removes any granted permissions
     * from the current session permanently.
     */

    @Keep
    @ExportedUIMethod(titleRes = R.string.action_revoke_permissions, order = 2)
    public void revokePermissions() {
        sessionData.permissions = 0;
    }
}
