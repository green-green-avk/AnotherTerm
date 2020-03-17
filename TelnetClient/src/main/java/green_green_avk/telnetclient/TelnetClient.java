package green_green_avk.telnetclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
// org.apache.commons.net.telnet seems... nope int rems of efficiency, just nope.

// No Android dependencies are required.

public class TelnetClient {

    public static class Cmd {
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

    protected static final ByteBuffer eraseLine = ByteBuffer.wrap(new byte[]{0x1B, '[', '1', 'K'});

    protected static int indexOf(@NonNull byte[] buf, int start, int end, byte v) {
        if (end > buf.length || end < 0) end = buf.length;
        if (start > end || start < 0) throw new IllegalArgumentException();
        for (int i = start; i < end; ++i) {
            if (v == buf[i]) return i;
        }
        return -1;
    }

    protected static int indexOf(@NonNull final ByteBuffer buf, int start, int end, final byte v) {
        if (buf.hasArray()) {
            if (start < 0) start = buf.position();
            final int r = indexOf(buf.array(),
                    start + buf.arrayOffset(),
                    end < 0 || end > buf.limit() ? buf.limit() : end,
                    v);
            if (r < 0) return r;
            else return r - buf.arrayOffset();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static int uv(final byte v) {
        return (int) v & 0xFF;
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
            try {
                client.sendRaw(v);
            } catch (final TelnetClientException e) {
                client.reportError(e);
            }
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

    protected final OptionHandler defaultOptionHamdler = new OptionHandler();

    protected final OptionHandler[] optionHandlers = new OptionHandler[256];

    {
        for (int i = 0; i < optionHandlers.length; ++i) setOptionHandler(i, defaultOptionHamdler);
    }

    public OptionHandler getOptionHandler(final int id) {
        return optionHandlers[id];
    }

    // OptionHandlers are supposed to be dedicated to some current TelnetClient object
    // and should not live longer than it.
    public void setOptionHandler(final int id, @Nullable final OptionHandler handler) {
        synchronized (optionsLock) {
            if (optionHandlers[id] == handler) return;
            if (optionHandlers[id] != null) {
                if (isConnected())
                    optionHandlers[id].onRemove(id);
            }
            optionHandlers[id] = handler != null ? handler : defaultOptionHamdler;
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
            Collections.newSetFromMap(new WeakHashMap<OnEventListener, Boolean>());

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

    public static class Markup extends ArrayList<Mark> {
        protected Markup() {
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

        public void release(@NonNull final Iterable<Mark> marks) {
            for (final Mark m : marks) release(m);
        }
    }

    protected final ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[8192]); // TODO: Or direct?
    protected boolean hasLeftovers = false;
    protected final Markup markup = new Markup();
    protected final MarkPool markPool = new MarkPool();
    protected volatile Socket socket = null;
    protected volatile InputStream inputSocketStream = null;
    protected volatile OutputStream outputSocketStream = null;
    protected volatile boolean mIsConnected = false;
    protected Thread readerThread = null;

    public interface OnErrorListener {
        void onError(Throwable e);
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
            if (onErrorListener != null) onErrorListener.onError(e);
        }
    }

    public boolean isConnected() {
        if (!mIsConnected) return false;
        final Socket s = socket;
        return s != null && !s.isClosed();
    }

    public void connect(final String addr, final int port) {
        synchronized (connectionLock) {
            if (mIsConnected) disconnect();
            try {
                socket = new Socket(addr, port);
                inputSocketStream = socket.getInputStream();
                outputSocketStream = socket.getOutputStream();
            } catch (final IOException e) {
                disconnect();
                throw new TelnetClientException(e);
            }
            readerThread = new Thread(reader);
            readerThread.setDaemon(true);
            readerThread.start();
            synchronized (optionsLock) {
                for (int i = 0; i < optionHandlers.length; ++i) {
                    final OptionHandler oh = optionHandlers[i];
                    if (oh != null) oh.onInit(i);
                }
            }
            startKeepAlive();
            mIsConnected = true;
        }
    }

    public void disconnect() {
        synchronized (connectionLock) {
            if (!mIsConnected) return;
            mIsConnected = false;
            stopKeepAlive();
            if (socket != null)
                try {
                    socket.close();
                } catch (final IOException ignored) {
                }
            socket = null;
            inputSocketStream = null; // Discard remaining buffers after close
            outputSocketStream = null;
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

    public void sendRaw(@NonNull final byte... buf) {
        sendRaw(buf, 0, buf.length);
    }

    public void sendRaw(@NonNull final byte[] buf, final int start, final int end) {
        if (start >= end) return;
        synchronized (sendLock) {
            try {
                final OutputStream oss = outputSocketStream;
                if (oss != null)
                    oss.write(buf, start, end - start);
            } catch (final IOException e) {
                throw new TelnetClientException(e);
            }
        }
    }

    public void send(@NonNull final byte... buf) {
        send(buf, 0, buf.length);
    }

    public void send(@NonNull final byte[] buf, final int start, final int end) {
        for (final OnEventListener l : onEventListeners) l.onSend(buf);
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
                if (outputStream != null) outputStream.write(buffer, start, end - start);
            }
        } catch (final IOException e) {
            reportError(e);
        }
    }

    public void inject(@NonNull final ByteBuffer buffer) {
        inject(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.limit());
    }

    protected final Runnable reader = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    final Markup mu = receive();
                    if (mu == null) return;
                    for (final Mark m : mu) {
                        if (m.type == Mark.Type.DATA) {
                            inject(m.buffer);
                        }
                    }
                } catch (final TelnetClientException e) {
                    if (mIsConnected) reportError(e);
                    return;
                }
            }
        }
    };

