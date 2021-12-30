package green_green_avk.anotherterm.wlterm;

import androidx.annotation.NonNull;

public class WlOwnCustomException extends Exception {
    public WlOwnCustomException() {
        super();
    }

    @Override
    @NonNull
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
