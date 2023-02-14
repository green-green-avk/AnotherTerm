package green_green_avk.telnetclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// https://tools.ietf.org/html/rfc1091

public class TerminalTypeTelnetOptionHandler extends TelnetClient.OptionHandler {
    public static final int ID = 24;

    protected static final byte[] WILL = msgWill(ID);
    protected static final byte SEND = 1;
    protected static final byte[] IS = {0};

    protected boolean nSent = false;
    protected boolean enabled = false;
    protected byte[] subMsg = encode(null);

    public TerminalTypeTelnetOptionHandler() {
    }

    public TerminalTypeTelnetOptionHandler(@Nullable final String value) {
        subMsg = encode(value);
    }

    protected static byte[] encode(@Nullable final String value) {
        if (value == null) return encode("UNKNOWN");
        return msgSub(ID, IS, value.getBytes(Charset.forName("ASCII")));
    }

    public void update(@Nullable final String value) {
        subMsg = encode(value);
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
        if (nSent) nSent = false;
        else sendRaw(WILL);
    }

    @Override
    protected void onDont(final int id) {
        enabled = false;
        nSent = false;
    }

    @Override
    protected void onSub(final int id, @NonNull final ByteBuffer sub) {
        if (sub.remaining() == 1 && sub.get(sub.position()) == SEND)
            sendRaw(subMsg);
    }
}
