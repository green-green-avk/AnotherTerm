package green_green_avk.anotherterm.backends.local;

import android.os.Build;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.ptyprocess.PtyProcess;

public final class LocalModule extends BackendModule {

    @Keep
    public static final Meta meta = new Meta(LocalModule.class, "local-terminal");

    private final Object connectionLock = new Object();

    private PtyProcess proc = null;
    private final OutputStream input = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            if (proc == null) return;
            proc.getOutputStream().write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (proc == null) return;
            proc.getOutputStream().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
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

    private String terminalString = "xterm";
    private String execute = "";

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        terminalString = pp.getString("terminal_string", terminalString);
        execute = pp.getString("execute", execute);
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
    protected void finalize() throws Throwable {
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
    @ExportedUIMethod(titleRes = R.string.action_send_sigint)
    public void sendSigInt() {
        proc.sendSignalToForeground(PtyProcess.SIGINT);
    }

    @Keep
    @ExportedUIMethod(titleRes = R.string.action_send_sighup)
    public void sendSigHup() {
        proc.sendSignalToForeground(PtyProcess.SIGHUP);
    }
}
