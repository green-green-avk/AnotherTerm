package green_green_avk.telnetclient;

// https://tools.ietf.org/html/rfc857

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
