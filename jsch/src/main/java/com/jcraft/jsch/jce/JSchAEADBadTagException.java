package com.jcraft.jsch.jce;

import javax.crypto.BadPaddingException;

/**
 * Android API < 19 workaround.
 */
public final class JSchAEADBadTagException extends BadPaddingException {
    JSchAEADBadTagException(final BadPaddingException e) {
        super(e.getLocalizedMessage());
        initCause(e);
    }
}
