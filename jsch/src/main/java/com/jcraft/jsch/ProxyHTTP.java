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
import java.net.Socket;

public class ProxyHTTP implements Proxy {
    private static final int DEFAULT_PORT = 80;
    private final String proxy_host;
    private final int proxy_port;
    private InputStream in;
    private OutputStream out;
    private Socket socket;

    private String user;
    private String passwd;

    public ProxyHTTP(final String proxy_host) {
        int port = DEFAULT_PORT;
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

    public ProxyHTTP(final String proxy_host, final int proxy_port) {
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

            out.write(Util.str2byte("CONNECT " + host + ":" + port + " HTTP/1.0\r\n"));

            if (user != null && passwd != null) {
                byte[] code = Util.str2byte(user + ":" + passwd);
                code = Util.toBase64(code, 0, code.length, true);
                out.write(Util.str2byte("Proxy-Authorization: Basic "));
                out.write(code);
                out.write(Util.str2byte("\r\n"));
            }

            out.write(Util.str2byte("\r\n"));
            out.flush();

            int foo = 0;

            final StringBuilder sb = new StringBuilder();
            while (foo >= 0) {
                foo = in.read();
                if (foo != 13) {
                    sb.append((char) foo);
                    continue;
                }
                foo = in.read();
                if (foo != 10) {
                    continue;
                }
                break;
            }
            if (foo < 0) {
                throw new IOException();
            }

            final String response = sb.toString();
            String reason = "Unknow reason";
            int code = -1;
            try {
                foo = response.indexOf(' ');
                final int bar = response.indexOf(' ', foo + 1);
                code = Integer.parseInt(response.substring(foo + 1, bar));
                reason = response.substring(bar + 1);
            } catch (final Exception ignored) {
            }
            if (code != 200) {
                throw new IOException("proxy error: " + reason);
            }

      /*
      while(foo>=0){
        foo=in.read(); if(foo!=13) continue;
        foo=in.read(); if(foo!=10) continue;
        foo=in.read(); if(foo!=13) continue;      
        foo=in.read(); if(foo!=10) continue;
        break;
      }
      */

            while (true) {
                int count = 0;
                while (foo >= 0) {
                    foo = in.read();
                    if (foo != 13) {
                        count++;
                        continue;
                    }
                    foo = in.read();
                    if (foo != 10) {
                        continue;
                    }
                    break;
                }
                if (foo < 0) {
                    throw new IOException();
                }
                if (count == 0) break;
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            try {
                if (socket != null)
                    socket.close();
            } catch (final Exception ignored) {
            }
            throw new JSchException("ProxyHTTP: " + e, e);
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
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null) socket.close();

        } catch (final Exception ignored) {
        }
        in = null;
        out = null;
        socket = null;
    }

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }
}
