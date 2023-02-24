package green_green_avk.telnetclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

// Netty seems excessively bulky and
// org.apache.commons.net.telnet seems... nope in terms of efficiency, just nope.

// No Android dependencies are required.

public class TelnetClient {
    public static final class Cmd {
        private Cmd() {
        }

        public static final byte SE = (byte) 240;
        public static final byte NOP = (byte) 241;
        public static final byte DM = (byte) 242;
        public static final byte BRK = (byte) 243;
        public static final byte IP = (byte) 244;
        public static final byte AO = (byte) 245;
        public static final byte AYT = (byte) 246;
        public static final byte EC = (byte) 247;
        public static final byte EL = (byte) 248;
        public static final byte GA = (byte) 249;
        public static final byte SB = (byte) 250;

        public static final byte WILL = (byte) 251;
        public static final byte WONT = (byte) 252;
        public static final byte DO = (byte) 253;
        public static final byte DONT = (byte) 254;

        public static final byte IAC = (byte) 255;
    }

    protected static final byte[] IAC = {Cmd.IAC};

    protected static final ByteBuffer eraseChar = ByteBuffer.wrap(new byte[]{8});

    protected static final ByteBuffer eraseLine =
            ByteBuffer.wrap(new byte[]{0x1B, '[', '1', 'K'});

    protected static int indexOf(@NonNull final byte[] buf,
                                 final int start, int end, final byte v) {
        if (end > buf.length || end < 0)
            end = buf.length;
        if (start > end || start < 0)
            throw new IllegalArgumentException();
        for (int i = start; i < end; ++i) {
            if (v == buf[i])
                return i;
        }
        return -1;
    }

    protected static int indexOf(@NonNull final ByteBuffer buf,
                                 final int start, final int end, final byte v) {
        if (buf.hasArray()) {
            final int r = indexOf(buf.array(),
                    buf.arrayOffset() + (start < 0 ? buf.position() : start),
                    buf.arrayOffset() + (end < 0 || end > buf.limit() ? buf.limit() : end),
                    v);
            return r < 0 ? r : r - buf.arrayOffset();
        }
        throw new UnsupportedOperationException();
    }

    public static int uv(final byte v) {
        return v & 0xFF;
    }

    public static byte[] escape(@Nullable final byte[] value) {
        if (value == null) return null;
        int len = value.length;
        int p = 0;
        while ((p = indexOf(value, p, -1, Cmd.IAC)) >= 0) {
            ++p;
            ++len;
        }
        if (len == value.length) return value;
        final byte[] r = new byte[len];
        p = 0;
        int d = 0;
        int e;
        while ((e = indexOf(value, p, -1, Cmd.IAC)) >= 0) {
            ++e;
            System.arraycopy(value, p, r, p + d, e - p);
            r[e + d] = Cmd.IAC;
            ++d;
            p = e;
        }
        System.arraycopy(value, p, r, p + d, value.length - p);
        return r;
    }

    protected final Object connectionLock = new Object();
    protected final Object sendLock = new Object();
    protected final Object dataToUserLock = new Object();
    protected final Object optionsLock = new Object();
    protected final Object keepAliveLock = new Object();

    public Object getSendLock() {
        return sendLock;
    }

    public static class OptionHandler {
        protected static byte[] msgWill(final int id) {
            return new byte[]{Cmd.IAC, Cmd.WILL, (byte) id};
        }

        protected static byte[] msgWont(final int id) {
            return new byte[]{Cmd.IAC, Cmd.WONT, (byte) id};
        }

        protected static byte[] msgDo(final int id) {
            return new byte[]{Cmd.IAC, Cmd.DO, (byte) id};
        }

        protected static byte[] msgDont(final int id) {
            return new byte[]{Cmd.IAC, Cmd.DONT, (byte) id};
        }

        protected static byte[] msgSub(final int id, final byte[] prefix, final byte... body) {
            final byte[] eBody = escape(body);
            final int pLen = (prefix != null) ? prefix.length : 0;
            final int bLen = (eBody != null) ? eBody.length : 0;
            final byte[] m = new byte[5 + pLen + bLen];
            m[0] = Cmd.IAC;
            m[1] = Cmd.SB;
            m[2] = (byte) id;
            m[pLen + bLen + 3] = Cmd.IAC;
            m[pLen + bLen + 4] = Cmd.SE;
            if (pLen > 0)
                System.arraycopy(prefix, 0, m, 3, pLen);
            if (bLen > 0)
                System.arraycopy(eBody, 0, m, 3 + pLen, bLen);
            return m;
        }

        protected TelnetClient client = null;

        protected void sendRaw(final byte... v) {
            client.sendRaw(v);
        }

        protected int id() {
            return -1;
        }

        protected void onInit(final int id) {
        }

        protected void onRemove(final int id) {
        }

