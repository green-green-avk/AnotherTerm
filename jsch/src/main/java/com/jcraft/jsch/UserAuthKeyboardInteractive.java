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

final class UserAuthKeyboardInteractive extends UserAuth {
    @Override
    public boolean start(final Session session) throws Exception {
        super.start(session);

        if (userinfo != null && !(userinfo instanceof UIKeyboardInteractive)) {
            return false;
        }

        final String dest = "ssh://" + username + "@" + session.host + ":" + session.port;

        boolean cancel = false;

        final byte[] _username = Util.str2byte(username);

        while (true) {

            if (session.auth_failures >= session.max_auth_tries) {
                return false;
            }

            // send
            // byte      SSH_MSG_USERAUTH_REQUEST(50)
            // string    user name (ISO-10646 UTF-8, as defined in [RFC-2279])
            // string    service name (US-ASCII) "ssh-userauth" ? "ssh-connection"
            // string    "keyboard-interactive" (US-ASCII)
            // string    language tag (as defined in [RFC-3066])
            // string    submethods (ISO-10646 UTF-8)
            packet.reset();
            buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
            buf.putString(_username);
            buf.putString(Util.str2byte("ssh-connection"));
            //buf.putString("ssh-userauth".getBytes());
            buf.putString(Util.str2byte("keyboard-interactive"));
            buf.putString(Util.empty);
            buf.putString(Util.empty);
            session.write(packet);

            boolean firsttime = true;
            loop:
            while (true) {
                buf = session.read(buf);
                final int command = buf.getCommand() & 0xff;

                if (command == SSH_MSG_USERAUTH_SUCCESS) {
                    return true;
                }
                if (command == SSH_MSG_USERAUTH_BANNER) {
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
                if (command == SSH_MSG_USERAUTH_FAILURE) {
                    buf.getInt();
                    buf.getByte();
                    buf.getByte();
                    final byte[] foo = buf.getString();
                    final int partial_success = buf.getByte();
//          System.err.println(new String(foo)+
//                             " partial_success:"+(partial_success!=0));

                    if (partial_success != 0) {
                        throw new JSchPartialAuthException(Util.byte2str(foo));
                    }

                    if (firsttime) {
                        return false;
                        //throw new JSchException("USERAUTH KI is not supported");
                        //cancel=true;  // ??
                    }
                    session.auth_failures++;
                    break;
                }
                if (command == SSH_MSG_USERAUTH_INFO_REQUEST) {
                    firsttime = false;
                    buf.getInt();
                    buf.getByte();
                    buf.getByte();
                    final String name = Util.byte2str(buf.getString());
                    final String instruction = Util.byte2str(buf.getString());
                    final String language_tag = Util.byte2str(buf.getString());
                    final int num = buf.getInt();
                    final String[] prompt = new String[num];
                    final boolean[] echo = new boolean[num];
                    for (int i = 0; i < num; i++) {
                        prompt[i] = Util.byte2str(buf.getString());
                        echo[i] = (buf.getByte() != 0);
                    }

                    byte[][] response = null;

                    if (num > 0 || (!name.isEmpty() || !instruction.isEmpty())) {
                        if (userinfo != null) {
                            final UIKeyboardInteractive kbi = (UIKeyboardInteractive) userinfo;
                            final CharSequence[] _response = kbi.promptKeyboardInteractive(
                                    dest,
                                    name,
                                    instruction,
                                    prompt,
                                    echo
                            );
                            if (_response != null) {
                                response = new byte[_response.length][];
                                for (int i = 0; i < _response.length; i++) {
                                    response[i] = Util.str2byte(_response[i]);
                                }
                                kbi.erase(_response);
                            }
                        }
                    }

                    // byte      SSH_MSG_USERAUTH_INFO_RESPONSE(61)
                    // int       num-responses
                    // string    response[1] (ISO-10646 UTF-8)
                    // ...
                    // string    response[num-responses] (ISO-10646 UTF-8)
                    packet.reset();
                    buf.putByte((byte) SSH_MSG_USERAUTH_INFO_RESPONSE);
                    if (num > 0 &&
                            (response == null ||  // cancel
                                    num != response.length)) {

                        if (response == null) {
                            // working around the bug in OpenSSH ;-<
                            buf.putInt(num);
                            for (int i = 0; i < num; i++) {
                                buf.putString(Util.empty);
                            }
                        } else {
                            buf.putInt(0);
                        }

                        if (response == null)
                            cancel = true;
                    } else {
                        buf.putInt(num);
                        for (int i = 0; i < num; i++) {
                            buf.putString(response[i]);
                        }
                    }
                    session.write(packet);
          /*
          if(cancel)
            break;
          */
                    continue loop;
                }
                //throw new JSchException("USERAUTH fail ("+command+")");
                return false;
            }
            if (cancel) {
                throw new JSchAuthCancelException("keyboard-interactive");
                //break;
            }
        }
        //return false;
    }
}
