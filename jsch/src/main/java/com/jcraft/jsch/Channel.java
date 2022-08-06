/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002-2018 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Channel {

    static final int SSH_MSG_CHANNEL_OPEN_CONFIRMATION = 91;
    static final int SSH_MSG_CHANNEL_OPEN_FAILURE = 92;
    static final int SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;

    static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
    static final int SSH_OPEN_CONNECT_FAILED = 2;
    static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE = 3;
    static final int SSH_OPEN_RESOURCE_SHORTAGE = 4;

    private static final Map<String, Class<? extends Channel>> IMPLS = new HashMap<>();

    static {
        IMPLS.put("session", ChannelSession.class);
        IMPLS.put("shell", ChannelShell.class);
        IMPLS.put("exec", ChannelExec.class);
        IMPLS.put("x11", ChannelX11.class);
//TODO//IMPLS.put("auth-agent@openssh.com", ChannelAgentForwarding.class);
        IMPLS.put("direct-tcpip", ChannelDirectTCPIP.class);
        IMPLS.put("forwarded-tcpip", ChannelForwardedTCPIP.class);
//TODO//IMPLS.put("sftp", ChannelSftp.class);
//TODO//IMPLS.put("subsystem", ChannelSubsystem.class);
        IMPLS.put("direct-streamlocal@openssh.com", ChannelDirectStreamLocal.class);
    }

    static int index = 0;
    private static final List<Channel> pool = new ArrayList<>();

    static Channel getChannel(final String type, final Session session) {
        final Class<? extends Channel> impl = IMPLS.get(type);
        if (impl == null)
            return null;
        final Channel r;
        try {
            r = impl.newInstance();
        } catch (final IllegalAccessException e) {
            throw new Error("Bad channel implementation", e);
        } catch (final InstantiationException e) {
            throw new Error("Bad channel implementation", e);
        }
        r.setSession(session);
        return r;
    }

    static Channel getChannel(final int id, final Session session) {
        synchronized (pool) {
            for (int i = 0; i < pool.size(); i++) {
                final Channel c = pool.get(i);
                if (c.id == id && c.session == session) return c;
            }
        }
        return null;
    }

    static void del(final Channel c) {
        synchronized (pool) {
            pool.remove(c);
        }
    }

    int id;
    volatile int recipient = -1;
    protected byte[] type = Util.str2byte("foo");
    volatile int lwsize_max = 0x100000;
    volatile int lwsize = lwsize_max;     // local initial window size
    volatile int lmpsize = 0x4000;     // local maximum packet size

    volatile long rwsize = 0;         // remote initial window size
    volatile int rmpsize = 0;        // remote maximum packet size

    Runnable onDisconnect = null;
    IO io = null;
    Thread thread = null;

    volatile boolean eof_local = false;
    volatile boolean eof_remote = false;

    volatile boolean close = false;
    volatile boolean connected = false;
    volatile boolean open_confirmation = false;

    public abstract static class ExitStatus {
    }

    public static final class NoExitStatus extends ExitStatus {
        NoExitStatus() {
        }
    }

    /**
     * Channel is closed due to {@code SSH_MSG_CHANNEL_CLOSE} without any exit status.
     */
    public static final NoExitStatus CLOSED_EXIT_STATUS = new NoExitStatus();

    /**
     * Channel is terminated but {@code SSH_MSG_CHANNEL_EOF} has already been received.
     */
    public static final NoExitStatus EOF_EXIT_STATUS = new NoExitStatus();

    /**
     * Channel closed due to process exit.
     */
    public static final class ProcessExitStatus extends ExitStatus {
        public final int value;

        ProcessExitStatus(final int value) {
            this.value = value;
        }
    }

    /**
     * Channel is closed due to process termination by a signal.
     */
    public static final class ProcessSignalExitStatus extends ExitStatus {
        public final String signalName;
        public final boolean coreDumped;
        public final String errorMessage;
        public final String languageTag;

        ProcessSignalExitStatus(final String signalName, final boolean coreDumped,
                                final String errorMessage, final String languageTag) {
            this.signalName = signalName;
            this.coreDumped = coreDumped;
            this.errorMessage = errorMessage;
            this.languageTag = languageTag;
        }
    }

    /**
     * Channel is closed due to {@code SSH_MSG_CHANNEL_OPEN_FAILURE}.
     * <p>
     * See <a href="https://www.rfc-editor.org/rfc/rfc4254.html">RFC4254</a>.
     */
    public static final class ConnectionOpenFailureExitStatus extends ExitStatus {
        public static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
        public static final int SSH_OPEN_CONNECT_FAILED = 2;
        public static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE = 3;
        public static final int SSH_OPEN_RESOURCE_SHORTAGE = 4;

        public final int reason;
        public final String description;
        public final String languageTag;

        ConnectionOpenFailureExitStatus(final int reason,
                                        final String description, final String languageTag) {
            this.reason = reason;
            this.description = description;
            this.languageTag = languageTag;
        }
    }

    volatile ExitStatus exitStatus = null;

    volatile int reply = 0;
    volatile int connectTimeout = 0;

    protected Session session;

    int notifyme = 0;

    Channel() {
        synchronized (pool) {
            id = index++;
            pool.add(this);
        }
    }

    synchronized void setRecipient(final int foo) {
        this.recipient = foo;
        if (notifyme > 0)
            notifyAll();
    }

    int getRecipient() {
        return recipient;
    }

    void init() throws JSchException {
    }

    public void connect() throws JSchException {
        connect(0);
    }

    public void connect(final int connectTimeout) throws JSchException {
        this.connectTimeout = connectTimeout;
        try {
            sendChannelOpen();
            start();
        } catch (final Exception e) {
            connected = false;
            disconnect();
            if (e instanceof JSchException)
                throw (JSchException) e;
            throw new JSchException(e.toString(), e);
        }
    }

    public void setXForwarding(final boolean foo) {
    }

    public void start() throws JSchException {
    }

    public boolean isEOF() {
        return eof_remote;
    }

    void getData(final Buffer buf) {
        setRecipient(buf.getInt());
        setRemoteWindowSize(buf.getUInt());
        setRemotePacketSize(buf.getInt());
    }

    /**
     * @param v to be executed on actual disconnect event
     *          when {@link #getExitStatus()} result is ready.
     *          The execution thread is the same as for
     *          {@link #setOutputStream(OutputStream out)}
     *          and
     *          {@link ChannelExec#setErrStream(OutputStream out)}
     *          argument methods.
     */
    public void setOnDisconnect(final Runnable v) {
        onDisconnect = v;
    }

    public void setInputStream(final InputStream in) {
        io.setInputStream(in, false);
    }

    public void setInputStream(final InputStream in, final boolean dontclose) {
        io.setInputStream(in, dontclose);
    }

    public void setOutputStream(final OutputStream out) {
        io.setOutputStream(out, false);
    }

    public void setOutputStream(final OutputStream out, final boolean dontclose) {
        io.setOutputStream(out, dontclose);
    }

    public void setExtOutputStream(final OutputStream out) {
        io.setExtOutputStream(out, false);
    }

    public void setExtOutputStream(final OutputStream out, final boolean dontclose) {
        io.setExtOutputStream(out, dontclose);
    }

    public InputStream getInputStream() throws IOException {
        int max_input_buffer_size = 32 * 1024;
        try {
            max_input_buffer_size =
                    Integer.parseInt(getSession().getConfig("max_input_buffer_size"));
        } catch (final Exception ignored) {
        }
        final PipedInputStream in =
                new MyPipedInputStream(
                        32 * 1024,  // this value should be customizable.
                        max_input_buffer_size
                );
        final boolean resizable = 32 * 1024 < max_input_buffer_size;
        io.setOutputStream(new PassiveOutputStream(in, resizable), false);
        return in;
    }

    public InputStream getExtInputStream() throws IOException {
        int max_input_buffer_size = 32 * 1024;
        try {
            max_input_buffer_size =
                    Integer.parseInt(getSession().getConfig("max_input_buffer_size"));
        } catch (final Exception ignored) {
        }
        final PipedInputStream in =
                new MyPipedInputStream(
                        32 * 1024,  // this value should be customizable.
                        max_input_buffer_size
                );
        final boolean resizable = 32 * 1024 < max_input_buffer_size;
        io.setExtOutputStream(new PassiveOutputStream(in, resizable), false);
        return in;
    }

    public OutputStream getOutputStream() throws IOException {

        final Channel channel = this;
        final OutputStream out = new OutputStream() {
            private int dataLen = 0;
            private Buffer buffer = null;
            private Packet packet = null;
            private boolean closed = false;

            private synchronized void init() throws IOException {
                buffer = new Buffer(rmpsize);
                packet = new Packet(buffer);

                final byte[] _buf = buffer.buffer;
                if (_buf.length - (14 + 0) - Session.buffer_margin <= 0) {
                    buffer = null;
                    packet = null;
                    throw new IOException("failed to initialize the channel.");
                }

            }

            final byte[] b = new byte[1];

            @Override
            public void write(final int w) throws IOException {
                b[0] = (byte) w;
                write(b, 0, 1);
            }

            @Override
            public void write(final byte[] buf, int s, int l) throws IOException {
                if (packet == null) {
                    init();
                }

                if (closed) {
                    throw new IOException("Already closed");
                }

                final byte[] _buf = buffer.buffer;
                final int _bufl = _buf.length;
                while (l > 0) {
                    final int _l = Math.min(l, _bufl - (14 + dataLen) - Session.buffer_margin);

                    if (_l <= 0) {
                        flush();
                        continue;
                    }

                    System.arraycopy(buf, s, _buf, 14 + dataLen, _l);
                    dataLen += _l;
                    s += _l;
                    l -= _l;
                }
            }

            @Override
            public void flush() throws IOException {
                if (closed) {
                    throw new IOException("Already closed");
                }
                if (dataLen == 0)
                    return;
                packet.reset();
                buffer.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
                buffer.putInt(recipient);
                buffer.putInt(dataLen);
                buffer.skip(dataLen);
                try {
                    final int foo = dataLen;
                    dataLen = 0;
                    synchronized (channel) {
                        if (!channel.close)
                            getSession().write(packet, channel, foo);
                    }
                } catch (final Exception e) {
                    close();
                    throw new IOException(e.toString(), e);
                }

            }

            @Override
            public void close() throws IOException {
                if (packet == null) {
                    try {
                        init();
                    } catch (final IOException e) {
                        // close should be finished silently.
                        return;
                    }
                }
                if (closed) {
                    return;
                }
                if (dataLen > 0) {
                    flush();
                }
                channel.eof();
                closed = true;
            }
        };
        return out;
    }

    static class MyPipedInputStream extends PipedInputStream {
        private int BUFFER_SIZE = 1024;
        private int max_buffer_size = BUFFER_SIZE;

        MyPipedInputStream() throws IOException {
            super();
        }

        MyPipedInputStream(final int size) throws IOException {
            super();
            buffer = new byte[size];
            BUFFER_SIZE = size;
            max_buffer_size = size;
        }

        MyPipedInputStream(final int size, final int max_buffer_size) throws IOException {
            this(size);
            this.max_buffer_size = max_buffer_size;
        }

        MyPipedInputStream(final PipedOutputStream out) throws IOException {
            super(out);
        }

        MyPipedInputStream(final PipedOutputStream out, final int size) throws IOException {
            super(out);
            buffer = new byte[size];
            BUFFER_SIZE = size;
        }

        /*
         * TODO: We should have our own Piped[I/O]Stream implementation.
         * Before accepting data, JDK's PipedInputStream will check the existence of
         * reader thread, and if it is not alive, the stream will be closed.
         * That behavior may cause the problem if multiple threads make access to it.
         */
        public synchronized void updateReadSide() throws IOException {
            if (available() != 0) { // not empty
                return;
            }
            in = 0;
            out = 0;
            buffer[in++] = 0;
            read();
        }

        private int freeSpace() {
            int size = 0;
            if (out < in) {
                size = buffer.length - in;
            } else if (in < out) {
                if (in == -1) size = buffer.length;
                else size = out - in;
            }
            return size;
        }

        synchronized void checkSpace(final int len) throws IOException {
            final int size = freeSpace();
            if (size < len) {
                final int datasize = buffer.length - size;
                int foo = buffer.length;
                while ((foo - datasize) < len) {
                    foo *= 2;
                }

                if (foo > max_buffer_size) {
                    foo = max_buffer_size;
                }
                if ((foo - datasize) < len) return;

                final byte[] tmp = new byte[foo];
                if (out < in) {
                    System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                } else if (in < out) {
                    if (in == -1) {
                    } else {
                        System.arraycopy(buffer, 0, tmp, 0, in);
                        System.arraycopy(buffer, out,
                                tmp, tmp.length - (buffer.length - out),
                                (buffer.length - out));
                        out = tmp.length - (buffer.length - out);
                    }
                } else {
                    System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                    in = buffer.length;
                }
                buffer = tmp;
            } else if (buffer.length == size && size > BUFFER_SIZE) {
                int i = size / 2;
                if (i < BUFFER_SIZE) i = BUFFER_SIZE;
                buffer = new byte[i];
            }
        }
    }

    void setLocalWindowSizeMax(final int foo) {
        this.lwsize_max = foo;
    }

    void setLocalWindowSize(final int foo) {
        this.lwsize = foo;
    }

    void setLocalPacketSize(final int foo) {
        this.lmpsize = foo;
    }

    synchronized void setRemoteWindowSize(final long foo) {
        this.rwsize = foo;
    }

    synchronized void addRemoteWindowSize(final long foo) {
        this.rwsize += foo;
        if (notifyme > 0)
            notifyAll();
    }

    void setRemotePacketSize(final int foo) {
        this.rmpsize = foo;
    }

    abstract void run();

    void write(final byte[] foo) throws IOException {
        write(foo, 0, foo.length);
    }

    void write(final byte[] foo, final int s, final int l) throws IOException {
        try {
            io.put(foo, s, l);
        } catch (final NullPointerException ignored) {
        }
    }

    void write_ext(final byte[] foo, final int s, final int l) throws IOException {
        try {
            io.put_ext(foo, s, l);
        } catch (final NullPointerException ignored) {
        }
    }

    void eof_remote() {
        eof_remote = true;
        try {
            io.out_close();
        } catch (final NullPointerException ignored) {
        }
    }

    void eof() {
        if (eof_local) return;
        eof_local = true;

        final int i = getRecipient();
        if (i == -1) return;

        try {
            final Buffer buf = new Buffer(100);
            final Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) Session.SSH_MSG_CHANNEL_EOF);
            buf.putInt(i);
            synchronized (this) {
                if (!close)
                    getSession().write(packet);
            }
        } catch (final Exception e) {
            //System.err.println("Channel.eof");
            //e.printStackTrace();
        }
    /*
    if(!isConnected()){ disconnect(); }
    */
    }

  /*
  http://www1.ietf.org/internet-drafts/draft-ietf-secsh-connect-24.txt

5.3  Closing a Channel
  When a party will no longer send more data to a channel, it SHOULD
   send SSH_MSG_CHANNEL_EOF.

            byte      SSH_MSG_CHANNEL_EOF
            uint32    recipient_channel

  No explicit response is sent to this message.  However, the
   application may send EOF to whatever is at the other end of the
  channel.  Note that the channel remains open after this message, and
   more data may still be sent in the other direction.  This message
   does not consume window space and can be sent even if no window space
   is available.

     When either party wishes to terminate the channel, it sends
     SSH_MSG_CHANNEL_CLOSE.  Upon receiving this message, a party MUST
   send back a SSH_MSG_CHANNEL_CLOSE unless it has already sent this
   message for the channel.  The channel is considered closed for a
     party when it has both sent and received SSH_MSG_CHANNEL_CLOSE, and
   the party may then reuse the channel number.  A party MAY send
   SSH_MSG_CHANNEL_CLOSE without having sent or received
   SSH_MSG_CHANNEL_EOF.

            byte      SSH_MSG_CHANNEL_CLOSE
            uint32    recipient_channel

   This message does not consume window space and can be sent even if no
   window space is available.

   It is recommended that any data sent before this message is delivered
     to the actual destination, if possible.
  */

    void close() {
        if (close) return;
        close = true;
        eof_local = eof_remote = true;

        final int i = getRecipient();
        if (i == -1) return;

        try {
            final Buffer buf = new Buffer(100);
            final Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) Session.SSH_MSG_CHANNEL_CLOSE);
            buf.putInt(i);
            synchronized (this) {
                getSession().write(packet);
            }
        } catch (final Exception e) {
            //e.printStackTrace();
        }
    }

    public boolean isClosed() {
        return close;
    }

    static void disconnect(final Session session) {
        final Channel[] channels;
        int count = 0;
        synchronized (pool) {
            channels = new Channel[pool.size()];
            for (final Channel c : pool) {
                try {
                    if (c.session == session) {
                        channels[count++] = c;
                    }
                } catch (final Exception ignored) {
                }
            }
        }
        for (int i = 0; i < count; i++) {
            channels[i].disconnect();
        }
    }

    public void disconnect() {
        //System.err.println(this+":disconnect "+io+" "+connected);
        //Thread.dumpStack();

        try {

            synchronized (this) {
                if (!connected) {
                    return;
                }
                connected = false;
            }

            close();

            eof_remote = eof_local = true;

            thread = null;

            try {
                if (io != null) {
                    io.close();
                }
            } catch (final Exception e) {
                //e.printStackTrace();
            }
            // io=null;
            try {
                if (onDisconnect != null) {
                    onDisconnect.run();
                }
            } catch (final Exception e) {
                //e.printStackTrace();
            }
        } finally {
            Channel.del(this);
        }
    }

    public boolean isConnected() {
        final Session _session = this.session;
        if (_session != null) {
            return _session.isConnected() && connected;
        }
        return false;
    }

    public void sendSignal(final String signal) throws Exception {
        final RequestSignal request = new RequestSignal();
        request.setSignal(signal);
        request.request(getSession(), this);
    }

