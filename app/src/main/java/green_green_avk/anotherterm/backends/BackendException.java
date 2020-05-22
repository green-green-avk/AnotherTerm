package green_green_avk.anotherterm.backends;

public class BackendException extends RuntimeException {
    public BackendException(final Throwable e) {
        super(e);
    }

    public BackendException(final String message) {
        super(message);
    }
}
