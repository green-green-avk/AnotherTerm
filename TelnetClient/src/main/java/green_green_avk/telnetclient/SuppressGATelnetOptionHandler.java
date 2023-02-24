package green_green_avk.telnetclient;

/**
 * <a href="https://tools.ietf.org/html/rfc858">RFC858</a>
 */
public class SuppressGATelnetOptionHandler extends TelnetClient.OptionHandler {
    public static final int ID = 3;

    protected static final byte[] WILL = msgWill(ID);
    protected static final byte[] DO = msgDo(ID);

    protected boolean wSent = false;
    protected boolean dSent = false;

    @Override
    protected int id() {
        return ID;
    }

    @Override
    protected void onInit(final int id) {
        sendRaw(WILL);
        wSent = true;
        sendRaw(DO);
        dSent = true;
    }

    @Override
    protected void onWill(final int id) {
        if (dSent)
            dSent = false;
        else
            sendRaw(DO);
    }

    @Override
    protected void onDo(final int id) {
        if (wSent)
            wSent = false;
        else
            sendRaw(WILL);
    }
}
