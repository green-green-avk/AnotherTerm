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

package com.jcraft.jsch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

final class ChannelAgentForwarding extends Channel {

    private static final int LOCAL_WINDOW_SIZE_MAX = 0x20000;
    private static final int LOCAL_MAXIMUM_PACKET_SIZE = 0x4000;

    private static final byte SSH_AGENTC_REQUEST_RSA_IDENTITIES = 1;
    private static final byte SSH_AGENT_RSA_IDENTITIES_ANSWER = 2;
    private static final byte SSH_AGENTC_RSA_CHALLENGE = 3;
    private static final byte SSH_AGENT_RSA_RESPONSE = 4;
    private static final byte SSH_AGENT_FAILURE = 5;
    private static final byte SSH_AGENT_SUCCESS = 6;
    private static final byte SSH_AGENTC_ADD_RSA_IDENTITY = 7;
    private static final byte SSH_AGENTC_REMOVE_RSA_IDENTITY = 8;
    private static final byte SSH_AGENTC_REMOVE_ALL_RSA_IDENTITIES = 9;

    private static final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
    private static final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
    private static final byte SSH2_AGENTC_SIGN_REQUEST = 13;
    private static final byte SSH2_AGENT_SIGN_RESPONSE = 14;
    private static final byte SSH2_AGENTC_ADD_IDENTITY = 17;
    private static final byte SSH2_AGENTC_REMOVE_IDENTITY = 18;
    private static final byte SSH2_AGENTC_REMOVE_ALL_IDENTITIES = 19;
    private static final byte SSH2_AGENT_FAILURE = 30;

    //private static final int SSH_AGENT_OLD_SIGNATURE=0x1;
    private static final int SSH_AGENT_RSA_SHA2_256 = 0x2;
    private static final int SSH_AGENT_RSA_SHA2_512 = 0x4;

    private final Buffer rbuf;
    private Buffer wbuf = null;
    private Packet packet = null;
    private final Buffer mbuf;

    ChannelAgentForwarding() {
        super();

        setLocalWindowSizeMax(LOCAL_WINDOW_SIZE_MAX);
        setLocalWindowSize(LOCAL_WINDOW_SIZE_MAX);
        setLocalPacketSize(LOCAL_MAXIMUM_PACKET_SIZE);

        type = Util.str2byte("auth-agent@openssh.com");
        rbuf = new Buffer();
        rbuf.reset();
        //wbuf=new Buffer(rmpsize);
        //packet=new Packet(wbuf);
        mbuf = new Buffer();
        connected = true;
    }

    @Override
    void run() {
        try {
            sendOpenConfirmation();
        } catch (final Exception e) {
            close = true;
            disconnect();
        }
    }

