package com.jcraft.jsch.jce;

import java.security.MessageDigest;

abstract class HASH implements com.jcraft.jsch.HASH {
    protected MessageDigest md = null;

    @Override
    public void update(final byte[] foo, final int start, final int len) throws Exception {
        md.update(foo, start, len);
    }

    @Override
    public byte[] digest() throws Exception {
        return md.digest();
    }
}
