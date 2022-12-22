package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.util.Date;

public class LogMessage {
    public enum Level {FATAL, ERROR, WARNING, INFO}

    public final CharSequence msg;
    @NonNull
    public final Date timestamp;
    @NonNull
    public final Level level;

    public LogMessage(@NonNull final Level level, final CharSequence msg) {
        this.msg = msg;
        timestamp = new Date();
        this.level = level;
    }
}
