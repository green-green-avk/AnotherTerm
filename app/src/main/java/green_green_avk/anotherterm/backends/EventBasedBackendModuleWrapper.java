package green_green_avk.anotherterm.backends;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class EventBasedBackendModuleWrapper {
    private static final int MSG_KILL_SERV = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_READ = 3;
    private static final int MSG_CONNECTED = 4;
    private static final int MSG_DISCONNECTED = 5;
    private static final int MSG_STOP = 6;
    private static final int MSG_CONNECTING = 7;

    private static final int MSG_S_WRITE = 2;
    private static final int MSG_S_CONNECT = 3;
    private static final int MSG_S_DISCONNECT = 4;
    private static final int MSG_S_RESIZE = 5;

    @NonNull
    public final BackendModule wrapped;
    private volatile boolean isStopping = false;
    private volatile boolean isConnected = false;
    private volatile boolean isConnecting = false;
    private final Listener listener;

    private final Object readLock = new Object();

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull final Message msg) {
            if (listener == null) return;
            switch (msg.what) {
                case MSG_KILL_SERV:
                    destroy();
                    break;
                case MSG_STOP:
                    wrapped.stop();
                    break;
                case MSG_CONNECTING:
                    isConnecting = true;
                    listener.onConnecting();
                    break;
                case MSG_CONNECTED:
                    isConnecting = false;
                    isConnected = true;
                    listener.onConnected();
                    break;
                case MSG_DISCONNECTED:
                    if (msg.obj instanceof BackendModule.StateMessage)
                        listener.onMessage(((BackendModule.StateMessage) msg.obj).message);
                    isConnecting = false;
                    isConnected = false;
                    listener.onDisconnected();
                    break;
                case MSG_READ:
                    synchronized (readLock) {
                        try {
                            listener.onRead((ByteBuffer) msg.obj);
                        } finally {
                            readLock.notify();
                        }
                    }
                    break;
                case MSG_ERROR:
                    listener.onError((Throwable) msg.obj);
                    if (!wrapped.isConnected() && (isConnected || isConnecting)) {
                        isConnecting = false;
                        isConnected = false;
                        listener.onDisconnected();
                    }
                    break;
            }
        }
    };
    private final HandlerThread serviceThread = new HandlerThread("Backend service");
    private Handler serviceHandler;
    private final Object scrLock = new Object();
    private int scrC;
    private int scrR;
    private int scrW;
    private int scrH;

    public interface Listener {
        void onConnecting();

        void onConnected();

        void onDisconnected();

        void onRead(@NonNull ByteBuffer v);

        void onError(@NonNull Throwable e);

        void onMessage(@NonNull String m);
    }

    public EventBasedBackendModuleWrapper(@NonNull final BackendModule module,
                                          @NonNull final Listener listener) {
        wrapped = module;
        this.listener = listener;
        init();
    }

    public boolean isConnected() {
        return wrapped.isConnected();
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public void connect() {
        serviceHandler.sendEmptyMessage(MSG_S_CONNECT);
    }

    public void disconnect() {
        serviceHandler.sendEmptyMessage(MSG_S_DISCONNECT);
    }

    private void init() {
        wrapped.setOnMessageListener(new BackendModule.OnMessageListener() {
            @Override
            public void onMessage(@NonNull final Object msg) {
                if (msg instanceof Throwable)
                    handler.obtainMessage(MSG_ERROR, msg).sendToTarget();
                else if (msg instanceof BackendModule.DisconnectStateMessage)
                    handler.obtainMessage(MSG_DISCONNECTED, msg).sendToTarget();
            }
        });
        wrapped.setOutputStream(new OutputStream() {
            @Override
            public void close() throws IOException {
                handler.obtainMessage(MSG_DISCONNECTED).sendToTarget();
                super.close();
            }

            @Override
            public void write(final int b) {
                synchronized (readLock) {
                    handler.obtainMessage(MSG_READ, ByteBuffer.wrap(new byte[]{(byte) b})).sendToTarget();
                    try {
                        readLock.wait();
                    } catch (final InterruptedException ignored) {
                    }
                }
            }

            @Override
            public void write(@NonNull final byte[] b, final int off, final int len) {
                synchronized (readLock) {
                    handler.obtainMessage(MSG_READ, ByteBuffer.wrap(b, off, len)).sendToTarget();
                    try {
                        readLock.wait();
                    } catch (final InterruptedException ignored) {
                    }
                }
            }

            @Override
            public void write(@NonNull final byte[] b) {
                synchronized (readLock) {
                    handler.obtainMessage(MSG_READ, ByteBuffer.wrap(b)).sendToTarget();
                    try {
                        readLock.wait();
                    } catch (final InterruptedException ignored) {
                    }
                }
            }
        });
        serviceThread.setDaemon(true);
        serviceThread.start();
        serviceHandler = new Handler(serviceThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull final Message msg) {
                try {
                    switch (msg.what) {
                        case MSG_S_WRITE: {
                            final OutputStream os = wrapped.getOutputStream();
                            try {
                                os.write((byte[]) msg.obj);
                                os.flush();
                            } catch (final BackendException | IOException e) {
                                handler.obtainMessage(MSG_ERROR, e).sendToTarget();
                            }
                            break;
                        }
                        case MSG_S_CONNECT:
                            try {
                                handler.sendEmptyMessage(MSG_CONNECTING);
                                wrapped.connect();
                                handler.sendEmptyMessage(MSG_CONNECTED);
                            } catch (final BackendException e) {
                                handler.obtainMessage(MSG_ERROR, e).sendToTarget();
                            }
                            break;
                        case MSG_S_DISCONNECT:
                            try {
                                wrapped.disconnect();
                                handler.sendEmptyMessage(MSG_DISCONNECTED);
                            } catch (final BackendException e) {
                                handler.obtainMessage(MSG_ERROR, e).sendToTarget();
                            }
                            break;
                        case MSG_S_RESIZE: {
                            final int _scrC, _scrR, _scrW, _scrH;
                            synchronized (scrLock) {
                                _scrC = scrC;
                                _scrR = scrR;
                                _scrW = scrW;
                                _scrH = scrH;
                            }
                            try {
                                wrapped.resize(_scrC, _scrR, _scrW, _scrH);
                            } catch (final BackendException e) {
                                handler.obtainMessage(MSG_ERROR, e).sendToTarget();
                            }
                            break;
                        }
                    }
                } catch (final BackendInterruptedException e) {
                    if (isStopping) return; // Good
                    // Your hamster seems broken...
                    handler.obtainMessage(MSG_ERROR, e).sendToTarget();
                    handler.sendEmptyMessage(MSG_DISCONNECTED);
                    isStopping = true;
                    Looper.myLooper().quit(); // Bailing out
                }
            }
        };
    }

    @Override
    protected void finalize() throws Throwable {
//        destroy();
        Log.d("Backend service", "Successfully reclaimed");
        super.finalize();
    }

    public void write(final byte[] v) {
        serviceHandler.obtainMessage(MSG_S_WRITE, v).sendToTarget();
    }

    public void resize(final int col, final int row, final int wp, final int hp) {
        synchronized (scrLock) {
            scrC = col;
            scrR = row;
            scrW = wp;
            scrH = hp;
        }
        if (!serviceHandler.hasMessages(MSG_S_RESIZE))
            serviceHandler.sendEmptyMessage(MSG_S_RESIZE);
    }

    public void stop() {
        disconnect();
        handler.sendEmptyMessageDelayed(MSG_STOP, 1000); // Graceful
        handler.sendEmptyMessageDelayed(MSG_KILL_SERV, 3000);
    }

    public void destroy() {
        serviceThread.quit();
        isStopping = true;
        serviceThread.interrupt();
        handler = new Handler();
    }
}
