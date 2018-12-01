package green_green_avk.anotherterm.backends;

public class BackendInterruptedException extends RuntimeException {
    public BackendInterruptedException() {
    }

    public BackendInterruptedException(String message) {
        super(message);
    }

    public BackendInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BackendInterruptedException(Throwable cause) {
        super(cause);
    }

    public BackendInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
