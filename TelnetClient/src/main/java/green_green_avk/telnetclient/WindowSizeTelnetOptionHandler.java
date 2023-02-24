package green_green_avk.telnetclient;

/**
 * <a href="https://tools.ietf.org/html/rfc1073">RFC1073</a>
 */
public class WindowSizeTelnetOptionHandler extends TelnetClient.OptionHandler {
    public static final int ID = 31;

    protected static final byte[] WILL = msgWill(ID);

    protected boolean nSent = false;
    protected boolean enabled = false;
    protected byte[] subMsg = encode(0, 0);

    public WindowSizeTelnetOptionHandler() {
    }

    public WindowSizeTelnetOptionHandler(final int width, final int height) {
        subMsg = encode(width, height);
    }

    protected static byte[] encode(int width, int height) {
        if (width < 0 || width > 0xFFFF)
            width = 0;
        if (height < 0 || height > 0xFFFF)
            height = 0;
        return msgSub(ID, null,
                (byte) ((width >> 8) & 0xFF), (byte) (width & 0xFF),
                (byte) ((height >> 8) & 0xFF), (byte) (height & 0xFF)
        );
    }

    public void update(final int width, final int height) {
        subMsg = encode(width, height);
        if (enabled)
            sendRaw(subMsg);
    }

    @Override
    protected int id() {
        return ID;
    }

    @Override
    protected void onInit(final int id) {
        enabled = false;
        sendRaw(WILL);
        nSent = true;
    }

    @Override
    protected void onDo(final int id) {
        enabled = true;
        if (nSent)
            nSent = false;
        else
            sendRaw(WILL);
        sendRaw(subMsg);
    }

    @Override
    protected void onDont(final int id) {
        enabled = false;
        nSent = false;
    }
}
