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

import java.nio.CharBuffer;

public interface UserInfo {
    interface Message {
        /**
         * Args: CharSequence message, String languageTag
         */
        int SIMPLE_MESSAGE = 0;
        /**
         * Args: String remoteHost, String hostKeyType, String hostKeyFingerprint
         */
        int REMOTE_IDENTITY_NEW_ASK_PROCEED = 1;
        /**
         * Args: String remoteHost, String hostKeyType, String hostKeyFingerprint
         */
        int REMOTE_IDENTITY_CHANGED = 2;
        /**
         * Args: String remoteHost, String hostKeyType, String hostKeyFingerprint
         */
        int REMOTE_IDENTITY_CHANGED_ASK_PROCEED = 3;
        /**
         * Args: String userHostPort
         */
        int PASSWORD_FOR_HOST = 0x11;
        /**
         * Args: String someKeyName
         */
        int PASSPHRASE_FOR_KEY = 0x12;
        /**
         * Args: String userHostPort, String prompt, String languageTag
         */
        int PASSWORD_FOR_HOST_CHANGE = 0x21;
        /**
         * Args: String remoteHost, String hostKeyType
         */
        int REMOTE_IDENTITY_KEY_REVOKED = 0x31;
        /**
         * User messages start from here up
         */
        int USER_BASE = 0x10000;
    }

    /**
     * Erases sensitive data returned by {@code prompt*()} calls.
     *
     * @param v data to erase
     */
    void erase(CharSequence v);

    interface SensitiveStringProvider {
        /**
         * @return a copy of the actual string that must be erased after use
         */
        CharBuffer get();
    }

    interface Result {
        int SUCCESS = 0;
        int FAILURE = 1;
    }

    /**
     * Provides a way to manage the password returned by {@code prompt*()} calls for future use.
     *
     * @param result {@link Result}
     * @param id     unique id for keyring
     * @param v      password to save
     *               (this {@link SensitiveStringProvider}
     *               will no longer be valid after the function returns:
     *               use {@link SensitiveStringProvider#get()} to )
     */
    default void onAuthResult(final int result, final String id, final SensitiveStringProvider v) {
    }

    /**
     * Requests a password from user.
     *
     * @param id      unique id for keyring or {@code null}
     * @param message a message id to show: {@link Message}
     * @param args    message arguments
     * @return a password or {@code null} if canceled
     */
    CharSequence promptPassword(String id, int message, Object... args);

    /**
     * Requests a binary answer from user.
     *
     * @param id      unique id for keyring or {@code null}
     * @param message a message id to show: {@link Message}
     * @param args    message arguments
     * @return user's answer
     */
    boolean promptYesNo(String id, int message, Object... args);

    /**
     * Shows a message.
     *
     * @param id      unique id or {@code null} (just in case)
     * @param message a message id to show: {@link Message}
     * @param args    message arguments
     */
    void showMessage(String id, int message, Object... args);

    /**
     * Shows a message.
     *
     * @param message a message to show
     */
    default void showMessage(final CharSequence message) {
        showMessage(null, Message.SIMPLE_MESSAGE, message, "en");
    }
}