    protected Markup receive() {
        markPool.release(markup);
        markup.clear();
        if (!hasLeftovers) inputBuffer.clear();
        hasLeftovers = false;
        final int len;
        final InputStream iss = inputSocketStream;
        if (iss == null) return null;
        try {
            len = iss.read(inputBuffer.array(),
                    inputBuffer.arrayOffset() + inputBuffer.position(),
                    inputBuffer.limit());
        } catch (final IOException e) {
            throw new TelnetClientException(e);
        }
        if (len < 0) return null; // EOF
        inputBuffer.limit(inputBuffer.position() + len);
        while (true) {
            if (inputBuffer.remaining() == 0) return markup;
            final int pos = indexOf(inputBuffer, -1, -1, Cmd.IAC);
            if (pos < 0) {
                markup.add(markPool.obtain(inputBuffer, Mark.Type.DATA));
                return markup;
            } else {
                if (pos > inputBuffer.position()) {
                    final ByteBuffer b = inputBuffer.duplicate();
                    b.limit(pos);
                    markup.add(markPool.obtain(b, Mark.Type.DATA));
                    inputBuffer.position(pos);
                }
                final int e = parseEscape(inputBuffer);
                if (e < 0) { // partial escape
                    if (inputBuffer.position() == 0 && inputBuffer.limit() == inputBuffer.capacity()) {
                        // too long escape
                        markup.add(markPool.obtain(inputBuffer, Mark.Type.DATA));
                        return markup;
                    }
                    inputBuffer.compact();
                    hasLeftovers = true;
                    return markup;
                }
                final ByteBuffer b = inputBuffer.duplicate();
                b.limit(e);
                markup.add(markPool.obtain(b, Mark.Type.ESCAPE));
                inputBuffer.position(e);
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
                        if (handler != null) handler.onDo(i);
                        return buf.position();
                    }
                    case Cmd.DONT: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null) handler.onDont(i);
                        return buf.position();
                    }
                    case Cmd.WILL: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null) handler.onWill(i);
                        return buf.position();
                    }
                    case Cmd.WONT: {
                        final int i = uv(buf.get());
                        final OptionHandler handler = optionHandlers[i];
                        if (handler != null) handler.onWont(i);
                        return buf.position();
                    }
                    case Cmd.SB: {
                        int e = buf.position();
                        do {
                            e = indexOf(buf, e, -1, Cmd.IAC);
                            if (e < 0) return -1;
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
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    protected int keepAliveInterval = 0;

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(final int interval) {
        keepAliveInterval = interval;
        applyKeepAlive();
    }

    protected void applyKeepAlive() {
        synchronized (keepAliveLock) {
            if (keepAliveTask != null) {
                keepAliveTask.cancel();
                keepAliveTask = null;
            }
            if (keepAliveTimer == null) return;
            keepAliveTimer.purge();
            if (keepAliveInterval > 0) {
                keepAliveTask = new KeepAliveTask();
                keepAliveTimer.schedule(keepAliveTask,
                        (long) keepAliveInterval * 1000, (long) keepAliveInterval * 1000);
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
            } catch (TelnetClientException e) {
                reportError(e);
            }
        }
    }

    protected final byte[] keepAliveMessage = {Cmd.IAC, Cmd.NOP};
}
