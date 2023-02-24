package green_green_avk.telnetclient;

/**
 * <a href="https://tools.ietf.org/html/rfc857">RFC857</a>
 */
public class EchoTelnetOptionHandler extends TelnetClient.OptionHandler {
    public static final int ID = 1;

    protected static final byte[] DO = msgDo(ID);

    @Override
    protected int id() {
        return ID;
    }

    @Override
    protected void onWill(final int id) {
        sendRaw(DO);
    }
}
