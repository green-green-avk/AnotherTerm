package green_green_avk.anotherterm.utils;

public class ResultException extends Exception {
    public ResultException() {
        super();
    }

    public ResultException(final String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
