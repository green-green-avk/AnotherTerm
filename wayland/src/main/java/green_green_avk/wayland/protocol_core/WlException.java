package green_green_avk.wayland.protocol_core;

public class WlException extends RuntimeException {
    public WlException() {
    }

    public WlException(final String message) {
        super(message);
    }

    public WlException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WlException(final Throwable cause) {
        super(cause);
    }
}
