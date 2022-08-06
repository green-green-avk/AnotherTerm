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

import com.jcraft.jsch.JSchException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public final class DH implements com.jcraft.jsch.DH {
    BigInteger p = null;
    BigInteger g = null;
    BigInteger e = null;  // my public key
    byte[] e_array = null;
    BigInteger f = null;  // your public key
    BigInteger K = null;  // shared secret key
    byte[] K_array = null;

    private KeyPairGenerator myKpairGen;
    private KeyAgreement myKeyAgree;

    @Override
    public void init() throws Exception {
        myKpairGen = KeyPairGenerator.getInstance("DH");
        myKeyAgree = KeyAgreement.getInstance("DH");
    }

    @Override
    public byte[] getE() throws Exception {
        if (e == null) {
            final DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);
            myKpairGen.initialize(dhSkipParamSpec);
            final KeyPair myKpair = myKpairGen.generateKeyPair();
            myKeyAgree.init(myKpair.getPrivate());
            e = ((DHPublicKey) (myKpair.getPublic())).getY();
            e_array = e.toByteArray();
        }
        return e_array;
    }

    @Override
    public byte[] getK() throws Exception {
        if (K == null) {
            final KeyFactory myKeyFac = KeyFactory.getInstance("DH");
            final DHPublicKeySpec keySpec = new DHPublicKeySpec(f, p, g);
            final PublicKey yourPubKey = myKeyFac.generatePublic(keySpec);
            myKeyAgree.doPhase(yourPubKey, true);
            final byte[] mySharedSecret = myKeyAgree.generateSecret();
            K = new BigInteger(1, mySharedSecret);
            K_array = K.toByteArray(); // TODO: investigate
            K_array = mySharedSecret;
        }
        return K_array;
    }

    @Override
    public void setP(final byte[] p) {
        setP(new BigInteger(1, p));
    }

    @Override
    public void setG(final byte[] g) {
        setG(new BigInteger(1, g));
    }

    @Override
    public void setF(final byte[] f) {
        setF(new BigInteger(1, f));
    }

    void setP(final BigInteger p) {
        this.p = p;
    }

    void setG(final BigInteger g) {
        this.g = g;
    }

    void setF(final BigInteger f) {
        this.f = f;
    }

    // e, f must be in [1, p-1].
    @Override
    public void checkRange() throws Exception {
    /*
    checkRange(e);
    checkRange(f);
    */
    }

    private void checkRange(final BigInteger tmp) throws Exception {
        final BigInteger one = BigInteger.ONE;
        final BigInteger p_1 = p.subtract(one);
        // !(1<tmp && tmp<p-1)  We expect tmp is in the range [2, p-2].
        if (!(one.compareTo(tmp) < 0 && tmp.compareTo(p_1) < 0)) {
            throw new JSchException("invalid DH value");
        }
    }
}
