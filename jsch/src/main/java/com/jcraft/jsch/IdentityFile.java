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

final class IdentityFile implements Identity {
    private final JSch jsch;
    private KeyPair keyPair;
    private final String name;

    static IdentityFile newInstance(final String prvfile, final String pubfile, final JSch jsch)
            throws JSchException {
        return new IdentityFile(jsch, prvfile,
                KeyPair.load(jsch, prvfile, pubfile));
    }

    static IdentityFile newInstance(final String name,
                                    final byte[] prvkey, final byte[] pubkey, final JSch jsch)
            throws JSchException {
        return new IdentityFile(jsch, name,
                KeyPair.load(jsch, prvkey, pubkey));
    }

    private IdentityFile(final JSch jsch, final String name, final KeyPair keyPair)
            throws JSchException {
        this.jsch = jsch;
        this.name = name;
        this.keyPair = keyPair;
    }

    /**
     * Decrypts this identity with the specified pass-phrase.
     *
     * @param passphrase the pass-phrase for this identity.
     * @return {@code true} if the decryption is succeeded
     * or this identity is not cyphered.
     */
    @Override
    public boolean setPassphrase(final byte[] passphrase) throws JSchException {
        return keyPair.decrypt(passphrase);
    }

    /**
     * Returns the public-key blob.
     *
     * @return the public-key blob
     */
    @Override
    public byte[] getPublicKeyBlob() {
        return keyPair.getPublicKeyBlob();
    }

    /**
     * Signs on data with this identity, and returns the result.
     *
     * @param data data to be signed
     * @return the signature
     */
    @Override
    public byte[] getSignature(final byte[] data) {
        return keyPair.getSignature(data);
    }

    /**
     * Signs on data with this identity, and returns the result.
     *
     * @param data data to be signed
     * @param alg  signature algorithm to use
     * @return the signature
     */
    @Override
    public byte[] getSignature(final byte[] data, final String alg) {
        return keyPair.getSignature(data, alg);
    }

    /**
     * @see #setPassphrase(byte[] passphrase)
     * @deprecated This method should not be invoked.
     */
    @Override
    @Deprecated
    public boolean decrypt() {
        throw new RuntimeException("not implemented");
    }

    /**
     * Returns the name of the key algorithm.
     *
     * @return "ssh-rsa" or "ssh-dss"
     */
    @Override
    public String getAlgName() {
        return Util.byte2str(keyPair.getKeyTypeName());
    }

    /**
     * Returns the name of this identity.
     * It will be useful to identify this object in the {@link IdentityRepository}.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns {@code true} if this identity is cyphered.
     *
     * @return {@code true} if this identity is cyphered.
     */
    @Override
    public boolean isEncrypted() {
        return keyPair.isEncrypted();
    }

    /**
     * Disposes internally allocated data, like byte array for the private key.
     */
    @Override
    public void clear() {
        keyPair.dispose();
        keyPair = null;
    }

    /**
     * Returns an instance of {@link KeyPair} used in this {@link Identity}.
     *
     * @return an instance of {@link KeyPair} used in this {@link Identity}.
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }
}
