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

abstract class DHGN extends KeyExchange {
    private static final int SSH_MSG_KEXDH_INIT = 30;
    private static final int SSH_MSG_KEXDH_REPLY = 31;

    private int state;

    DH dh;

    byte[] V_S;
    byte[] V_C;
    byte[] I_S;
    byte[] I_C;

    byte[] e;

    private Buffer buf;

    abstract byte[] G();

    abstract byte[] P();

    abstract String sha_name();

    private void preInit(final Configuration cfg) throws JSchNotImplementedException {
        try {
            final Class<? extends HASH> c =
                    Class.forName(cfg.getConfig(sha_name()))
                            .asSubclass(HASH.class);
            sha = c.getDeclaredConstructor().newInstance();
            sha.init();
        } catch (final Exception | LinkageError e) {
            throw JSchNotImplementedException.forFeature(sha_name(), e);
        }

        try {
            final Class<? extends DH> c =
                    Class.forName(cfg.getConfig("dh"))
                            .asSubclass(DH.class);
            dh = c.getDeclaredConstructor().newInstance();
            dh.init();
        } catch (final Exception | LinkageError e) {
            throw JSchNotImplementedException.forFeature("dh", e);
        }
    }

    @Override
    public void check(final Configuration cfg) throws JSchException {
        preInit(cfg);
    }

    @Override
    public void init(final Session _session,
                     final byte[] V_S, final byte[] V_C, final byte[] I_S, final byte[] I_C)
            throws Exception {
        super.init(_session, V_S, V_C, I_S, I_C);
        preInit(session);
        this.V_S = V_S;
        this.V_C = V_C;
        this.I_S = I_S;
        this.I_C = I_C;

        buf = new Buffer();
        final Packet packet = new Packet(buf);

        dh.setP(P());
        dh.setG(G());
        // The client responds with:
        // byte  SSH_MSG_KEXDH_INIT(30)
        // mpint e <- g^x mod p
        //         x is a random number (1 < x < (p-1)/2)

        e = dh.getE();
        packet.reset();
        buf.putByte((byte) SSH_MSG_KEXDH_INIT);
        buf.putMPInt(e);
        session.write(packet);

        if (session.getLogger().isEnabled(Logger.DEBUG)) {
            session.getLogger().log(Logger.DEBUG,
                    "SSH_MSG_KEXDH_INIT sent");
            session.getLogger().log(Logger.DEBUG,
                    "expecting SSH_MSG_KEXDH_REPLY");
        }

        state = SSH_MSG_KEXDH_REPLY;
    }

    @Override
    public boolean next(final Buffer _buf) throws Exception {
        int i, j;

        switch (state) {
            case SSH_MSG_KEXDH_REPLY:
                // The server responds with:
                // byte      SSH_MSG_KEXDH_REPLY(31)
                // string    server public host key and certificates (K_S)
                // mpint     f
                // string    signature of H
                j = _buf.getInt();
                j = _buf.getByte();
                j = _buf.getByte();
                if (j != 31) {
                    session.getLogger().log(Logger.ERROR,
                            "type: must be 31 " + j);
                    return false;
                }

                K_S = _buf.getString();

                final byte[] f = _buf.getMPInt();
                final byte[] sig_of_H = _buf.getString();

                dh.setF(f);

                dh.checkRange();

                K = normalize(dh.getK());

                //The hash H is computed as the HASH hash of the concatenation of the
                //following:
                // string    V_C, the client's version string (CR and NL excluded)
                // string    V_S, the server's version string (CR and NL excluded)
                // string    I_C, the payload of the client's SSH_MSG_KEXINIT
                // string    I_S, the payload of the server's SSH_MSG_KEXINIT
                // string    K_S, the host key
                // mpint     e, exchange value sent by the client
                // mpint     f, exchange value sent by the server
                // mpint     K, the shared secret
                // This value is called the exchange hash, and it is used to authenti-
                // cate the key exchange.
                buf.reset();
                buf.putString(V_C);
                buf.putString(V_S);
                buf.putString(I_C);
                buf.putString(I_S);
                buf.putString(K_S);
                buf.putMPInt(e);
                buf.putMPInt(f);
                buf.putMPInt(K);
                final byte[] foo = new byte[buf.getLength()];
                buf.getByte(foo);
                sha.update(foo, 0, foo.length);
                H = sha.digest();
                //System.err.print("H -> "); //dump(H, 0, H.length);

                i = 0;
                j = 0;
                j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) |
                        ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                final String alg = Util.byte2str(K_S, i, j);
                i += j;

                final boolean result = verify(alg, K_S, i, sig_of_H);

                state = STATE_END;
                return result;
        }
        return false;
    }

    @Override
    public int getState() {
        return state;
    }
}
