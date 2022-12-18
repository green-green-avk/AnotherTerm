/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2015-2018 ymnk, JCraft,Inc. All rights reserved.

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

package com.jcraft.jsch.bc;

import com.jcraft.jsch.Buffer;

import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.signers.Ed448Signer;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;

abstract class SignatureEdDSA implements com.jcraft.jsch.SignatureEdDSA {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    Signer signature;

    abstract String getName();

    abstract String getAlgo();

    abstract int getKeylen();

    @Override
    public void init() throws Exception {
        if (!"Ed25519".equals(getAlgo()) && !"Ed448".equals(getAlgo())) {
            throw new NoSuchAlgorithmException("invalid curve " + getAlgo());
        }

        if ("Ed25519".equals(getAlgo())) {
            signature = new Ed25519Signer();
        } else {
            signature = new Ed448Signer(new byte[0]);
        }
    }

    @Override
    public void setPubKey(final byte[] y_arr) throws Exception {
        try {
            if ("Ed25519".equals(getAlgo())) {
                final Ed25519PublicKeyParameters pubKey =
                        new Ed25519PublicKeyParameters(y_arr, 0);
                signature.init(false, pubKey);
            } else {
                final Ed448PublicKeyParameters pubKey =
                        new Ed448PublicKeyParameters(y_arr, 0);
                signature.init(false, pubKey);
            }
        } catch (final Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public void setPrvKey(final byte[] bytes) throws Exception {
        try {
            if ("Ed25519".equals(getAlgo())) {
                final Ed25519PrivateKeyParameters prvKey =
                        new Ed25519PrivateKeyParameters(bytes, 0);
                signature.init(true, prvKey);
            } else {
                final Ed448PrivateKeyParameters prvKey =
                        new Ed448PrivateKeyParameters(bytes, 0);
                signature.init(true, prvKey);
            }
        } catch (final Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    public byte[] sign() throws Exception {
        try {
            return signature.generateSignature();
        } catch (final Exception e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public void update(final byte[] foo) throws Exception {
        try {
            signature.update(foo, 0, foo.length);
        } catch (final Exception e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        final Buffer buf = new Buffer(sig);

        final String foo = new String(buf.getString(), UTF_8);
        if (foo.equals(getName())) {
            final int j = buf.getInt();
            final int i = buf.getOffSet();
            sig = Arrays.copyOfRange(sig, i, i + j);
        }

        try {
            return signature.verifySignature(sig);
        } catch (final Exception e) {
            throw new SignatureException(e);
        }
    }
}
