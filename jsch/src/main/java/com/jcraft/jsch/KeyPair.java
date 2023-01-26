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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class KeyPair {

    public static final int DEFERRED = -1;
    public static final int ERROR = 0;
    public static final int DSA = 1;
    public static final int RSA = 2;
    public static final int ECDSA = 3;
    public static final int UNKNOWN = 4;
    public static final int ED25519 = 5;
    public static final int ED448 = 6;

    static final int VENDOR_OPENSSH = 0;
    static final int VENDOR_FSECURE = 1;
    static final int VENDOR_PUTTY = 2;
    static final int VENDOR_PKCS8 = 3;
    static final int VENDOR_OPENSSH_V1 = 4;

    int vendor = VENDOR_OPENSSH;

    private static final byte[] AUTH_MAGIC = Util.str2byte("openssh-key-v1\0");
    private static final byte[] cr = Util.str2byte("\n");

    public static KeyPair genKeyPair(final JSch jsch, final int type) throws JSchException {
        return genKeyPair(jsch, type, 1024);
    }

    public static KeyPair genKeyPair(final JSch jsch, final int type, final int key_size)
            throws JSchException {
        final KeyPair kpair;
        switch (type) {
            case DSA:
                kpair = new KeyPairDSA(jsch);
                break;
            case RSA:
                kpair = new KeyPairRSA(jsch);
                break;
            case ECDSA:
                kpair = new KeyPairECDSA(jsch);
                break;
            case ED25519:
                kpair = new KeyPairEd25519(jsch);
                break;
            case ED448:
                kpair = new KeyPairEd448(jsch);
                break;
            default:
                kpair = null;
                break;
        }
        if (kpair != null) {
            kpair.generate(key_size);
        }
        return kpair;
    }

    abstract void generate(int key_size) throws JSchException;

    abstract byte[] getBegin();

    abstract byte[] getEnd();

    public abstract int getKeySize();

    public abstract byte[] getSignature(byte[] data);

    public abstract byte[] getSignature(byte[] data, String alg);

    public abstract Signature getVerifier();

    public abstract Signature getVerifier(String alg);

    public abstract byte[] forSSHAgent() throws JSchException;

    public String getPublicKeyComment() {
        return publicKeyComment;
    }

    public void setPublicKeyComment(final String publicKeyComment) {
        this.publicKeyComment = publicKeyComment;
    }

    protected String publicKeyComment = "no comment";

    final JSch jsch;
    protected Cipher cipher;
    private HASH hash;
    private Random random;

    private byte[] passphrase;

    protected String kdfName;
    protected byte[] kdfOptions;

    public KeyPair(final JSch jsch) {
        this.jsch = jsch;
    }

    static byte[][] header = {Util.str2byte("Proc-Type: 4,ENCRYPTED"),
            Util.str2byte("DEK-Info: DES-EDE3-CBC,")};

    abstract byte[] getPrivateKey();

    /**
     * Writes the plain private key to the given output stream.
     *
     * @param out output stream
     * @see #writePrivateKey(OutputStream out, byte[] passphrase)
     */
    public void writePrivateKey(final OutputStream out) {
        this.writePrivateKey(out, null);
    }

    /**
     * Writes the cyphered private key to the given output stream.
     *
     * @param out        output stream
     * @param passphrase a passphrase to encrypt the private key
     */
    public void writePrivateKey(final OutputStream out, byte[] passphrase) {
        if (passphrase == null)
            passphrase = this.passphrase;

        final byte[] plain = getPrivateKey();
        final byte[][] _iv = new byte[1][];
        final byte[] encoded = encrypt(plain, _iv, passphrase);
        if (encoded != plain)
            Util.bzero(plain);
        final byte[] iv = _iv[0];
        final byte[] prv = Util.toBase64(encoded, true);

        try {
            out.write(getBegin());
            out.write(cr);
            if (passphrase != null) {
                out.write(header[0]);
                out.write(cr);
                out.write(header[1]);
                for (final byte b : iv) {
                    out.write(b2a((byte) ((b >>> 4) & 0x0f)));
                    out.write(b2a((byte) (b & 0x0f)));
                }
                out.write(cr);
                out.write(cr);
            }
            int i = 0;
            while (i < prv.length) {
                if (i + 64 < prv.length) {
                    out.write(prv, i, 64);
                    out.write(cr);
                    i += 64;
                    continue;
                }
                out.write(prv, i, prv.length - i);
                out.write(cr);
                break;
            }
            out.write(getEnd());
            out.write(cr);
            //out.close();
        } catch (final Exception ignored) {
        }
    }

    private static final byte[] space = Util.str2byte(" ");

    abstract byte[] getKeyTypeName();

    public abstract int getKeyType();

    /**
     * Returns the blob of the public key.
     *
     * @return blob of the public key
     */
    public byte[] getPublicKeyBlob() {
        // TODO JSchException should be thrown
        //if(publickeyblob == null)
        //  throw new JSchException("public-key blob is not available");
        return publickeyblob;
    }

    /**
     * Writes the public key with the specified comment to the output stream.
     *
     * @param out     output stream
     * @param comment comment
     */
    public void writePublicKey(final OutputStream out, final String comment) {
        final byte[] pubblob = getPublicKeyBlob();
        final byte[] pub = Util.toBase64(pubblob, true);
        try {
            out.write(getKeyTypeName());
            out.write(space);
            out.write(pub, 0, pub.length);
            out.write(space);
            out.write(Util.str2byte(comment));
            out.write(cr);
        } catch (final Exception ignored) {
        }
    }

    /**
     * Writes the public key with the specified comment to the file.
     *
     * @param name    file name
     * @param comment comment
     * @see #writePublicKey(OutputStream out, String comment)
     */
    public void writePublicKey(final String name, final String comment)
            throws IOException {
        final FileOutputStream fos = new FileOutputStream(name);
        writePublicKey(fos, comment);
        fos.close();
    }

    /**
     * Writes the public key with the specified comment to the output stream in
     * the format defined in http://www.ietf.org/rfc/rfc4716.txt
     *
     * @param out     output stream
     * @param comment comment
     */
    public void writeSECSHPublicKey(final OutputStream out, final String comment) {
        final byte[] pubblob = getPublicKeyBlob();
        final byte[] pub = Util.toBase64(pubblob, true);
        try {
            out.write(Util.str2byte("---- BEGIN SSH2 PUBLIC KEY ----"));
            out.write(cr);
            out.write(Util.str2byte("Comment: \"" + comment + "\""));
            out.write(cr);
            int index = 0;
            while (index < pub.length) {
                int len = 70;
                if ((pub.length - index) < len) len = pub.length - index;
                out.write(pub, index, len);
                out.write(cr);
                index += len;
            }
            out.write(Util.str2byte("---- END SSH2 PUBLIC KEY ----"));
            out.write(cr);
        } catch (final Exception ignored) {
        }
    }

    /**
     * Writes the public key with the specified comment to the output stream in
     * the format defined in http://www.ietf.org/rfc/rfc4716.txt
     *
     * @param name    file name
     * @param comment comment
     * @see #writeSECSHPublicKey(OutputStream out, String comment)
     */
    public void writeSECSHPublicKey(final String name, final String comment)
            throws IOException {
        final FileOutputStream fos = new FileOutputStream(name);
        writeSECSHPublicKey(fos, comment);
        fos.close();
    }

    /**
     * Writes the plain private key to the file.
     *
     * @param name file name
     * @see #writePrivateKey(String name, byte[] passphrase)
     */
    public void writePrivateKey(final String name)
            throws IOException {
        this.writePrivateKey(name, null);
    }

    /**
     * Writes the cyphered private key to the file.
     *
     * @param name       file name
     * @param passphrase a passphrase to encrypt the private key
     * @see #writePrivateKey(OutputStream out, byte[] passphrase)
     */
    public void writePrivateKey(final String name, final byte[] passphrase)
            throws IOException {
        final FileOutputStream fos = new FileOutputStream(name);
        writePrivateKey(fos, passphrase);
        fos.close();
    }

    /**
     * Returns the finger-print of the public key.
     *
     * @return finger print
     */
    public String getFingerPrint() {
        if (hash == null)
            hash = genHash();
        final byte[] kblob = getPublicKeyBlob();
        if (kblob == null)
            return null;
        try {
            return Util.getFingerPrint(hash, kblob);
        } catch (final JSchException e) {
            throw new JSchErrorException(e);
        }
    }

    private byte[] encrypt(final byte[] plain, final byte[][] _iv, final byte[] passphrase) {
        if (passphrase == null) return plain;

        if (cipher == null) cipher = genCipher();
        final byte[] iv = _iv[0] = new byte[cipher.getIVSize()];

        if (random == null) random = genRandom();
        random.fill(iv, 0, iv.length);

        final byte[] key = genKey(passphrase, iv);
        byte[] encoded = plain;

        // PKCS#5Padding
        {
            //int bsize=cipher.getBlockSize();
            final int bsize = cipher.getIVSize();
            final byte[] foo = new byte[(encoded.length / bsize + 1) * bsize];
            System.arraycopy(encoded, 0, foo, 0, encoded.length);
            final int padding = bsize - encoded.length % bsize;
            for (int i = foo.length - 1; (foo.length - padding) <= i; i--) {
                foo[i] = (byte) padding;
            }
            encoded = foo;
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            cipher.update(encoded, 0, encoded.length, encoded, 0);
        } catch (final Exception e) {
            //System.err.println(e);
        }
        Util.bzero(key);
        return encoded;
    }

    abstract boolean parse(byte[] data);

    private byte[] decrypt(final byte[] data, final byte[] passphrase, final byte[] iv) {
        try {
            final byte[] key = genKey(passphrase, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            Util.bzero(key);
            final byte[] plain = new byte[data.length];
            cipher.update(data, 0, data.length, plain, 0);
            return plain;
        } catch (final Exception e) {
            throw new JSchErrorException(e.getLocalizedMessage(), e);
        }
    }

    int writeSEQUENCE(final byte[] buf, int index, final int len) {
        buf[index++] = 0x30;
        index = writeLength(buf, index, len);
        return index;
    }

    int writeINTEGER(final byte[] buf, int index, final byte[] data) {
        buf[index++] = 0x02;
        index = writeLength(buf, index, data.length);
        System.arraycopy(data, 0, buf, index, data.length);
        index += data.length;
        return index;
    }

    int writeOCTETSTRING(final byte[] buf, int index, final byte[] data) {
        buf[index++] = 0x04;
        index = writeLength(buf, index, data.length);
        System.arraycopy(data, 0, buf, index, data.length);
        index += data.length;
        return index;
    }

    int writeDATA(final byte[] buf, final byte n, int index, final byte[] data) {
        buf[index++] = n;
        index = writeLength(buf, index, data.length);
        System.arraycopy(data, 0, buf, index, data.length);
        index += data.length;
        return index;
    }

    int countLength(int len) {
        int i = 1;
        if (len <= 0x7f) return i;
        while (len > 0) {
            len >>>= 8;
            i++;
        }
        return i;
    }

    int writeLength(final byte[] data, int index, int len) {
        int i = countLength(len) - 1;
        if (i == 0) {
            data[index++] = (byte) len;
            return index;
        }
        data[index++] = (byte) (0x80 | i);
        final int j = index + i;
        while (i > 0) {
            data[index + i - 1] = (byte) (len & 0xff);
            len >>>= 8;
            i--;
        }
        return j;
    }

    private Random genRandom() {
        if (random == null) {
            try {
                final Class<? extends Random> c =
                        Class.forName(JSch.getConfig("random")).asSubclass(Random.class);
                random = c.getDeclaredConstructor().newInstance();
            } catch (final Exception e) {
                jsch.getInstanceLogger().log(Logger.ERROR,
                        "Unable to load 'random' class", e);
                throw new JSchErrorException("Unable to load 'random' class: " + e, e);
            }
        }
        return random;
    }

    private HASH genHash() {
        try {
            final Class<? extends HASH> c =
                    Class.forName(JSch.getConfig("md5")).asSubclass(HASH.class);
            hash = c.getDeclaredConstructor().newInstance();
            hash.init();
        } catch (final Exception e) {
            throw new JSchErrorException("Unable to load 'md5' class: " + e, e);
        }
        return hash;
    }

    private Cipher genCipher() {
        try {
            final Class<? extends Cipher> c =
                    Class.forName(JSch.getConfig("3des-cbc")).asSubclass(Cipher.class);
            cipher = c.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new JSchErrorException("Unable to load '3des-cbc' class: " + e, e);
        }
        return cipher;
    }

    /*
    hash is MD5
    h(0) <- hash(passphrase, iv);
    h(n) <- hash(h(n-1), passphrase, iv);
    key <- (h(0),...,h(n))[0,..,key.length];
  */
    synchronized byte[] genKey(final byte[] passphrase, final byte[] iv) {
        if (cipher == null)
            cipher = genCipher();
        if (hash == null)
            hash = genHash();

        byte[] key = new byte[cipher.getBlockSize()];
        final int hsize = hash.getBlockSize();
        final byte[] hn = new byte[key.length / hsize * hsize +
                (key.length % hsize == 0 ? 0 : hsize)];
        try {
            byte[] tmp = null;
            if (vendor == VENDOR_OPENSSH) {
                for (int index = 0; index + hsize <= hn.length; ) {
                    if (tmp != null) {
                        hash.update(tmp, 0, tmp.length);
                    }
                    hash.update(passphrase, 0, passphrase.length);
                    hash.update(iv, 0, iv.length > 8 ? 8 : iv.length);
                    tmp = hash.digest();
                    System.arraycopy(tmp, 0, hn, index, tmp.length);
                    index += tmp.length;
                }
                System.arraycopy(hn, 0, key, 0, key.length);
            } else if (vendor == VENDOR_FSECURE) {
                for (int index = 0; index + hsize <= hn.length; ) {
                    if (tmp != null) {
                        hash.update(tmp, 0, tmp.length);
                    }
                    hash.update(passphrase, 0, passphrase.length);
                    tmp = hash.digest();
                    System.arraycopy(tmp, 0, hn, index, tmp.length);
                    index += tmp.length;
                }
                System.arraycopy(hn, 0, key, 0, key.length);
            } else if (vendor == VENDOR_PUTTY) {
                final Class<? extends HASH> c =
                        Class.forName(JSch.getConfig("sha-1")).asSubclass(HASH.class);
                final HASH sha1 = c.getDeclaredConstructor().newInstance();
                tmp = new byte[4];
                key = new byte[20 * 2];
                for (int i = 0; i < 2; i++) {
                    sha1.init();
                    tmp[3] = (byte) i;
                    sha1.update(tmp, 0, tmp.length);
                    sha1.update(passphrase, 0, passphrase.length);
                    System.arraycopy(sha1.digest(), 0, key, i * 20, 20);
                }
            }
        } catch (final Exception e) {
            throw new JSchErrorException(e.getLocalizedMessage(), e);
        }
        return key;
    }

    /**
     * @deprecated use #writePrivateKey(OutputStream out, byte[] passphrase)
     */
    @Deprecated
    public void setPassphrase(final String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) {
            setPassphrase((byte[]) null);
        } else {
            setPassphrase(Util.str2byte(passphrase));
        }
    }

    /**
     * @deprecated use #writePrivateKey(String name, byte[] passphrase)
     */
    @Deprecated
    public void setPassphrase(final byte[] passphrase) {
        if (passphrase != null && passphrase.length == 0)
            this.passphrase = null;
        else
            this.passphrase = passphrase;
    }

    protected boolean encrypted = false;
    protected byte[] data = null;
    private byte[] iv = null;
    private byte[] publickeyblob = null;

    public boolean isEncrypted() {
        return encrypted;
    }

    public boolean decrypt(final String _passphrase) {
        if (_passphrase == null || _passphrase.isEmpty()) {
            return !encrypted;
        }
        return decrypt(Util.str2byte(_passphrase));
    }

    public boolean decrypt(byte[] _passphrase) {
        if (!encrypted) {
            return true;
        }
        if (_passphrase == null) {
            return !encrypted;
        }
        final byte[] bar = new byte[_passphrase.length];
        System.arraycopy(_passphrase, 0, bar, 0, bar.length);
        _passphrase = bar;
        final byte[] foo = decrypt(data, _passphrase, iv);
        Util.bzero(_passphrase);
        if (parse(foo)) {
            encrypted = false;
        }
        return !encrypted;
    }

    public static KeyPair load(final JSch jsch, final String prvkey)
            throws JSchException {
        String pubkey = prvkey + ".pub";
        if (!new File(pubkey).exists()) {
            pubkey = null;
        }
        return load(jsch, prvkey, pubkey);
    }

    public static KeyPair load(final JSch jsch, final String prvfile, final String pubfile)
            throws JSchException {

        final byte[] prvkey;
        byte[] pubkey = null;

        try {
            prvkey = Util.fromFile(prvfile);
        } catch (final IOException e) {
            throw new JSchException(e.toString(), e);
        }

        String _pubfile = pubfile;
        if (pubfile == null) {
            _pubfile = prvfile + ".pub";
        }

        try {
            pubkey = Util.fromFile(_pubfile);
        } catch (final IOException e) {
            if (pubfile != null) {
                throw new JSchException(e.toString(), e);
            }
        }

        try {
            return load(jsch, prvkey, pubkey);
        } finally {
            Util.bzero(prvkey);
        }
    }

    public static KeyPair load(final JSch jsch, final byte[] prvkey, byte[] pubkey)
            throws JSchException {

        byte[] iv = new byte[8];       // 8
        boolean encrypted = true;
        byte[] data = null;

        byte[] publickeyblob = null;

        int type = ERROR;
        int vendor = VENDOR_OPENSSH;
        String publicKeyComment = "";
        Cipher cipher = null;
        String kdfName = null;
        byte[] kdfOptions = null;

        // prvkey from "ssh-add" command on the remote.
        if (pubkey == null &&
                prvkey != null &&
                (prvkey.length > 11 &&
                        prvkey[0] == 0 && prvkey[1] == 0 && prvkey[2] == 0 &&
                        // length of key type string
                        (prvkey[3] == 7 || prvkey[3] == 9 || prvkey[3] == 11 || prvkey[3] == 19))) {

            final Buffer buf = new Buffer(prvkey);
            buf.skip(prvkey.length);  // for using Buffer#available()
            final String _type = Util.byte2str(buf.getString()); // ssh-rsa or ssh-dss
            buf.rewind();

            final KeyPair kpair;
            switch (_type) {
                case "ssh-rsa":
                    kpair = KeyPairRSA.fromSSHAgent(jsch, buf);
                    break;
                case "ssh-dss":
                    kpair = KeyPairDSA.fromSSHAgent(jsch, buf);
                    break;
                case "ecdsa-sha2-nistp256":
                case "ecdsa-sha2-nistp384":
                case "ecdsa-sha2-nistp521":
                    kpair = KeyPairECDSA.fromSSHAgent(jsch, buf);
                    break;
                case "ssh-ed25519":
                    kpair = KeyPairEd25519.fromSSHAgent(jsch, buf);
                    break;
                case "ssh-ed448":
                    kpair = KeyPairEd448.fromSSHAgent(jsch, buf);
                    break;
                default:
                    throw new JSchException("privatekey: invalid key " + Util.byte2str(prvkey, 4, 7));
            }
            return kpair;
        }

        try {
            byte[] buf = prvkey;

            if (buf != null) {
                final KeyPair ppk = loadPPK(jsch, buf);
                if (ppk != null)
                    return ppk;
            }

            int len = (buf != null ? buf.length : 0);
            int i = 0;

            // skip garbage lines.
            while (i < len) {
                if (buf[i] == '-' && i + 4 < len &&
                        buf[i + 1] == '-' && buf[i + 2] == '-' &&
                        buf[i + 3] == '-' && buf[i + 4] == '-') {
                    break;
                }
                i++;
            }

            while (i < len) {
                if (buf[i] == 'B' && i + 3 < len && buf[i + 1] == 'E' && buf[i + 2] == 'G' && buf[i + 3] == 'I') {
                    i += 6;
                    if (i + 2 >= len)
                        throw new JSchException("invalid privatekey");
                    if (buf[i] == 'D' && buf[i + 1] == 'S' && buf[i + 2] == 'A') {
                        type = DSA;
                    } else if (buf[i] == 'R' && buf[i + 1] == 'S' && buf[i + 2] == 'A') {
                        type = RSA;
                    } else if (buf[i] == 'E' && buf[i + 1] == 'C') {
                        type = ECDSA;
                    } else if (buf[i] == 'S' && buf[i + 1] == 'S' && buf[i + 2] == 'H') { // FSecure
                        type = UNKNOWN;
                        vendor = VENDOR_FSECURE;
                    } else if (i + 6 < len &&
                            buf[i] == 'P' && buf[i + 1] == 'R' &&
                            buf[i + 2] == 'I' && buf[i + 3] == 'V' &&
                            buf[i + 4] == 'A' && buf[i + 5] == 'T' && buf[i + 6] == 'E') {
                        type = UNKNOWN;
                        vendor = VENDOR_PKCS8;
                        encrypted = false;
                        i += 3;
                    } else if (i + 8 < len &&
                            buf[i] == 'E' && buf[i + 1] == 'N' &&
                            buf[i + 2] == 'C' && buf[i + 3] == 'R' &&
                            buf[i + 4] == 'Y' && buf[i + 5] == 'P' && buf[i + 6] == 'T' &&
                            buf[i + 7] == 'E' && buf[i + 8] == 'D') {
                        type = UNKNOWN;
                        vendor = VENDOR_PKCS8;
                        i += 5;

                    } else if (isOpenSSHPrivateKey(buf, i, len)) {
                        type = UNKNOWN;
                        vendor = VENDOR_OPENSSH_V1;
                    } else {
                        throw new JSchException("invalid privatekey");
                    }
                    i += 3;
                    continue;
                }
                if (buf[i] == 'A' && i + 7 < len && buf[i + 1] == 'E' && buf[i + 2] == 'S' && buf[i + 3] == '-' &&
                        buf[i + 4] == '2' && buf[i + 5] == '5' && buf[i + 6] == '6' && buf[i + 7] == '-') {
                    i += 8;
                    if (Session.checkCipher(JSch.getConfig("aes256-cbc"))) {
                        final Class<? extends Cipher> c =
                                Class.forName(JSch.getConfig("aes256-cbc"))
                                        .asSubclass(Cipher.class);
                        cipher = c.getDeclaredConstructor().newInstance();
                        // key=new byte[cipher.getBlockSize()];
                        iv = new byte[cipher.getIVSize()];
                    } else {
                        throw new JSchException("privatekey: aes256-cbc is not available");
                    }
                    continue;
                }
                if (buf[i] == 'A' && i + 7 < len && buf[i + 1] == 'E' && buf[i + 2] == 'S' && buf[i + 3] == '-' &&
                        buf[i + 4] == '1' && buf[i + 5] == '9' && buf[i + 6] == '2' && buf[i + 7] == '-') {
                    i += 8;
                    if (Session.checkCipher(JSch.getConfig("aes192-cbc"))) {
                        final Class<? extends Cipher> c =
                                Class.forName(JSch.getConfig("aes192-cbc"))
                                        .asSubclass(Cipher.class);
                        cipher = c.getDeclaredConstructor().newInstance();
                        // key=new byte[cipher.getBlockSize()];
                        iv = new byte[cipher.getIVSize()];
                    } else {
                        throw new JSchException("privatekey: aes192-cbc is not available");
                    }
                    continue;
                }
                if (buf[i] == 'A' && i + 7 < len && buf[i + 1] == 'E' && buf[i + 2] == 'S' && buf[i + 3] == '-' &&
                        buf[i + 4] == '1' && buf[i + 5] == '2' && buf[i + 6] == '8' && buf[i + 7] == '-') {
                    i += 8;
                    if (Session.checkCipher(JSch.getConfig("aes128-cbc"))) {
                        final Class<? extends Cipher> c =
                                Class.forName(JSch.getConfig("aes128-cbc"))
                                        .asSubclass(Cipher.class);
                        cipher = c.getDeclaredConstructor().newInstance();
                        // key=new byte[cipher.getBlockSize()];
                        iv = new byte[cipher.getIVSize()];
                    } else {
                        throw new JSchException("privatekey: aes128-cbc is not available");
                    }
                    continue;
                }
                if (buf[i] == 'C' && i + 3 < len && buf[i + 1] == 'B' && buf[i + 2] == 'C' && buf[i + 3] == ',') {
                    i += 4;
                    for (int ii = 0; ii < iv.length; ii++) {
                        iv[ii] = (byte) (((a2b(buf[i++]) << 4) & 0xf0) + (a2b(buf[i++]) & 0xf));
                    }
                    continue;
                }
                if (buf[i] == 0x0d && i + 1 < buf.length && buf[i + 1] == 0x0a) {
                    i++;
                    continue;
                }
                if (buf[i] == 0x0a && i + 1 < buf.length) {
                    if (buf[i + 1] == 0x0a) {
                        i += 2;
                        break;
                    }
                    if (buf[i + 1] == 0x0d &&
                            i + 2 < buf.length && buf[i + 2] == 0x0a) {
                        i += 3;
                        break;
                    }
                    boolean inheader = false;
                    for (int j = i + 1; j < buf.length; j++) {
                        if (buf[j] == 0x0a) break;
                        //if(buf[j]==0x0d) break;
                        if (buf[j] == ':') {
                            inheader = true;
                            break;
                        }
                    }
                    if (!inheader) {
                        i++;
                        if (vendor != VENDOR_PKCS8)
                            encrypted = false;    // no passphrase
                        break;
                    }
                }
                i++;
            }

            if (buf != null) {

                if (type == ERROR) {
                    throw new JSchException("invalid privatekey");
                }

                int start = i;
                while (i < len) {
                    if (buf[i] == '-') {
                        break;
                    }
                    i++;
                }

                if ((len - i) == 0 || (i - start) == 0) {
                    throw new JSchException("invalid privatekey");
                }

                // The content of 'buf' will be changed, so it should be copied.
                final byte[] tmp = new byte[i - start];
                System.arraycopy(buf, start, tmp, 0, tmp.length);
                final byte[] _buf = tmp;

                start = 0;
                i = 0;

                int _len = _buf.length;
                while (i < _len) {
                    if (_buf[i] == 0x0a) {
                        final boolean xd = (_buf[i - 1] == 0x0d);
                        // ignore 0x0a (or 0x0d0x0a)
                        System.arraycopy(_buf, i + 1, _buf, i - (xd ? 1 : 0), _len - (i + 1));
                        if (xd) _len--;
                        _len--;
                        continue;
                    }
                    if (_buf[i] == '-') {
                        break;
                    }
                    i++;
                }

                if (i - start > 0)
                    data = Util.fromBase64(_buf, start, i - start);

                Util.bzero(_buf);
            }

            if (data != null &&
                    data.length > 4 &&            // FSecure
                    data[0] == (byte) 0x3f &&
                    data[1] == (byte) 0x6f &&
                    data[2] == (byte) 0xf9 &&
                    data[3] == (byte) 0xeb) {

                final Buffer _buf = new Buffer(data);
                _buf.getInt();  // 0x3f6ff9be
                _buf.getInt();
                final byte[] _type = _buf.getString();
                //System.err.println("type: "+Util.byte2str(_type));
                final String _cipher = Util.byte2str(_buf.getString());
                //System.err.println("cipher: "+_cipher);
                switch (_cipher) {
                    case "3des-cbc": {
                        _buf.getInt();
                        final byte[] foo = new byte[data.length - _buf.getOffSet()];
                        _buf.getByte(foo);
                        data = foo;
                        encrypted = true;
                        throw new JSchException("unknown privatekey format"); // TODO: investigate
                    }
                    case "none": {
                        _buf.getInt();
                        _buf.getInt();

                        encrypted = false;

                        final byte[] foo = new byte[data.length - _buf.getOffSet()];
                        _buf.getByte(foo);
                        data = foo;
                        break;
                    }
                }
            }
            // OPENSSH V1 PRIVATE KEY
            else if (data != null &&
                    Arrays.equals(AUTH_MAGIC, Arrays.copyOfRange(data, 0, AUTH_MAGIC.length))) {

                vendor = VENDOR_OPENSSH_V1;
                final Buffer buffer = new Buffer(data);
                final byte[] magic = new byte[AUTH_MAGIC.length];
                buffer.getByte(magic);

                final String cipherName = Util.byte2str(buffer.getString());
                kdfName = Util.byte2str(buffer.getString()); // string kdfname
                kdfOptions = buffer.getString(); // string kdfoptions

                final int nrKeys = buffer.getInt(); // int number of keys N; Should be 1
                if (nrKeys != 1) {
                    throw new IOException("We don't support having more than 1 key in the file (yet).");
                }

                pubkey = buffer.getString();

                if ("none".equals(cipherName)) {
                    encrypted = false;
                    data = buffer.getString();
                    type = readOpenSSHKeyv1(data);
                } else if (Session.checkCipher(JSch.getConfig(cipherName))) {
                    encrypted = true;
                    final Class<? extends Cipher> c =
                            Class.forName(JSch.getConfig(cipherName))
                                    .asSubclass(Cipher.class);
                    cipher = c.getDeclaredConstructor().newInstance();
                    data = buffer.getString();
                    // the type can only be determined after encryption, so we take this intermediate here:
                    type = DEFERRED;
                } else {
                    throw new JSchException("cipher " + cipherName + " is not available");
                }
            }

            if (pubkey != null) {
                try {
                    buf = pubkey;
                    len = buf.length;
                    if (buf.length > 4 &&             // FSecure's public key
                            buf[0] == '-' && buf[1] == '-' && buf[2] == '-' && buf[3] == '-') {

                        boolean valid = true;
                        i = 0;
                        do {
                            i++;
                        } while (buf.length > i && buf[i] != 0x0a);
                        if (buf.length <= i) {
                            valid = false;
                        }

                        while (valid) {
                            if (buf[i] == 0x0a) {
                                boolean inheader = false;
                                for (int j = i + 1; j < buf.length; j++) {
                                    if (buf[j] == 0x0a) break;
                                    if (buf[j] == ':') {
                                        inheader = true;
                                        break;
                                    }
                                }
                                if (!inheader) {
                                    i++;
                                    break;
                                }
                            }
                            i++;
                        }
                        if (buf.length <= i) {
                            valid = false;
                        }

                        final int start = i;
                        while (valid && i < len) {
                            if (buf[i] == 0x0a) {
                                System.arraycopy(buf, i + 1, buf, i, len - i - 1);
                                len--;
                                continue;
                            }
                            if (buf[i] == '-') {
                                break;
                            }
                            i++;
                        }
                        if (valid) {
                            publickeyblob = Util.fromBase64(buf, start, i - start);
                            if (prvkey == null || type == UNKNOWN) {
                                if (publickeyblob[8] == 'd') {
                                    type = DSA;
                                } else if (publickeyblob[8] == 'r') {
                                    type = RSA;
                                }
                            }
                        }
                    } else {
                        if (buf[0] == 's' && buf[1] == 's' && buf[2] == 'h' && buf[3] == '-') {
                            if (prvkey == null &&
                                    buf.length > 7) {
                                if (buf[4] == 'd') {
                                    type = DSA;
                                } else if (buf[4] == 'r') {
                                    type = RSA;
                                } else if (buf[4] == 'e' && buf[6] == '2') {
                                    type = ED25519;
                                } else if (buf[4] == 'e' && buf[6] == '4') {
                                    type = ED448;
                                }
                            }
                            i = 0;
                            while (i < len) {
                                if (buf[i] == ' ') break;
                                i++;
                            }
                            i++;
                            if (i < len) {
                                final int start = i;
                                while (i < len) {
                                    if (buf[i] == ' ') break;
                                    i++;
                                }
                                publickeyblob = Util.fromBase64(buf, start, i - start);
                            }
                            if (i++ < len) {
                                final int start = i;
                                while (i < len) {
                                    if (buf[i] == '\n') break;
                                    i++;
                                }
                                if (i > 0 && buf[i - 1] == 0x0d) i--;
                                if (start < i) {
                                    publicKeyComment = Util.byte2str(buf, start, i - start);
                                }
                            }
                        } else if (buf[0] == 'e' && buf[1] == 'c' && buf[2] == 'd' && buf[3] == 's') {
                            if (prvkey == null && buf.length > 7) {
                                type = ECDSA;
                            }
                            i = 0;
                            while (i < len) {
                                if (buf[i] == ' ') break;
                                i++;
                            }
                            i++;
                            if (i < len) {
                                final int start = i;
                                while (i < len) {
                                    if (buf[i] == ' ') break;
                                    i++;
                                }
                                publickeyblob = Util.fromBase64(buf, start, i - start);
                            }
                            if (i++ < len) {
                                final int start = i;
                                while (i < len) {
                                    if (buf[i] == '\n') break;
                                    i++;
                                }
                                if (i > 0 && buf[i - 1] == 0x0d) i--;
                                if (start < i) {
                                    publicKeyComment = Util.byte2str(buf, start, i - start);
                                }
                            }
                        }
                    }
                } catch (final Exception ignored) {
                }
            }
        } catch (final JSchException | JSchErrorException e) {
            throw e;
        } catch (final Exception e) {
            throw new JSchException(e.toString(), e);
        }

        return getKeyPair(jsch, prvkey, pubkey, iv, encrypted, data, publickeyblob, type, vendor, publicKeyComment, cipher, kdfName, kdfOptions);
    }

    static KeyPair getKeyPair(final JSch jsch, final byte[] prvkey, final byte[] pubkey, final byte[] iv, final boolean encrypted, final byte[] data, final byte[] publickeyblob, final int type, final int vendor, final String publicKeyComment, final Cipher cipher, final String kdfName, final byte[] kdfOptions) throws JSchException {
        final KeyPair kpair;
        if (type == DSA) {
            kpair = new KeyPairDSA(jsch);
        } else if (type == RSA) {
            kpair = new KeyPairRSA(jsch);
        } else if (type == ECDSA) {
            kpair = new KeyPairECDSA(jsch, pubkey);
        } else if (type == ED25519) {
            kpair = new KeyPairEd25519(jsch, pubkey, prvkey);
        } else if (type == ED448) {
            kpair = new KeyPairEd448(jsch, pubkey, prvkey);
        } else if (vendor == VENDOR_PKCS8) {
            kpair = new KeyPairPKCS8(jsch);
        } else if (type == DEFERRED) {
            kpair = new KeyPairDeferred(jsch);
        } else {
            kpair = null;
        }

        if (kpair != null) {
            kpair.encrypted = encrypted;
            kpair.publickeyblob = publickeyblob;
            kpair.vendor = vendor;
            kpair.publicKeyComment = publicKeyComment;
            kpair.cipher = cipher;
            kpair.kdfName = kdfName;
            kpair.kdfOptions = kdfOptions;

            if (encrypted) {
                kpair.iv = iv;
                kpair.data = data;
            } else {
                if (kpair.parse(data)) {
                    kpair.encrypted = false;
                    return kpair;
                } else {
                    throw new JSchException("invalid privatekey");
                }
            }
        }

        return kpair;
    }

    /**
     * reads openssh key v1 format and returns key type.
     *
     * @param data
     * @return key type 1=DSA, 2=RSA, 3=ECDSA, 4=UNKNOWN, 5=ED25519, 6=ED448
     * @throws IOException
     * @throws JSchException
     */
    static int readOpenSSHKeyv1(final byte[] data) throws IOException, JSchException {
        if (data.length % 8 != 0) {
            throw new IOException("The private key section must be a multiple of the block size (8)");
        }

        final Buffer prvKEyBuffer = new Buffer(data);
        final int checkInt1 = prvKEyBuffer.getInt(); // uint32 checkint1
        final int checkInt2 = prvKEyBuffer.getInt(); // uint32 checkint2
        if (checkInt1 != checkInt2) {
            throw new JSchException("openssh v1 key check failed. Wrong passphrase?");
        }

        // The private key section contains both the public key and the private key
        final String keyType = Util.byte2str(prvKEyBuffer.getString()); // string keytype

        if (keyType.equalsIgnoreCase("ssh-rsa")) {
            return RSA;
        } else if (keyType.startsWith("ssh-dss")) {
            return DSA;
        } else if (keyType.startsWith("ecdsa-sha2")) {
            return ECDSA;
        } else if (keyType.startsWith("ssh-ed25519")) {
            return ED25519;
        } else if (keyType.startsWith("ssh-ed448")) {
            return ED448;
        } else
            throw new JSchException("keytype " + keyType +
                    " not supported as part of openssh v1 format");
    }

    private static boolean isOpenSSHPrivateKey(final byte[] buf, final int i, final int len) {
        final String ident = "OPENSSH PRIVATE KEY-----";
        return i + ident.length() < len && ident.equals(Util.byte2str(
                Arrays.copyOfRange(buf, i, i + ident.length())));
    }

    private static byte a2b(final byte c) {
        if ('0' <= c && c <= '9') return (byte) (c - '0');
        return (byte) (c - 'a' + 10);
    }

    private static byte b2a(final byte c) {
        if (0 <= c && c <= 9) return (byte) (c + '0');
        return (byte) (c - 10 + 'A');
    }

    public void dispose() {
        Util.bzero(passphrase);
    }

    @Override
    protected void finalize() {
        dispose();
    }

    private static final String[] header1 = {
            "PuTTY-User-Key-File-2: ",
            "Encryption: ",
            "Comment: ",
            "Public-Lines: "
    };

    private static final String[] header2 = {
            "Private-Lines: "
    };

    private static final String[] header3 = {
            "Private-MAC: "
    };

    static KeyPair loadPPK(final JSch jsch, final byte[] buf) throws JSchException {
        byte[] pubkey = null;
        byte[] prvkey = null;
        int lines = 0;

        final Buffer buffer = new Buffer(buf);
        final Map<String, String> v = new HashMap<>();

        while (true) {
            if (!parseHeader(buffer, v))
                break;
        }

        final String typ = v.get("PuTTY-User-Key-File-2");
        if (typ == null) {
            return null;
        }

        lines = Integer.parseInt(v.get("Public-Lines"));
        pubkey = parseLines(buffer, lines);

        while (true) {
            if (!parseHeader(buffer, v))
                break;
        }

        lines = Integer.parseInt(v.get("Private-Lines"));
        prvkey = parseLines(buffer, lines);

        while (true) {
            if (!parseHeader(buffer, v))
                break;
        }

        prvkey = Util.fromBase64(prvkey, 0, prvkey.length);
        pubkey = Util.fromBase64(pubkey, 0, pubkey.length);

        final KeyPair kpair;

        switch (typ) {
            case "ssh-rsa": {
                final Buffer _buf = new Buffer(pubkey);
                _buf.skip(pubkey.length);

                final int len = _buf.getInt();
                _buf.getByte(new byte[len]);             // ssh-rsa

                final byte[] pub_array = new byte[_buf.getInt()];
                _buf.getByte(pub_array);
                final byte[] n_array = new byte[_buf.getInt()];
                _buf.getByte(n_array);

                kpair = new KeyPairRSA(jsch,
                        n_array, pub_array, null);
                break;
            }
            case "ssh-dss": {
                final Buffer _buf = new Buffer(pubkey);
                _buf.skip(pubkey.length);

                final int len = _buf.getInt();
                _buf.getByte(new byte[len]);              // ssh-dss


                final byte[] p_array = new byte[_buf.getInt()];
                _buf.getByte(p_array);
                final byte[] q_array = new byte[_buf.getInt()];
                _buf.getByte(q_array);
                final byte[] g_array = new byte[_buf.getInt()];
                _buf.getByte(g_array);
                final byte[] y_array = new byte[_buf.getInt()];
                _buf.getByte(y_array);

                kpair = new KeyPairDSA(jsch,
                        p_array, q_array, g_array,
                        y_array, null);
                break;
            }
            default:
                return null;
        }

        kpair.encrypted = !v.get("Encryption").equals("none");
        kpair.vendor = VENDOR_PUTTY;
        kpair.publicKeyComment = v.get("Comment");
        if (kpair.encrypted) {
            if (Session.checkCipher(JSch.getConfig("aes256-cbc"))) {
                try {
                    final Class<? extends Cipher> c =
                            Class.forName(JSch.getConfig("aes256-cbc"))
                                    .asSubclass(Cipher.class);
                    kpair.cipher = c.getDeclaredConstructor().newInstance();
                    kpair.iv = new byte[kpair.cipher.getIVSize()];
                } catch (final Exception e) {
                    throw new JSchException("The cipher 'aes256-cbc' is required, but it is not available.");
                }
            } else {
                throw new JSchException("The cipher 'aes256-cbc' is required, but it is not available.");
            }
            kpair.data = prvkey;
        } else {
            kpair.data = prvkey;
            kpair.parse(prvkey);
        }
        return kpair;
    }

    private static byte[] parseLines(final Buffer buffer, int lines) {
        final byte[] buf = buffer.buffer;
        int index = buffer.index;
        byte[] data = null;

        int i = index;
        while (lines-- > 0) {
            while (buf.length > i) {
                if (buf[i++] == 0x0d) {
                    if (data == null) {
                        data = new byte[i - index - 1];
                        System.arraycopy(buf, index, data, 0, i - index - 1);
                    } else {
                        final byte[] tmp = new byte[data.length + i - index - 1];
                        System.arraycopy(data, 0, tmp, 0, data.length);
                        System.arraycopy(buf, index, tmp, data.length, i - index - 1);
                        Arrays.fill(data, (byte) 0); // clear
                        data = tmp;
                    }
                    break;
                }
            }
            if (buf[i] == 0x0a)
                i++;
            index = i;
        }

        if (data != null)
            buffer.index = index;

        return data;
    }

    private static boolean parseHeader(final Buffer buffer,
                                       final Map<? super String, ? super String> output) {
        final byte[] buf = buffer.buffer;
        int index = buffer.index;
        String key = null;
        String value = null;
        for (int i = index; i < buf.length; i++) {
            if (buf[i] == 0x0d) {
                break;
            }
            if (buf[i] == ':') {
                key = Util.byte2str(buf, index, i - index);
                i++;
                if (i < buf.length && buf[i] == ' ') {
                    i++;
                }
                index = i;
                break;
            }
        }

        if (key == null)
            return false;

        for (int i = index; i < buf.length; i++) {
            if (buf[i] == 0x0d) {
                value = Util.byte2str(buf, index, i - index);
                i++;
                if (i < buf.length && buf[i] == 0x0a) {
                    i++;
                }
                index = i;
                break;
            }
        }

        if (value != null) {
            output.put(key, value);
            buffer.index = index;
        }

        return value != null;
    }

    void copy(final KeyPair kpair) {
        this.publickeyblob = kpair.publickeyblob;
        this.vendor = kpair.vendor;
        this.publicKeyComment = kpair.publicKeyComment;
        this.cipher = kpair.cipher;
    }

    static final class ASN1Exception extends Exception {
        private static final long serialVersionUID = -1L;
    }

    static final class ASN1 {
        private final byte[] buf;
        private final int start;
        private final int length;

        ASN1(final byte[] buf) throws ASN1Exception {
            this(buf, 0, buf.length);
        }

        ASN1(final byte[] buf, final int start, final int length) throws ASN1Exception {
            this.buf = buf;
            this.start = start;
            this.length = length;
            if (start + length > buf.length)
                throw new ASN1Exception();
        }

        int getType() {
            return buf[start] & 0xff;
        }

        boolean isSEQUENCE() {
            return getType() == (0x30 & 0xff);
        }

        boolean isINTEGER() {
            return getType() == (0x02 & 0xff);
        }

        boolean isOBJECT() {
            return getType() == (0x06 & 0xff);
        }

        boolean isOCTETSTRING() {
            return getType() == (0x04 & 0xff);
        }

        private int getLength(final int[] indexp) {
            int index = indexp[0];
            int length = buf[index++] & 0xff;
            if ((length & 0x80) != 0) {
                int foo = length & 0x7f;
                length = 0;
                while (foo-- > 0) {
                    length = (length << 8) + (buf[index++] & 0xff);
                }
            }
            indexp[0] = index;
            return length;
        }

        byte[] getContent() {
            final int[] indexp = new int[1];
            indexp[0] = start + 1;
            final int length = getLength(indexp);
            final int index = indexp[0];
            final byte[] tmp = new byte[length];
            System.arraycopy(buf, index, tmp, 0, tmp.length);
            return tmp;
        }

        ASN1[] getContents() throws ASN1Exception {
            final int typ = buf[start];
            final int[] indexp = new int[1];
            indexp[0] = start + 1;
            int length = getLength(indexp);
            if (typ == 0x05) {
                return new ASN1[0];
            }
            int index = indexp[0];
            final List<ASN1> values = new ArrayList<>();
            while (length > 0) {
                index++;
                length--;
                final int tmp = index;
                indexp[0] = index;
                final int l = getLength(indexp);
                index = indexp[0];
                length -= (index - tmp);
                values.add(new ASN1(buf, tmp - 1, 1 + (index - tmp) + l));
                index += l;
                length -= l;
            }
            final ASN1[] result = new ASN1[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        }
    }
}
