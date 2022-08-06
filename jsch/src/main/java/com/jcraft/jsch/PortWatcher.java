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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class PortWatcher {
    private static final List<PortWatcher> pool = new ArrayList<>();
    private static InetAddress anyLocalAddress = null;

    static {
        // 0.0.0.0
/*
    try{ anyLocalAddress=InetAddress.getByAddress(new byte[4]); }
    catch(final UnknownHostException e){
    }
*/
        try {
            anyLocalAddress = InetAddress.getByName("0.0.0.0");
        } catch (final UnknownHostException ignored) {
        }
    }


    final Session session;
    int lport;
    final int rport;
    final String host;
    InetAddress boundaddress;
    private Runnable thread;
    ServerSocket ss;
    int connectTimeout = 0;
    private final String socketPath;

    PortWatcher(final Session session,
                final String address, final int lport,
                final String socketPath,
                final ServerSocketFactory ssf)
            throws JSchException {
        this.session = session;
        this.lport = lport;
        this.host = null;
        this.rport = 0;
        this.socketPath = socketPath;
        bindLocalPort(address, lport, ssf);
    }

    private void bindLocalPort(final String address, final int lport,
                               final ServerSocketFactory ssf)
            throws JSchException {
        try {
            boundaddress = InetAddress.getByName(address);
            ss = (ssf == null) ?
                    new ServerSocket(lport, 0, boundaddress) :
                    ssf.createServerSocket(lport, 0, boundaddress);
        } catch (final Exception e) {
            throw new JSchException("PortForwardingL: local port " + address + ":" + lport + " cannot be bound.", e);
        }
        if (lport == 0) {
            final int assigned = ss.getLocalPort();
            if (assigned != -1)
                this.lport = assigned;
        }
    }

    static String[] getPortForwarding(final Session session) {
        final List<String> r = new ArrayList<>();
        synchronized (pool) {
            for (final PortWatcher p : pool) {
                if (p.session == session) {
                    r.add(p.lport + ":" + p.host + ":" + p.rport);
                }
            }
        }
        return r.toArray(new String[0]);
    }

    static PortWatcher getPort(final Session session, final String address, final int lport)
            throws JSchException {
        final InetAddress addr;
        try {
            addr = InetAddress.getByName(address);
        } catch (final UnknownHostException uhe) {
            throw new JSchException("PortForwardingL: invalid address " + address + " specified.", uhe);
        }
        synchronized (pool) {
            for (final PortWatcher pw : pool) {
                if (pw.session == session && pw.lport == lport) {
                    if (/*pw.boundaddress.isAnyLocalAddress() ||*/
                            (pw.boundaddress.equals(anyLocalAddress)) ||
                                    pw.boundaddress.equals(addr))
                        return pw;
                }
            }
            return null;
        }
    }

    private static String normalize(final String address) {
        if (address != null) {
            if (address.isEmpty() || "*".equals(address))
                return "0.0.0.0";
            if ("localhost".equals(address))
                return "127.0.0.1";
        }
        return address;
    }

    static PortWatcher addPort(final Session session,
                               String address, final int lport,
                               final String host, final int rport,
                               final ServerSocketFactory ssf)
            throws JSchException {
        address = normalize(address);
        if (getPort(session, address, lport) != null) {
            throw new JSchException("PortForwardingL: local port " + address + ":" + lport + " is already registered.");
        }
        final PortWatcher pw = new PortWatcher(session, address, lport, host, rport, ssf);
        synchronized (pool) {
            pool.add(pw);
        }
        return pw;
    }

    static void delPort(final Session session, String address, final int lport)
            throws JSchException {
        address = normalize(address);
        final PortWatcher pw = getPort(session, address, lport);
        if (pw == null) {
            throw new JSchException("PortForwardingL: local port " + address + ":" + lport + " is not registered.");
        }
        synchronized (pool) {
            pool.remove(pw);
        }
        pw.delete();
    }

    static void delPort(final Session session) {
        synchronized (pool) {
            final Iterator<PortWatcher> it = pool.iterator();
            while (it.hasNext()) {
                final PortWatcher pw = it.next();
                if (pw.session == session) {
                    it.remove();
                    pw.delete();
                }
            }
        }
    }

    PortWatcher(final Session session,
                final String address, final int lport,
                final String host, final int rport,
                final ServerSocketFactory factory)
            throws JSchException {
        this.session = session;
        this.lport = lport;
        this.host = host;
        this.rport = rport;
        this.socketPath = null;
        bindLocalPort(address, lport, factory);
    }

    public static PortWatcher addSocket(final Session session,
                                        final String bindAddress, final int lport,
                                        final String socketPath,
                                        final ServerSocketFactory ssf)
            throws JSchException {
        final String address = normalize(bindAddress);
        if (getPort(session, address, lport) != null) {
            throw new JSchException("PortForwardingL: local port " + address + ":" + lport + " is already registered.");
        }
        final PortWatcher pw = new PortWatcher(session, address, lport, socketPath, ssf);
        pool.add(pw);
        return pw;
    }

    void run() {
        thread = this::run;
        try {
            while (thread != null) {
                final Socket socket = ss.accept();
                socket.setTcpNoDelay(true);
                final InputStream in = socket.getInputStream();
                final OutputStream out = socket.getOutputStream();
                if (socketPath != null && !socketPath.isEmpty()) {
                    final ChannelDirectStreamLocal channel = new ChannelDirectStreamLocal();
                    channel.setSession(session);
                    channel.init();
                    channel.setInputStream(in);
                    channel.setOutputStream(out);
                    session.addChannel(channel);
                    channel.setSocketPath(socketPath);
                    channel.setOrgIPAddress(socket.getInetAddress().getHostAddress());
                    channel.setOrgPort(socket.getPort());
                    channel.connect(connectTimeout);
                } else {
                    final ChannelDirectTCPIP channel = new ChannelDirectTCPIP();
                    channel.setSession(session);
                    channel.init();
                    channel.setInputStream(in);
                    channel.setOutputStream(out);
                    session.addChannel(channel);
                    channel.setHost(host);
                    channel.setPort(rport);
                    channel.setOrgIPAddress(socket.getInetAddress().getHostAddress());
                    channel.setOrgPort(socket.getPort());
                    channel.connect(connectTimeout);
                    //if (channel.exitstatus != null) {
                    //}
                }
            }
        } catch (final Exception e) {
            //System.err.println("! "+e);
        }
        delete();
    }

    void delete() {
        thread = null;
        try {
            if (ss != null) ss.close();
            ss = null;
        } catch (final Exception ignored) {
        }
    }

    void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