        protected void onWill(final int id) {
            sendRaw(msgDont(id));
        }

        protected void onWont(final int id) {
        }

        protected void onDo(final int id) {
            sendRaw(msgWont(id));
        }

        protected void onDont(final int id) {
        }

        protected void onSub(final int id, @NonNull final ByteBuffer sub) {
        }
    }

    protected final OptionHandler defaultOptionHandler = new OptionHandler();

    protected final OptionHandler[] optionHandlers = new OptionHandler[256];

    {
        for (int i = 0; i < optionHandlers.length; ++i)
            setOptionHandler(i, defaultOptionHandler);
    }

    public OptionHandler getOptionHandler(final int id) {
        return optionHandlers[id];
    }

    // OptionHandlers are supposed to be dedicated to some current TelnetClient object
    // and should not live longer than it.
    public void setOptionHandler(final int id, @Nullable final OptionHandler handler) {
        synchronized (optionsLock) {
            if (optionHandlers[id] == handler)
                return;
            if (optionHandlers[id] != null) {
                if (isConnected())
                    optionHandlers[id].onRemove(id);
            }
            optionHandlers[id] = handler != null ? handler : defaultOptionHandler;
            optionHandlers[id].client = this;
            if (isConnected())
                optionHandlers[id].onInit(id);
        }
    }

    public void setOptionHandler(@NonNull final OptionHandler handler) {
        setOptionHandler(handler.id(), handler);
    }

    public void removeOptionHandler(final int id) {
        setOptionHandler(id, null);
    }

    public interface OnEventListener {
        void onSend(@NonNull byte[] buffer);
    }

    protected final Set<OnEventListener> onEventListeners =
            Collections.newSetFromMap(new WeakHashMap<>());

    public void addOnEventListener(@NonNull final OnEventListener l) {
        onEventListeners.add(l);
    }

    public void removeOnEventListener(@NonNull final OnEventListener l) {
        onEventListeners.remove(l);
    }

    public static class Mark {
        public enum Type {
            DATA, ESCAPE
        }

        public ByteBuffer buffer;
        public Type type;
    }

    public static final class Markup extends ArrayList<Mark> {
        private Markup() {
            super(64);
        }
    }

    public static final class MarkPool {
        private final Set<Mark> marks = new HashSet<>();

        @NonNull
        public Mark obtain(@NonNull final ByteBuffer buffer, @NonNull final Mark.Type type) {
            if (marks.isEmpty()) {
                final Mark m = new Mark();
                m.buffer = buffer;
                m.type = type;
                return m;
            } else {
                final Iterator<Mark> i = marks.iterator();
                final Mark m = i.next();
                i.remove();
                m.buffer = buffer;
                m.type = type;
                return m;
            }
        }

        public void release(@NonNull final Mark mark) {
            marks.add(mark);
        }

        public void release(@NonNull final Iterable<? extends Mark> marks) {
            for (final Mark m : marks)
                release(m);
        }
    }

