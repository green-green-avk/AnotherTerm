package green_green_avk.anotherterm.utils;

import java.util.Date;

public class LogMessage {
    public final String msg;
    public final Date timestamp;

    public LogMessage(final String msg) {
        this.msg = msg;
        timestamp = new Date();
    }
}
