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

import java.io.PipedOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public final class ChannelForwardedTCPIP extends Channel {

    private static final List<Config> pool = new ArrayList<>();

    private static final int LOCAL_WINDOW_SIZE_MAX = 0x20000;
    //static private final int LOCAL_WINDOW_SIZE_MAX=0x100000;
    private static final int LOCAL_MAXIMUM_PACKET_SIZE = 0x4000;

    private static final int TIMEOUT = 10 * 1000;

    private Socket socket = null;
    private ForwardedTCPIPDaemon daemon = null;
    private Config config = null;

    ChannelForwardedTCPIP() {
        super();
        setLocalWindowSizeMax(LOCAL_WINDOW_SIZE_MAX);
        setLocalWindowSize(LOCAL_WINDOW_SIZE_MAX);
        setLocalPacketSize(LOCAL_MAXIMUM_PACKET_SIZE);
        io = new IO();
        connected = true;
    }

    @Override
    public void run() {
        try {
            if (config instanceof ConfigDaemon) {
                final ConfigDaemon _config = (ConfigDaemon) config;
                final Class<? extends ForwardedTCPIPDaemon> c =
                        Class.forName(_config.target)
                                .asSubclass(ForwardedTCPIPDaemon.class);
                daemon = c.getDeclaredConstructor().newInstance();

                final PipedOutputStream out = new PipedOutputStream();
                io.setInputStream(new PassiveInputStream(out, 32 * 1024),
                        false);

                daemon.setChannel(this, getInputStream(), out);
                daemon.setArg(_config.arg);
                new Thread(daemon).start();
            } else {
                final ConfigLHost _config = (ConfigLHost) config;
                socket = (_config.factory == null) ?
                        Util.createSocket(_config.target, _config.lport, TIMEOUT) :
                        _config.factory.createSocket(_config.target, _config.lport);
                socket.setTcpNoDelay(true);
                io.setInputStream(socket.getInputStream());
                io.setOutputStream(socket.getOutputStream());
            }
            sendOpenConfirmation();
        } catch (final Exception e) {
            sendOpenFailure(SSH_OPEN_ADMINISTRATIVELY_PROHIBITED);
            close = true;
            disconnect();
            return;
        }

        thread = Thread.currentThread();
        final Buffer buf = new Buffer(rmpsize);
        final Packet packet = new Packet(buf);
        int i = 0;
        try {
            final Session _session = getSession();
            while (thread != null &&
                    io != null &&
                    io.in != null) {
                i = io.in.read(buf.buffer,
                        14,
                        buf.buffer.length - 14
                                - Session.buffer_margin
                );
                if (i <= 0) {
                    eof();
                    break;
                }
                packet.reset();
                buf.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
                buf.putInt(recipient);
                buf.putInt(i);
                buf.skip(i);
                synchronized (this) {
                    if (close)
                        break;
                    _session.write(packet, this, i);
                }
            }
        } catch (final Exception e) {
            //System.err.println(e);
        }
        //thread=null;
        //eof();
        disconnect();
    }

    @Override
    void getData(final Buffer buf) {
        setRecipient(buf.getInt());
        setRemoteWindowSize(buf.getUInt());
        setRemotePacketSize(buf.getInt());
        final byte[] addr = buf.getString();
        final int port = buf.getInt();
        final byte[] orgaddr = buf.getString();
        final int orgport = buf.getInt();

    /*
    System.err.println("addr: "+Util.byte2str(addr));
    System.err.println("port: "+port);
    System.err.println("orgaddr: "+Util.byte2str(orgaddr));
    System.err.println("orgport: "+orgport);
    */

        final Session _session;
        try {
            _session = getSession();
        } catch (final JSchException e) {
            // session has been already down.
            this.config = null;
            if (JSch.getLogger().isEnabled(Logger.ERROR)) {
                JSch.getLogger().log(Logger.ERROR,
                        "ChannelForwardedTCPIP: " + Util.byte2str(addr) + ":" + port + " is not registered.");
            }
            return;
        }

        this.config = getPort(_session, Util.byte2str(addr), port);
        if (this.config == null)
            this.config = getPort(_session, null, port);

        if (this.config == null) {
            if (_session.getLogger().isEnabled(Logger.ERROR)) {
                _session.getLogger().log(Logger.ERROR,
                        "ChannelForwardedTCPIP: " + Util.byte2str(addr) + ":" + port + " is not registered.");
            }
        }
    }

    private static Config getPort(final Session session,
                                  final String address_to_bind, final int rport) {
        synchronized (pool) {
            for (final Config config : pool) {
                if (config.session != session) continue;
                if (config.rport != rport) {
                    if (config.rport != 0 || config.allocated_rport != rport)
                        continue;
                }
                if (address_to_bind != null &&
                        !config.address_to_bind.equals(address_to_bind)) continue;
                return config;
            }
            return null;
        }
    }

    static String[] getPortForwarding(final Session session) {
        final List<String> r = new ArrayList<>();
        synchronized (pool) {
            for (final Config config : pool) {
                if (config.session == session) {
                    if (config instanceof ConfigDaemon)
                        r.add(config.allocated_rport + ":" + config.target + ":");
                    else
                        r.add(config.allocated_rport + ":" + config.target + ":" + ((ConfigLHost) config).lport);
                }
            }
        }
        return r.toArray(new String[0]);
    }

    static String normalize(final String address) {
        if (address == null)
            return "localhost";
        if (address.isEmpty() || "*".equals(address))
            return "";
        return address;
    }

    static void addPort(final Session session,
                        final String _address_to_bind, final int port, final int allocated_port,
                        final String target, final int lport,
                        final SocketFactory factory)
            throws JSchException {
        final String address_to_bind = normalize(_address_to_bind);
        synchronized (pool) {
            if (getPort(session, address_to_bind, port) != null) {
                throw new JSchException("PortForwardingR: remote port " + port + " is already registered.");
            }
            final ConfigLHost config = new ConfigLHost();
            config.session = session;
            config.rport = port;
            config.allocated_rport = allocated_port;
            config.target = target;
            config.lport = lport;
            config.address_to_bind = address_to_bind;
            config.factory = factory;
            pool.add(config);
        }
    }

    static void addPort(final Session session,
                        final String _address_to_bind, final int port, final int allocated_port,
                        final String daemon, final Object[] arg)
            throws JSchException {
        final String address_to_bind = normalize(_address_to_bind);
        synchronized (pool) {
            if (getPort(session, address_to_bind, port) != null) {
                throw new JSchException("PortForwardingR: remote port " + port + " is already registered.");
            }
            final ConfigDaemon config = new ConfigDaemon();
            config.session = session;
            config.rport = port;
            config.allocated_rport = port;
            config.target = daemon;
            config.arg = arg;
            config.address_to_bind = address_to_bind;
            pool.add(config);
        }
    }

    static void delPort(final ChannelForwardedTCPIP c) {
        final Session _session;
        try {
            _session = c.getSession();
        } catch (final JSchException e) {
            // session has been already down.
            return;
        }
        if (c.config != null)
            delPort(_session, c.config.rport);
    }

    static void delPort(final Session session, final int rport) {
        delPort(session, null, rport);
    }

    static void delPort(final Session session, String address_to_bind, final int rport) {
        synchronized (pool) {
            Config config = getPort(session, normalize(address_to_bind), rport);
            if (config == null)
                config = getPort(session, null, rport);
            if (config == null) return;
            pool.remove(config);
            if (address_to_bind == null) {
                address_to_bind = config.address_to_bind;
            }
            if (address_to_bind == null) {
                address_to_bind = "0.0.0.0";
            }
        }

        final Buffer buf = new Buffer(200); // ??
        final Packet packet = new Packet(buf);

        try {
            // byte SSH_MSG_GLOBAL_REQUEST 80
            // string "cancel-tcpip-forward"
            // boolean want_reply
            // string  address_to_bind (e.g. "127.0.0.1")
            // uint32  port number to bind
            packet.reset();
            buf.putByte((byte) 80/*SSH_MSG_GLOBAL_REQUEST*/);
            buf.putString(Util.str2byte("cancel-tcpip-forward"));
            buf.putByte((byte) 0);
            buf.putString(Util.str2byte(address_to_bind));
            buf.putInt(rport);
            session.write(packet);
        } catch (final Exception e) {
//    throw new JSchException(e.toString(), e);
        }
    }

    static void delPort(final Session session) {
        final int[] rport;
        int count = 0;
        synchronized (pool) {
            rport = new int[pool.size()];
            for (final Config config : pool) {
                if (config.session == session) {
                    rport[count++] = config.rport; // ((Integer)bar[1]).intValue();
                }
            }
        }
        for (int i = 0; i < count; i++) {
            delPort(session, rport[i]);
        }
    }

    public int getRemotePort() {
        return (config != null ? config.rport : 0);
    }

    private void setSocketFactory(final SocketFactory factory) {
        if (config != null && (config instanceof ConfigLHost))
            ((ConfigLHost) config).factory = factory;
    }

    abstract static class Config {
        Session session;
        int rport;
        int allocated_rport;
        String address_to_bind;
        String target;
    }

    static class ConfigDaemon extends Config {
        Object[] arg;
    }

    static class ConfigLHost extends Config {
        int lport;
        SocketFactory factory;
    }
}
