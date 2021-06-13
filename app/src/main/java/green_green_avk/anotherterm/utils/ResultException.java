package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

public class ResultException extends Exception {
    public ResultException() {
        super();
    }

    public ResultException(final String message) {
        super(message);
    }

    @Override
    @NonNull
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
