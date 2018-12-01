package green_green_avk.anotherterm.backends;

public class BackendException extends RuntimeException {
    public BackendException(Throwable e) {
        super(e);
    }

    public BackendException(String message) {
        super(message);
    }
}
