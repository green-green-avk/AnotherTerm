/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2006-2018 ymnk, JCraft,Inc. All rights reserved.

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

/*
 This file depends on following documents,
   - SOCKS: A protocol for TCP proxy across firewalls, Ying-Da Lee
     http://www.socks.nec.com/protocol/socks4.protocol
 */

package com.jcraft.jsch;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ProxySOCKS4 implements Proxy {
    private static final int DEFAULTPORT = 1080;
    private final String proxy_host;
    private final int proxy_port;
    private InputStream in;
    private OutputStream out;
    private Socket socket;

    private String user;
    private String passwd;

    public ProxySOCKS4(final String proxy_host) {
        int port = DEFAULTPORT;
        String host = proxy_host;
        final String[] proxyHostPort = proxy_host.split(":", 2);
        if (proxyHostPort.length == 2) {
            try {
                host = proxyHostPort[0];
                port = Integer.parseInt(proxyHostPort[1]);
            } catch (final NumberFormatException ignored) {
            }
        }
        this.proxy_host = host;
        this.proxy_port = port;
    }

    public ProxySOCKS4(final String proxy_host, final int proxy_port) {
        this.proxy_host = proxy_host;
        this.proxy_port = proxy_port;
    }

    public void setUserPasswd(final String user, final String passwd) {
        this.user = user;
        this.passwd = passwd;
    }

    @Override
    public void connect(final SocketFactory socket_factory, final String host, final int port,
                        final int timeout) throws JSchException {
        try {
            if (socket_factory == null) {
                socket = Util.createSocket(proxy_host, proxy_port, timeout);
                //socket=new Socket(proxy_host, proxy_port);
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } else {
                socket = socket_factory.createSocket(proxy_host, proxy_port);
                in = socket_factory.getInputStream(socket);
                out = socket_factory.getOutputStream(socket);
            }
            if (timeout > 0) {
                socket.setSoTimeout(timeout);
            }
            socket.setTcpNoDelay(true);

            final byte[] buf = new byte[1024];
            int index;

/*
   1) CONNECT
   
   The client connects to the SOCKS server and sends a CONNECT request when
   it wants to establish a connection to an application server. The client
   includes in the request packet the IP address and the port number of the
   destination host, and userid, in the following format.
   
               +----+----+----+----+----+----+----+----+----+----+....+----+
               | VN | CD | DSTPORT |      DSTIP        | USERID       |NULL|
               +----+----+----+----+----+----+----+----+----+----+....+----+
   # of bytes:   1    1      2              4           variable       1
   
   VN is the SOCKS protocol version number and should be 4. CD is the
   SOCKS command code and should be 1 for CONNECT request. NULL is a byte
   of all zero bits.
*/

            index = 0;
            buf[index++] = 4;
            buf[index++] = 1;

            buf[index++] = (byte) (port >>> 8);
            buf[index++] = (byte) (port & 0xff);

            try {
                final InetAddress addr = InetAddress.getByName(host);
                final byte[] byteAddress = addr.getAddress();
                for (final byte address : byteAddress) {
                    buf[index++] = address;
                }
            } catch (final UnknownHostException uhe) {
                throw new JSchException("ProxySOCKS4: " + uhe, uhe);
            }

            if (user != null) {
                System.arraycopy(Util.str2byte(user), 0, buf, index, user.length());
                index += user.length();
            }
            buf[index++] = 0;
            out.write(buf, 0, index);

/*
   The SOCKS server checks to see whether such a request should be granted
   based on any combination of source IP address, destination IP address,
   destination port number, the userid, and information it may obtain by
   consulting IDENT, cf. RFC 1413.  If the request is granted, the SOCKS
   server makes a connection to the specified port of the destination host.
   A reply packet is sent to the client when this connection is established,
   or when the request is rejected or the operation fails. 
   
               +----+----+----+----+----+----+----+----+
               | VN | CD | DSTPORT |      DSTIP        |
               +----+----+----+----+----+----+----+----+
   # of bytes:   1    1      2              4
   
   VN is the version of the reply code and should be 0. CD is the result
   code with one of the following values:
   
   90: request granted
   91: request rejected or failed
   92: request rejected becasue SOCKS server cannot connect to
       identd on the client
   93: request rejected because the client program and identd
       report different user-ids
   
   The remaining fields are ignored.
*/

            final int len = 8;
            int s = 0;
            while (s < len) {
                final int i = in.read(buf, s, len - s);
                if (i <= 0) {
                    throw new JSchException("ProxySOCKS4: stream is closed");
                }
                s += i;
            }
            if (buf[0] != 0) {
                throw new JSchException("ProxySOCKS4: server returns VN " + buf[0]);
            }
            if (buf[1] != 90) {
                try {
                    socket.close();
                } catch (final Exception ignored) {
                }
                final String message = "ProxySOCKS4: server returns CD " + buf[1];
                throw new JSchException(message);
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            try {
                if (socket != null) socket.close();
            } catch (final Exception ignored) {
            }
            throw new JSchException("ProxySOCKS4: " + e, e);
        }
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (final Exception ignored) {
        }
        in = null;
        out = null;
        socket = null;
    }

    public static int getDefaultPort() {
        return DEFAULTPORT;
    }
}
