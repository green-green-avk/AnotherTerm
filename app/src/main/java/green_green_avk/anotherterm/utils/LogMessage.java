package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.util.Date;

public class LogMessage {
    public final String msg;
    @NonNull
    public final Date timestamp;

    public LogMessage(final String msg) {
        this.msg = msg;
        timestamp = new Date();
    }
}
