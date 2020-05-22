package green_green_avk.anotherterm.backends;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class BackendInterruptedException extends RuntimeException {
    public BackendInterruptedException() {
    }

    public BackendInterruptedException(final String message) {
        super(message);
    }

    public BackendInterruptedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BackendInterruptedException(final Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public BackendInterruptedException(final String message, final Throwable cause,
                                       final boolean enableSuppression,
                                       final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
