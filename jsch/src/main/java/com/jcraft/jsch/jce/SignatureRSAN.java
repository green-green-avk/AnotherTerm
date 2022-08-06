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

package com.jcraft.jsch.jce;

import com.jcraft.jsch.Buffer;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

abstract class SignatureRSAN implements com.jcraft.jsch.SignatureRSA {
    private static final Charset UTF8 = Charset.forName("UTF8");

    private java.security.Signature signature;
    private KeyFactory keyFactory;

    abstract String getName();

    @Override
    public void init() throws Exception {
        final String name = getName();
        final String foo;
        switch (name) {
            case "rsa-sha2-256":
            case "ssh-rsa-sha256@ssh.com":
                foo = "SHA256withRSA";
                break;
            case "rsa-sha2-512":
            case "ssh-rsa-sha512@ssh.com":
                foo = "SHA512withRSA";
                break;
            case "ssh-rsa-sha384@ssh.com":
                foo = "SHA384withRSA";
                break;
            case "ssh-rsa-sha224@ssh.com":
                foo = "SHA224withRSA";
                break;
            default:
                foo = "SHA1withRSA";
        }
        signature = java.security.Signature.getInstance(foo);
        keyFactory = KeyFactory.getInstance("RSA");
    }

    @Override
    public void setPubKey(final byte[] e, final byte[] n) throws Exception {
        final RSAPublicKeySpec rsaPubKeySpec =
                new RSAPublicKeySpec(new BigInteger(n),
                        new BigInteger(e));
        final PublicKey pubKey = keyFactory.generatePublic(rsaPubKeySpec);
        signature.initVerify(pubKey);
    }

    @Override
    public void setPrvKey(final byte[] d, final byte[] n) throws Exception {
        final RSAPrivateKeySpec rsaPrivKeySpec =
                new RSAPrivateKeySpec(new BigInteger(n),
                        new BigInteger(d));
        final PrivateKey prvKey = keyFactory.generatePrivate(rsaPrivKeySpec);
        signature.initSign(prvKey);
    }

    @Override
    public byte[] sign() throws Exception {
        return signature.sign();
    }

    @Override
    public void update(final byte[] foo) throws Exception {
        signature.update(foo);
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        final Buffer buf = new Buffer(sig);

        final String foo = new String(buf.getString(), UTF8);
        switch (foo) {
            case "ssh-rsa":
            case "rsa-sha2-256":
            case "rsa-sha2-512":
            case "ssh-rsa-sha224@ssh.com":
            case "ssh-rsa-sha256@ssh.com":
            case "ssh-rsa-sha384@ssh.com":
            case "ssh-rsa-sha512@ssh.com": {
                if (!foo.equals(getName()))
                    return false;
                final int j = buf.getInt();
                final int i = buf.getOffSet();
                final byte[] tmp = new byte[j];
                System.arraycopy(sig, i, tmp, 0, j);
                sig = tmp;
                break;
            }
        }

        return signature.verify(sig);
    }
}