//  public String toString(){
//      return "Channel: type="+new String(type)+",id="+id+",recipient="+recipient+",window_size="+window_size+",packet_size="+packet_size;
//  }

/*
  class OutputThread extends Thread{
    Channel c;
    OutputThread(Channel c){ this.c=c;}
    public void run(){c.output_thread();}
  }
*/

    static class PassiveInputStream extends MyPipedInputStream {
        PipedOutputStream os;

        PassiveInputStream(final PipedOutputStream out, final int size) throws IOException {
            super(out, size);
            this.os = out;
        }

        PassiveInputStream(final PipedOutputStream out) throws IOException {
            super(out);
            this.os = out;
        }

        @Override
        public void close() throws IOException {
            if (this.os != null) {
                this.os.close();
            }
            this.os = null;
        }
    }

    static class PassiveOutputStream extends PipedOutputStream {
        private MyPipedInputStream _sink = null;

        PassiveOutputStream(final PipedInputStream in,
                            final boolean resizable_buffer) throws IOException {
            super(in);
            if (resizable_buffer && (in instanceof MyPipedInputStream)) {
                this._sink = (MyPipedInputStream) in;
            }
        }

        @Override
        public void write(final int b) throws IOException {
            if (_sink != null) {
                _sink.checkSpace(1);
            }
            super.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (_sink != null) {
                _sink.checkSpace(len);
            }
            super.write(b, off, len);
        }
    }

    void setExitStatus(final ExitStatus status) {
        exitStatus = status;
    }

    public ExitStatus getExitStatus() {
        return exitStatus;
    }

    void setSession(final Session session) {
        this.session = session;
    }

    public Session getSession() throws JSchException {
        final Session _session = session;
        if (_session == null) {
            throw new JSchException("session is not available");
        }
        return _session;
    }

    public int getId() {
        return id;
    }

    protected void sendOpenConfirmation() throws Exception {
        final Buffer buf = new Buffer(200);
        final Packet packet = new Packet(buf);
        packet.reset();
        buf.putByte((byte) SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
        buf.putInt(getRecipient());
        buf.putInt(id);
        buf.putInt(lwsize);
        buf.putInt(lmpsize);
        getSession().write(packet);
    }

    protected void sendOpenFailure(final int reasonCode) {
        try {
            final Buffer buf = new Buffer(200);
            final Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) SSH_MSG_CHANNEL_OPEN_FAILURE);
            buf.putInt(getRecipient());
            buf.putInt(reasonCode);
            buf.putString(Util.str2byte("open failed"));
            buf.putString(Util.empty);
            getSession().write(packet);
        } catch (final Exception ignored) {
        }
    }

    protected Packet genChannelOpenPacket() {
        final Buffer buf = new Buffer(200);
        final Packet packet = new Packet(buf);
        // byte   SSH_MSG_CHANNEL_OPEN(90)
        // string channel type         //
        // uint32 sender channel       // 0
        // uint32 initial window size  // 0x100000(65536)
        // uint32 maxmum packet size   // 0x4000(16384)
        packet.reset();
        buf.putByte((byte) 90);
        buf.putString(this.type);
        buf.putInt(this.id);
        buf.putInt(this.lwsize);
        buf.putInt(this.lmpsize);
        return packet;
    }

    protected void sendChannelOpen() throws Exception {
        final Session _session = getSession();
        if (!_session.isConnected()) {
            throw new JSchException("session is down");
        }

        final Packet packet = genChannelOpenPacket();
        _session.write(packet);

        int retry = 2000;
        final long start = System.currentTimeMillis();
        final long timeout = connectTimeout;
        if (timeout != 0L)
            retry = 1;
        synchronized (this) {
            while (this.getRecipient() == -1 &&
                    _session.isConnected() &&
                    retry > 0) {
                if (timeout > 0L) {
                    if ((System.currentTimeMillis() - start) > timeout) {
                        retry = 0;
                        continue;
                    }
                }
                try {
                    final long t = timeout == 0L ? 10L : timeout;
                    this.notifyme = 1;
                    wait(t);
                } catch (final InterruptedException ignored) {
                } finally {
                    this.notifyme = 0;
                }
                retry--;
            }
        }
        if (!_session.isConnected()) {
            throw new JSchException("session is down");
        }
        if (this.getRecipient() == -1) {  // timeout
            throw new JSchException("channel opening timed out");
        }
        if (!this.open_confirmation) {  // SSH_MSG_CHANNEL_OPEN_FAILURE
            throw new JSchException("channel is not opened");
        }
        connected = true;
    }
}
