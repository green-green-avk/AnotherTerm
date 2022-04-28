package green_green_avk.anotherterm.backends;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public final class EventBasedBackendModuleWrapper {
    private static final int MSG_KILL_SERV = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_READ = 3;
    private static final int MSG_CONNECTED = 4;
    private static final int MSG_DISCONNECTED = 5;
    private static final int MSG_STOP = 6;
    private static final int MSG_CONNECTING = 7;
    private static final int MSG_MESSAGE = 8;

    private static final int MSG_S_WRITE = 2;
    private static final int MSG_S_CONNECT = 3;
    private static final int MSG_S_DISCONNECT = 4;
    private static final int MSG_S_RESIZE = 5;
    private static final int MSG_S_METHOD = 0x10;

    @NonNull
    public final BackendModule wrapped;
    private volatile boolean isStopped = false;
    private volatile boolean isStopping = false;
    private volatile boolean isConnected = false;
    private volatile boolean isConnecting = false;
    private final Listener listener;

    private final Object readLock = new Object();

    private final Handler handler = new Handler(Looper.myLooper()) {
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
                case MSG_MESSAGE:
                    listener.onMessage(msg.obj.toString());
                    break;
            }
        }
    };
    private final HandlerThread serviceThread = new HandlerThread("Backend service");
    private final Handler serviceHandler;
    private final Object scrLock = new Object();
    private int scrC;
    private int scrR;
    private int scrW;
    private int scrH;

    private boolean sendEvent(final int what) {
        return !isStopped && handler.sendEmptyMessage(what);
    }

    private boolean sendEvent(final int what, final long delayMillis) {
        return !isStopped && handler.sendEmptyMessageDelayed(what, delayMillis);
    }

    private boolean sendEvent(final int what, @Nullable final Object obj) {
        return !isStopped && handler.sendMessage(handler.obtainMessage(what, obj));
    }

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
        wrapped.setOnMessageListener(new BackendModule.OnMessageListener() {
            @Override
            public void onMessage(@NonNull final Object msg) {
                if (msg instanceof Throwable)
                    sendEvent(MSG_ERROR, msg);
                else if (msg instanceof String)
                    sendEvent(MSG_MESSAGE, msg);
                else if (msg instanceof BackendModule.DisconnectStateMessage)
                    sendEvent(MSG_DISCONNECTED, msg);
            }
        });
        wrapped.setOutputStream(new OutputStream() {
            @Override
            public void close() throws IOException {
                sendEvent(MSG_DISCONNECTED);
                super.close();
            }

            private void _write(@NonNull final ByteBuffer b) {
                synchronized (readLock) {
                    sendEvent(MSG_READ, b);
                    try {
                        readLock.wait();
                    } catch (final InterruptedException ignored) {
                    }
                }
            }

            @Override
            public void write(final int b) {
                _write(ByteBuffer.wrap(new byte[]{(byte) b}));
            }

            @Override
            public void write(@NonNull final byte[] b, final int off, final int len) {
                _write(ByteBuffer.wrap(b, off, len));
            }

            @Override
            public void write(@NonNull final byte[] b) {
                _write(ByteBuffer.wrap(b));
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
                                sendEvent(MSG_ERROR, e);
                            }
                            break;
                        }
                        case MSG_S_CONNECT:
                            try {
                                sendEvent(MSG_CONNECTING);
                                wrapped.connect();
                                sendEvent(MSG_CONNECTED);
                            } catch (final BackendException e) {
                                sendEvent(MSG_ERROR, e);
                            }
                            break;
                        case MSG_S_DISCONNECT:
                            try {
                                wrapped.disconnect();
                                sendEvent(MSG_DISCONNECTED);
                            } catch (final BackendException e) {
                                sendEvent(MSG_ERROR, e);
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
                                sendEvent(MSG_ERROR, e);
                            }
                            break;
                        }
                        case MSG_S_METHOD: {
                            try {
                                ((Runnable) msg.obj).run();
                            } catch (final BackendException e) {
                                sendEvent(MSG_MESSAGE, e.getMessage());
                            }
                        }
                    }
                } catch (final BackendInterruptedException e) {
                    if (isStopped) return; // Good
                    // Your hamster seems broken...
                    sendEvent(MSG_ERROR, e);
                    sendEvent(MSG_DISCONNECTED);
                    isStopped = true;
                    Looper.myLooper().quit(); // Bailing out
                }
            }
        };
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
        if (!isStopping) {
            disconnect();
            handler.sendEmptyMessageDelayed(MSG_STOP, 1000); // Graceful
            handler.sendEmptyMessageDelayed(MSG_KILL_SERV, 3000);
            isStopping = true;
        }
    }

    public void destroy() {
        serviceThread.quit();
        isStopped = true;
        serviceThread.interrupt();
    }

    public Object callWrappedMethod(@NonNull final Method m, final Object... args) {
        final BackendModule.ExportedUIMethodOnThread ta =
                m.getAnnotation(BackendModule.ExportedUIMethodOnThread.class);
        if (ta != null) {
            if (m.getReturnType() != Void.TYPE)
                throw new NotImplementedException("We can't return values from a thread yet");
            if (ta.thread() != BackendModule.ExportedUIMethodOnThread.Thread.WRITE)
                throw new NotImplementedException("Unsupported thread type");
            final Message msg = serviceHandler.obtainMessage(MSG_S_METHOD,
                    (Runnable) () -> {
                        try {
                            wrapped.callMethod(m, args);
                        } catch (final BackendException e) {
                            throw e;
                        } catch (final Exception ignored) {
                        }
                    });
            if (ta.before())
                serviceHandler.sendMessageAtFrontOfQueue(msg);
            else
                msg.sendToTarget();
            return null;
        } else
            return wrapped.callMethod(m, args);
    }
}