    @Override
    void write(final byte[] foo, final int s, final int l) throws IOException {

        if (packet == null) {
            wbuf = new Buffer(rmpsize);
            packet = new Packet(wbuf);
        }

        rbuf.shift();
        if (rbuf.buffer.length < rbuf.index + l) {
            final byte[] newbuf = new byte[rbuf.s + l];
            System.arraycopy(rbuf.buffer, 0, newbuf, 0, rbuf.buffer.length);
            rbuf.buffer = newbuf;
        }

        rbuf.putByte(foo, s, l);

        final int mlen = rbuf.getInt();
        if (mlen > rbuf.getLength()) {
            rbuf.s -= 4;
            return;
        }

        final int typ = rbuf.getByte();

        final Session _session;
        try {
            _session = getSession();
        } catch (final JSchException e) {
            throw new IOException(e.toString(), e);
        }

        final IdentityRepository irepo = _session.getIdentityRepository();
        final UserInfo userinfo = _session.getUserInfo();

        mbuf.reset();

        if (typ == SSH2_AGENTC_REQUEST_IDENTITIES) {
            mbuf.putByte(SSH2_AGENT_IDENTITIES_ANSWER);
            final List<Identity> identities = irepo.getIdentities();
            synchronized (identities) {
                int count = 0;
                for (final Identity identity : identities) {
                    if (identity.getPublicKeyBlob() != null)
                        count++;
                }
                mbuf.putInt(count);
                for (final Identity identity : identities) {
                    final byte[] pubkeyblob = identity.getPublicKeyBlob();
                    if (pubkeyblob == null)
                        continue;
                    mbuf.putString(pubkeyblob);
                    mbuf.putString(Util.empty);
                }
            }
        } else if (typ == SSH_AGENTC_REQUEST_RSA_IDENTITIES) {
            mbuf.putByte(SSH_AGENT_RSA_IDENTITIES_ANSWER);
            mbuf.putInt(0);
        } else if (typ == SSH2_AGENTC_SIGN_REQUEST) {
            final byte[] blob = rbuf.getString();
            final byte[] data = rbuf.getString();
            final int flags = rbuf.getInt();

//      if((flags & SSH_AGENT_OLD_SIGNATURE)!=0){ // old OpenSSH 2.0, 2.1
//        datafellows = SSH_BUG_SIGBLOB;
//      }

            final List<Identity> identities = irepo.getIdentities();
            Identity identity = null;
            synchronized (identities) {
                for (final Identity _identity : identities) {
                    if (_identity.getPublicKeyBlob() == null)
                        continue;
                    if (!Arrays.equals(blob, _identity.getPublicKeyBlob())) {
                        continue;
                    }
                    if (_identity.isEncrypted()) {
                        if (userinfo == null)
                            continue;
                        while (_identity.isEncrypted()) {
                            final CharSequence _passphrase =
                                    userinfo.promptPassword(null,
                                            UserInfo.Message.PASSPHRASE_FOR_KEY,
                                            _identity.getName());
                            if (_passphrase == null) {
                                break;
                            }
                            final byte[] passphrase = Util.str2byte(_passphrase);
                            userinfo.erase(_passphrase);
                            try {
                                if (_identity.setPassphrase(passphrase)) {
                                    break;
                                }
                            } catch (final JSchException e) {
                                break;
                            }
                        }
                    }

                    if (!_identity.isEncrypted()) {
                        identity = _identity;
                        break;
                    }
                }
            }

            byte[] signature = null;

            if (identity != null) {
                final Buffer kbuf = new Buffer(blob);
                final String keytype = Util.byte2str(kbuf.getString());
                if ("ssh-rsa".equals(keytype)) {
                    if ((flags & SSH_AGENT_RSA_SHA2_256) != 0) {
                        signature = identity.getSignature(data, "rsa-sha2-256");
                    } else if ((flags & SSH_AGENT_RSA_SHA2_512) != 0) {
                        signature = identity.getSignature(data, "rsa-sha2-512");
                    } else {
                        signature = identity.getSignature(data, "ssh-rsa");
                    }
                } else {
                    signature = identity.getSignature(data);
                }
            }

            if (signature == null) {
                mbuf.putByte(SSH2_AGENT_FAILURE);
            } else {
                mbuf.putByte(SSH2_AGENT_SIGN_RESPONSE);
                mbuf.putString(signature);
            }
        } else if (typ == SSH2_AGENTC_REMOVE_IDENTITY) {
            final byte[] blob = rbuf.getString();
            irepo.remove(blob);
            mbuf.putByte(SSH_AGENT_SUCCESS);
        } else if (typ == SSH_AGENTC_REMOVE_ALL_RSA_IDENTITIES) {
            mbuf.putByte(SSH_AGENT_SUCCESS);
        } else if (typ == SSH2_AGENTC_REMOVE_ALL_IDENTITIES) {
            irepo.removeAll();
            mbuf.putByte(SSH_AGENT_SUCCESS);
        } else if (typ == SSH2_AGENTC_ADD_IDENTITY) {
            final byte[] tmp = new byte[rbuf.getLength()];
            rbuf.getByte(tmp);
            final boolean result = irepo.add(tmp);
            mbuf.putByte(result ? SSH_AGENT_SUCCESS : SSH_AGENT_FAILURE);
        } else {
            rbuf.skip(rbuf.getLength() - 1);
            mbuf.putByte(SSH_AGENT_FAILURE);
        }

        final byte[] response = new byte[mbuf.getLength()];
        mbuf.getByte(response);
        send(response);
    }

    private void send(final byte[] message) {
        packet.reset();
        wbuf.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
        wbuf.putInt(recipient);
        wbuf.putInt(4 + message.length);
        wbuf.putString(message);

        try {
            getSession().write(packet, this, 4 + message.length);
        } catch (final Exception ignored) {
        }
    }

    @Override
    void eof_remote() {
        super.eof_remote();
        eof();
    }
}