    protected final ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[8192]);
    protected boolean hasLeftovers = false;
    protected final Markup markup = new Markup();
    protected final MarkPool markPool = new MarkPool();
    protected volatile Socket socket = null;
    protected boolean isOwnedSocket = true;
    protected volatile InputStream inputSocketStream = null;
    protected volatile OutputStream outputSocketStream = null;
    protected volatile boolean mIsConnected = false;
    protected Thread readerThread = null;

    public interface OnErrorListener {
        void onError(@NonNull Throwable e);
    }

    protected OutputStream outputStream = null;
    protected OnErrorListener onErrorListener = null;

    public void setOutputStream(@Nullable final OutputStream s) {
        synchronized (dataToUserLock) {
            outputStream = s;
        }
    }

    public void setOnErrorListener(@Nullable final OnErrorListener l) {
        synchronized (dataToUserLock) {
            onErrorListener = l;
        }
    }

    protected void reportError(@NonNull final Throwable e) {
        synchronized (dataToUserLock) {
            if (onErrorListener != null)
                onErrorListener.onError(e);
        }
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    protected void initConnection() {
        readerThread = new Thread(reader);
        readerThread.setDaemon(true);
        readerThread.start();
        synchronized (optionsLock) {
            for (int i = 0; i < optionHandlers.length; ++i) {
                final OptionHandler oh = optionHandlers[i];
                if (oh != null)
                    oh.onInit(i);
            }
        }
        startKeepAlive();
        mIsConnected = true;
    }

    /**
     * Uses an already connected socket.
     *
     * @param connectedSocket to use
     * @param adopt           the provided socket will not be closed on {@link #disconnect()}
     *                        if {@code false}.
     */
    public void connect(@NonNull final Socket connectedSocket, final boolean adopt) {
        synchronized (connectionLock) {
            if (mIsConnected)
                tearDownConnection();
            isOwnedSocket = adopt;
            socket = connectedSocket;
            try {
                inputSocketStream = socket.getInputStream();
                outputSocketStream = socket.getOutputStream();
            } catch (final IOException e) {
                tearDownConnection();
                throw new TelnetClientConnectionException(e);
            }
            initConnection();
        }
    }

    /**
     * Connects.
     *
     * @param hostname destination hostname ({@code null} for loopback)
     * @param port     destination port
     * @param timeout  connect timeout in milliseconds ({@code 0} for no timeout)
     * @param proxy    a proxy server to use
     */
    public void connect(@Nullable final String hostname, final int port,
                        final int timeout, @NonNull final Proxy proxy) {
        synchronized (connectionLock) {
            if (mIsConnected)
                tearDownConnection();
            isOwnedSocket = true;
            try {
                socket = new Socket(proxy);
                socket.connect(new InetSocketAddress(hostname, port), timeout);
                inputSocketStream = socket.getInputStream();
                outputSocketStream = socket.getOutputStream();
            } catch (final IOException | IllegalArgumentException | SecurityException e) {
                tearDownConnection();
                throw new TelnetClientConnectionException(e);
            }
            initConnection();
        }
    }

    protected void tearDownConnection() {
        mIsConnected = false;
        stopKeepAlive();
        if (isOwnedSocket) {
            final Socket s = socket;
            if (s != null) {
                try {
                    s.close();
                } catch (final IOException ignored) {
                }
            }
        } else {
            final OutputStream oss = outputSocketStream;
            if (oss != null) {
                try {
                    oss.flush();
                } catch (final IOException ignored) {
                }
            }
        }
        socket = null;
        inputSocketStream = null;
        outputSocketStream = null;
        if (readerThread != null) {
            if (readerThread != Thread.currentThread()) {
                try {
                    readerThread.join();
                } catch (final InterruptedException ignored) {
                }
            }
            readerThread = null;
        }
    }

    protected void killConnection() {
        disconnect();
    }

    public void disconnect() {
        synchronized (connectionLock) {
            if (!mIsConnected)
                return;
            tearDownConnection();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        tearDownConnection();
        super.finalize();
    }

    public void sendRaw(@NonNull final byte... buf) {
        sendRaw(buf, 0, buf.length);
    }

    public void sendRaw(@NonNull final byte[] buf, final int start, final int end) {
        if (start >= end)
            return;
        synchronized (sendLock) {
            try {
                final OutputStream oss = outputSocketStream;
                if (oss != null)
                    oss.write(buf, start, end - start);
            } catch (final IOException e) {
                killConnection();
                throw new TelnetClientConnectionException(e);
            }
        }
    }

    public void send(@NonNull final byte... buf) {
        send(buf, 0, buf.length);
    }

    public void send(@NonNull final byte[] buf, final int start, final int end) {
        for (final OnEventListener l : onEventListeners)
            l.onSend(buf);
        int b = start;
        int e;
        synchronized (sendLock) {
            while ((e = indexOf(buf, b, end, Cmd.IAC)) >= 0) {
                ++e;
                sendRaw(buf, b, e);
                sendRaw(IAC, 0, IAC.length);
                b = e;
            }
            sendRaw(buf, b, end);
        }
    }

    public void inject(@NonNull final byte[] buffer, final int start, final int end) {
        try {
            synchronized (dataToUserLock) {
                if (outputStream != null)
                    outputStream.write(buffer, start, end - start);
            }
        } catch (final IOException e) {
            reportError(e);
        }
    }

    public void inject(@NonNull final ByteBuffer buffer) {
        inject(buffer.array(),
                buffer.arrayOffset() + buffer.position(),
                buffer.arrayOffset() + buffer.limit());
    }

    protected final Runnable reader = () -> {
        while (true) {
            try {
                final Markup mu = receive();
                for (final Mark m : mu) {
                    if (m.type == Mark.Type.DATA) {
                        inject(m.buffer);
                    }
                }
            } catch (final TelnetClientException e) {
                reportError(e);
                return;
            }
        }
    };

    @NonNull
    protected Markup receive() {
        markPool.release(markup);
        markup.clear();
        if (!hasLeftovers)
            inputBuffer.clear();
        else
            inputBuffer.compact();
        hasLeftovers = false;
        final int len;
        final InputStream iss = inputSocketStream;
        if (iss == null) {
            killConnection();
            throw new TelnetClientInterruptedException();
        }
        try {
            len = iss.read(inputBuffer.array(),
                    inputBuffer.arrayOffset() + inputBuffer.position(),
                    inputBuffer.limit() - inputBuffer.position());
        } catch (final IOException e) {
            killConnection();
            throw new TelnetClientConnectionException(e);
        }
        if (len < 0) {
            killConnection();
            throw new TelnetClientEOFException();
        }
        inputBuffer.limit(inputBuffer.position() + len);
        while (true) {
            if (inputBuffer.remaining() == 0)
                return markup;
            final int escPos = indexOf(inputBuffer, -1, -1, Cmd.IAC);
            if (escPos < 0) {
                markup.add(markPool.obtain(inputBuffer, Mark.Type.DATA));
                return markup;
            } else {
                if (escPos > inputBuffer.position()) {
                    final ByteBuffer b = inputBuffer.duplicate();
                    b.limit(escPos);
                    markup.add(markPool.obtain(b, Mark.Type.DATA));
                    inputBuffer.position(escPos);
                }
                final int escEnd = parseEscape(inputBuffer);
                if (escEnd < 0) { // partial escape
                    if (inputBuffer.position() == 0 &&
                            inputBuffer.limit() == inputBuffer.capacity()) {
                        // too long escape
                        markup.add(markPool.obtain(inputBuffer, Mark.Type.DATA));
                        return markup;
                    }
                    hasLeftovers = true;
                    return markup;
                }
                final ByteBuffer b = inputBuffer.duplicate();
                b.limit(escEnd);
                markup.add(markPool.obtain(b, Mark.Type.ESCAPE));
                inputBuffer.position(escEnd);
            }
        }
    }

    protected int parseEscape(@NonNull final ByteBuffer buffer) {
        final ByteBuffer buf = buffer.duplicate();
        buf.get();
        try {
            synchronized (optionsLock) {
                switch (buf.get()) {
                    case Cmd.IAC:
                        return buf.position() - 1;
                    case Cmd.EC: // surrogate (not precise)
                        markup.add(markPool.obtain(eraseChar, Mark.Type.DATA));
                        return buf.position();
                    case Cmd.EL: // surrogate (not precise)
                        markup.add(markPool.obtain(eraseLine, Mark.Type.DATA));
                        return buf.position();
                    case Cmd.DO: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null)
                            handler.onDo(i);
                        return buf.position();
                    }
                    case Cmd.DONT: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null)
                            handler.onDont(i);
                        return buf.position();
                    }
                    case Cmd.WILL: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null)
                            handler.onWill(i);
                        return buf.position();
                    }
                    case Cmd.WONT: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null)
                            handler.onWont(i);
                        return buf.position();
                    }
                    case Cmd.SB: {
                        int e = buf.position();
                        do {
                            e = indexOf(buf, e, -1, Cmd.IAC);
                            if (e < 0)
                                return -1;
                        } while (buf.get(e + 1) != Cmd.SE);
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null) {
                            final ByteBuffer b = buf.duplicate();
                            b.limit(e);
                            handler.onSub(i, b);
                        }
                        return e + 2;
                    }
                    default:
                        return buf.position();
                }
            }
        } catch (final BufferUnderflowException e) {
            return -1;
        }
    }

    protected long keepAliveInterval = 0;

    /**
     * Returns the interval to send keep-alive messages.
     *
     * @return the interval, in milliseconds.
     * @see #setKeepAliveInterval(long)
     */
    public long getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Sets the interval to send keep-alive messages.
     * <p>
     * Never by default.
     *
     * @param interval in milliseconds, {@code 0} - never.
     * @see #getKeepAliveInterval()
     */
    public void setKeepAliveInterval(final long interval) {
        keepAliveInterval = interval;
        applyKeepAlive();
    }

    protected void applyKeepAlive() {
        synchronized (keepAliveLock) {
            if (keepAliveTask != null) {
                keepAliveTask.cancel();
                keepAliveTask = null;
            }
            if (keepAliveTimer == null)
                return;
            keepAliveTimer.purge();
            if (keepAliveInterval > 0) {
                keepAliveTask = new KeepAliveTask();
                keepAliveTimer.schedule(keepAliveTask,
                        keepAliveInterval, keepAliveInterval);
            }
        }
    }

    protected void startKeepAlive() {
        synchronized (keepAliveLock) {
            keepAliveTimer = new Timer(true);
            applyKeepAlive();
        }
    }

    protected void stopKeepAlive() {
        synchronized (keepAliveLock) {
            if (keepAliveTask != null) {
                keepAliveTask.cancel();
                keepAliveTask = null;
            }
            if (keepAliveTimer != null) {
                keepAliveTimer.cancel();
                keepAliveTimer = null;
            }
        }
    }

    protected Timer keepAliveTimer = null;

    protected KeepAliveTask keepAliveTask = null;

    protected final class KeepAliveTask extends TimerTask {
        @Override
        public void run() {
            try {
                sendRaw(keepAliveMessage);
            } catch (final TelnetClientException e) {
                reportError(e);
            }
        }
    }

    protected final byte[] keepAliveMessage = {Cmd.IAC, Cmd.NOP};
}
