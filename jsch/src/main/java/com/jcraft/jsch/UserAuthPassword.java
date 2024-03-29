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

final class UserAuthPassword extends UserAuth {
    private static final int SSH_MSG_USERAUTH_PASSWD_CHANGEREQ = 60;

    @Override
    public boolean start(final Session session) throws Exception {
        super.start(session);

        final String dest = "ssh://" + username + "@" + session.host + ":" + session.port;

        byte[] password = session.password;
        byte[] newPassword = null;

        try {

            while (true) {

                if (session.auth_failures >= session.max_auth_tries) {
                    return false;
                }

                if (password == null) {
                    if (userinfo == null) {
                        //throw new JSchException("USERAUTH fail");
                        return false;
                    }
                    final CharSequence _password = userinfo.promptPassword(dest,
                            UserInfo.Message.PASSWORD_FOR_HOST,
                            dest);
                    if (_password == null) {
                        throw new JSchAuthCancelException("password");
                        //break;
                    }
                    password = Util.str2byte(_password);
                    userinfo.erase(_password);
                }

                final byte[] _username = Util.str2byte(username);

                // send
                // byte      SSH_MSG_USERAUTH_REQUEST(50)
                // string    user name
                // string    service name ("ssh-connection")
                // string    "password"
                // boolen    FALSE
                // string    plaintext password (ISO-10646 UTF-8)
                packet.reset();
                buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                buf.putString(_username);
                buf.putString(Util.str2byte("ssh-connection"));
                buf.putString(Util.str2byte("password"));
                buf.putByte((byte) 0);
                buf.putString(password);
                session.write(packet);

                loop:
                while (true) {
                    buf = session.read(buf);
                    final int command = buf.getCommand() & 0xff;

                    switch (command) {
                        case SSH_MSG_USERAUTH_SUCCESS: {
                            reportPasswordState(UserInfo.Result.SUCCESS,
                                    dest, newPassword != null ? newPassword : password);
                            return true;
                        }
                        case SSH_MSG_USERAUTH_BANNER: {
                            buf.getInt();
                            buf.getByte();
                            buf.getByte();
                            final byte[] message = buf.getString();
                            final byte[] lang = buf.getString();
                            if (userinfo != null) {
                                userinfo.showMessage(Util.byte2str(message));
                            }
                            continue loop;
                        }
                        case SSH_MSG_USERAUTH_PASSWD_CHANGEREQ: {
                            buf.getInt();
                            buf.getByte();
                            buf.getByte();
                            final byte[] instruction = buf.getString();
                            final byte[] language_tag = buf.getString();
                            if (userinfo == null) {
                                return false;
                            }

                            final CharSequence _newPassword = userinfo.promptPassword(dest,
                                    UserInfo.Message.PASSWORD_FOR_HOST_CHANGE,
                                    instruction, language_tag);
                            if (_newPassword == null) {
                                throw new JSchAuthCancelException("password");
                            }
                            Util.bzero(newPassword);
                            newPassword = Util.str2byte(_newPassword);
                            userinfo.erase(_newPassword);

                            // send
                            // byte      SSH_MSG_USERAUTH_REQUEST(50)
                            // string    user name
                            // string    service name ("ssh-connection")
                            // string    "password"
                            // boolen    TRUE
                            // string    plaintext old password (ISO-10646 UTF-8)
                            // string    plaintext new password (ISO-10646 UTF-8)
                            packet.reset();
                            buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                            buf.putString(_username);
                            buf.putString(Util.str2byte("ssh-connection"));
                            buf.putString(Util.str2byte("password"));
                            buf.putByte((byte) 1);
                            buf.putString(password);
                            buf.putString(newPassword);
                            session.write(packet);
                            continue loop;
                        }
                        case SSH_MSG_USERAUTH_FAILURE: {
                            buf.getInt();
                            buf.getByte();
                            buf.getByte();
                            final byte[] foo = buf.getString();
                            final int partial_success = buf.getByte();
                            //System.err.println(new String(foo)+
                            //                 " partial_success:"+(partial_success!=0));
                            if (partial_success != 0) {
                                if (newPassword != null) {
                                    reportPasswordState(UserInfo.Result.SUCCESS,
                                            dest, newPassword);
                                }
                                throw new JSchPartialAuthException(Util.byte2str(foo));
                            } else {
                                reportPasswordState(UserInfo.Result.FAILURE,
                                        dest, password);
                            }
                            session.auth_failures++;
                            break loop;
                        }
                        default: {
                            //System.err.println("USERAUTH fail ("+buf.getCommand()+")");
//          throw new JSchException("USERAUTH fail ("+buf.getCommand()+")");
                            return false;
                        }
                    }
                }

                Util.bzero(password);
                password = null;
                Util.bzero(newPassword);
                newPassword = null;

            }

        } finally {
            Util.bzero(password);
            Util.bzero(newPassword);
        }

        //throw new JSchException("USERAUTH fail");
        //return false;
    }
}
