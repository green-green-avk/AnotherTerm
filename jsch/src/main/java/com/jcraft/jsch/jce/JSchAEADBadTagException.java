package com.jcraft.jsch.jce;

import javax.crypto.BadPaddingException;

/**
 * Android API < 19 workaround.
 */
public final class JSchAEADBadTagException extends BadPaddingException {
    public JSchAEADBadTagException(final String msg) {
        super(msg);
    }

    JSchAEADBadTagException(final BadPaddingException e) {
        super(e.getLocalizedMessage());
        initCause(e);
    }
}
