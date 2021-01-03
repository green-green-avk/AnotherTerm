package green_green_avk.telnetclient;

public class TelnetClientException extends RuntimeException {
    public TelnetClientException() {
        super();
    }

    public TelnetClientException(final String s) {
        super(s);
    }

    public TelnetClientException(final Throwable throwable) {
        super(throwable);
    }
}
