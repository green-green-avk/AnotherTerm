package green_green_avk.telnetclient;

public class TelnetClientConnectionException extends TelnetClientException {
    public TelnetClientConnectionException() {
    }

    public TelnetClientConnectionException(final String s) {
        super(s);
    }

    public TelnetClientConnectionException(final Throwable throwable) {
        super(throwable);
    }
}
