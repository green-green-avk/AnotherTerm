package com.jcraft.jsch;

/**
 * Unlikely to happen internal logic error but recoverable.
 * <p>
 * It also includes misconfiguration scenarios at the moment.
 */
public class JSchErrorException extends RuntimeException {
    public JSchErrorException() {
    }

    public JSchErrorException(final String message) {
        super(message);
    }

    public JSchErrorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JSchErrorException(final Throwable cause) {
        super(cause);
    }
}
