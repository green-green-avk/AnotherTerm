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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class UserAuthPublicKey extends UserAuth {

    @Override
    public boolean start(final Session session) throws Exception {
        super.start(session);

        session.doOnPublicKeyAuth();
        final List<Identity> identities = session.getIdentityRepository().getIdentities();

        synchronized (identities) {
            if (identities.size() <= 0) {
                return false;
            }

            final String pkmethodstr = session.getConfig("PubkeyAcceptedAlgorithms");
            if (session.getLogger().isEnabled(Logger.DEBUG)) {
                session.getLogger().log(Logger.DEBUG,
                        "PubkeyAcceptedAlgorithms = " + pkmethodstr);
            }

            final List<String> pkmethods = Arrays.asList(Util.split(pkmethodstr, ","));
            if (pkmethods.isEmpty()) {
                return false;
            }

            final String[] server_sig_algs = session.getServerSigAlgs();
            if (server_sig_algs != null && server_sig_algs.length > 0) {
                final List<String> _known = new ArrayList<>();
                final List<String> _unknown = new ArrayList<>();
                for (final String pkmethod : pkmethods) {
                    boolean add = false;
                    for (final String server_sig_alg : server_sig_algs) {
                        if (pkmethod.equals(server_sig_alg)) {
                            add = true;
                            break;
                        }
                    }

                    if (add) {
                        _known.add(pkmethod);
                    } else {
                        _unknown.add(pkmethod);
                    }
                }

                if (!_known.isEmpty()) {
                    if (session.getLogger().isEnabled(Logger.DEBUG)) {
                        session.getLogger().log(Logger.DEBUG,
                                "PubkeyAcceptedAlgorithms in server-sig-algs = " + _known);
                    }
                }

                if (!_unknown.isEmpty()) {
                    if (session.getLogger().isEnabled(Logger.DEBUG)) {
                        session.getLogger().log(Logger.DEBUG,
                                "PubkeyAcceptedAlgorithms not in server-sig-algs = " + _unknown);
                    }
                }

                if (!_known.isEmpty() && !_unknown.isEmpty()) {
                    final boolean success = _start(session, identities, _known);
                    if (success) {
                        return true;
                    }

                    return _start(session, identities, _unknown);
                }
            } else {
                if (session.getLogger().isEnabled(Logger.DEBUG)) {
                    session.getLogger().log(Logger.DEBUG, "No server-sig-algs found, using PubkeyAcceptedAlgorithms = " + pkmethods);
                }
            }

            return _start(session, identities, pkmethods);
        }
    }

    private boolean _start(final Session session, final List<? extends Identity> identities,
                           final List<String> pkmethods)
            throws Exception {
        if (session.auth_failures >= session.max_auth_tries) {
            return false;
        }

        final Set<String> available_pks = session.getAvailableSignatures();

        final List<String> rsamethods = new ArrayList<>();
        final List<String> nonrsamethods = new ArrayList<>();
        for (final String pkmethod : pkmethods) {
            switch (pkmethod) {
                case "ssh-rsa":
                case "rsa-sha2-256":
                case "rsa-sha2-512":
                case "ssh-rsa-sha224@ssh.com":
                case "ssh-rsa-sha256@ssh.com":
                case "ssh-rsa-sha384@ssh.com":
                case "ssh-rsa-sha512@ssh.com":
                    rsamethods.add(pkmethod);
                    break;
                default:
                    nonrsamethods.add(pkmethod);
                    break;
            }
        }

        final byte[] _username = Util.str2byte(username);

        int command;

        iloop:
        for (final Identity identity : identities) {

            if (session.auth_failures >= session.max_auth_tries) {
                return false;
            }

            //System.err.println("UserAuthPublicKey: identity.isEncrypted()="+identity.isEncrypted());
            decryptKey(session, identity);
            //System.err.println("UserAuthPublicKey: identity.isEncrypted()="+identity.isEncrypted());

            final String _ipkmethod = identity.getAlgName();
            final List<String> ipkmethods;
            if ("ssh-rsa".equals(_ipkmethod)) {
                ipkmethods = rsamethods;
            } else if (nonrsamethods.contains(_ipkmethod)) {
                ipkmethods = Collections.singletonList(_ipkmethod);
            } else {
                ipkmethods = null;
            }
            if (ipkmethods == null) {
                if (session.getLogger().isEnabled(Logger.DEBUG)) {
                    session.getLogger().log(Logger.DEBUG,
                            _ipkmethod + " cannot be used as public key type for identity " + identity.getName());
                }
                continue;
            }

            byte[] pubkeyblob = identity.getPublicKeyBlob();
            List<String> pkmethodsuccesses = null;

            if (pubkeyblob != null) {
                command = SSH_MSG_USERAUTH_FAILURE;
                loop3:
                for (final String ipkmethod : ipkmethods) {
                    if (!available_pks.contains(ipkmethod) && !(identity instanceof AgentIdentity)) {
                        if (session.getLogger().isEnabled(Logger.DEBUG)) {
                            session.getLogger().log(Logger.DEBUG,
                                    ipkmethod + " not available for identity " + identity.getName());
                        }
                        continue loop3;
                    }

                    // send
                    // byte      SSH_MSG_USERAUTH_REQUEST(50)
                    // string    user name
                    // string    service name ("ssh-connection")
                    // string    "publickey"
                    // boolen    FALSE
                    // string    public key algorithm name
                    // string    public key blob
                    packet.reset();
                    buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                    buf.putString(_username);
                    buf.putString(Util.str2byte("ssh-connection"));
                    buf.putString(Util.str2byte("publickey"));
                    buf.putByte((byte) 0);
                    buf.putString(Util.str2byte(ipkmethod));
                    buf.putString(pubkeyblob);
                    session.write(packet);

                    loop1:
                    while (true) {
                        buf = session.read(buf);
                        command = buf.getCommand() & 0xff;

                        switch (command) {
                            case SSH_MSG_USERAUTH_PK_OK:
                                if (session.getLogger().isEnabled(Logger.DEBUG)) {
                                    session.getLogger().log(Logger.DEBUG,
                                            ipkmethod + " preauth success");
                                }
                                pkmethodsuccesses = Collections.singletonList(ipkmethod);
                                break loop3;
                            case SSH_MSG_USERAUTH_FAILURE:
                                if (session.getLogger().isEnabled(Logger.DEBUG)) {
                                    session.getLogger().log(Logger.DEBUG,
                                            ipkmethod + " preauth failure");
                                }
                                continue loop3;
                            case SSH_MSG_USERAUTH_BANNER: {
                                buf.getInt();
                                buf.getByte();
                                buf.getByte();
                                final byte[] _message = buf.getString();
                                final byte[] lang = buf.getString();
                                final String message = Util.byte2str(_message);
                                if (userinfo != null) {
                                    userinfo.showMessage(message);
                                }
                                continue loop1;
                            }
                        }
                        //System.err.println("USERAUTH fail ("+command+")");
                        //throw new JSchException("USERAUTH fail ("+command+")");
                        if (session.getLogger().isEnabled(Logger.DEBUG)) {
                            session.getLogger().log(Logger.DEBUG,
                                    ipkmethod + " preauth failure command (" + command + ")");
                        }
                        continue loop3;
                    }
                }

                if (command != SSH_MSG_USERAUTH_PK_OK) {
                    continue iloop;
                }
            }


            if (identity.isEncrypted()) continue;
            if (pubkeyblob == null) pubkeyblob = identity.getPublicKeyBlob();

            //System.err.println("UserAuthPublicKey: pubkeyblob="+pubkeyblob);

            if (pubkeyblob == null) continue;
            if (pkmethodsuccesses == null) pkmethodsuccesses = ipkmethods;

            loop4:
            for (final String pkmethodsuccess : pkmethodsuccesses) {
                if (!available_pks.contains(pkmethodsuccess) && !(identity instanceof AgentIdentity)) {
                    if (session.getLogger().isEnabled(Logger.DEBUG)) {
                        session.getLogger().log(Logger.DEBUG,
                                pkmethodsuccess + " not available for identity " + identity.getName());
                    }
                    continue loop4;
                }

                // send
                // byte      SSH_MSG_USERAUTH_REQUEST(50)
                // string    user name
                // string    service name ("ssh-connection")
                // string    "publickey"
                // boolen    TRUE
                // string    public key algorithm name
                // string    public key blob
                // string    signature
                packet.reset();
                buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                buf.putString(_username);
                buf.putString(Util.str2byte("ssh-connection"));
                buf.putString(Util.str2byte("publickey"));
                buf.putByte((byte) 1);
                buf.putString(Util.str2byte(pkmethodsuccess));
                buf.putString(pubkeyblob);

                //        byte[] tmp=new byte[buf.index-5];
                //        System.arraycopy(buf.buffer, 5, tmp, 0, tmp.length);
                //        buf.putString(signature);

                final byte[] sid = session.getSessionId();
                final int sidlen = sid.length;
                final byte[] tmp = new byte[4 + sidlen + buf.index - 5];
                tmp[0] = (byte) (sidlen >>> 24);
                tmp[1] = (byte) (sidlen >>> 16);
                tmp[2] = (byte) (sidlen >>> 8);
                tmp[3] = (byte) (sidlen);
                System.arraycopy(sid, 0, tmp, 4, sidlen);
                System.arraycopy(buf.buffer, 5, tmp, 4 + sidlen, buf.index - 5);
                final byte[] signature = identity.getSignature(tmp, pkmethodsuccess);
                if (signature == null) {  // for example, too long key length.
                    if (session.getLogger().isEnabled(Logger.DEBUG)) {
                        session.getLogger().log(Logger.DEBUG,
                                pkmethodsuccess + " signature failure");
                    }
                    continue loop4;
                }
                buf.putString(signature);
                session.write(packet);

                loop2:
                while (true) {
                    buf = session.read(buf);
                    command = buf.getCommand() & 0xff;

                    switch (command) {
                        case SSH_MSG_USERAUTH_SUCCESS:
                            if (session.getLogger().isEnabled(Logger.DEBUG)) {
                                session.getLogger().log(Logger.DEBUG,
                                        pkmethodsuccess + " auth success");
                            }
                            return true;
                        case SSH_MSG_USERAUTH_BANNER: {
                            buf.getInt();
                            buf.getByte();
                            buf.getByte();
                            final byte[] _message = buf.getString();
                            final byte[] lang = buf.getString();
                            final String message = Util.byte2str(_message);
                            if (userinfo != null) {
                                userinfo.showMessage(message);
                            }
                            continue loop2;
                        }
                        case SSH_MSG_USERAUTH_FAILURE: {
                            buf.getInt();
                            buf.getByte();
                            buf.getByte();
                            final byte[] foo = buf.getString();
                            final int partial_success = buf.getByte();
                            //System.err.println(new String(foo)+
                            //                   " partial_success:"+(partial_success!=0));
                            if (partial_success != 0) {
                                throw new JSchPartialAuthException(Util.byte2str(foo));
                            }
                            session.auth_failures++;
                            if (session.getLogger().isEnabled(Logger.DEBUG)) {
                                session.getLogger().log(Logger.DEBUG,
                                        pkmethodsuccess + " auth failure");
                            }
                            break loop2;
                        }
                    }
                    //System.err.println("USERAUTH fail ("+command+")");
                    //throw new JSchException("USERAUTH fail ("+command+")");
                    if (session.getLogger().isEnabled(Logger.DEBUG)) {
                        session.getLogger().log(Logger.DEBUG,
                                pkmethodsuccess + " auth failure command (" + command + ")");
                    }
                    break loop2;
                }
            }
        }
        return false;
    }

    private void decryptKey(final Session session, final Identity identity) throws JSchException {
        byte[] passphrase = null;
        int count = 5;
        while (true) {
            if (identity.isEncrypted() && passphrase == null) {
                if (userinfo == null) {
                    throw new JSchException("USERAUTH fail");
                }
                final CharSequence _passphrase =
                        userinfo.promptPassword(identity.getName(),
                                UserInfo.Message.PASSPHRASE_FOR_KEY,
                                identity.getName());
                if (_passphrase == null) {
                    throw new JSchAuthCancelException("publickey");
                    //throw new JSchException("USERAUTH cancel");
                    //break;
                }
                passphrase = Util.str2byte(_passphrase);
                userinfo.erase(_passphrase);
            }

            if (!identity.isEncrypted() || passphrase != null) {
                if (identity.setPassphrase(passphrase)) {
                    if (passphrase != null) {
                        if (session.getIdentityRepository() instanceof IdentityRepositoryWrapper) {
                            ((IdentityRepositoryWrapper) session.getIdentityRepository()).check();
                        }
                        reportPasswordState(UserInfo.Result.SUCCESS,
                                identity.getName(), passphrase);
                    }
                    break;
                } else {
                    if (passphrase != null) {
                        reportPasswordState(UserInfo.Result.FAILURE,
                                identity.getName(), passphrase);
                    }
                }
            }
            Util.bzero(passphrase);
            passphrase = null;
            count--;
            if (count == 0) {
                throw new JSchAuthKeyDecryptionException("Too many retries");
            }
        }

        Util.bzero(passphrase);
    }
}
