package green_green_avk.bluetoothspp;

import java.io.IOException;

public final class BluetoothSPPException extends IOException {
    public enum Reason {INACCESSIBLE, SECURITY}

    public final Reason reason;

    public BluetoothSPPException(final Reason reason) {
        super("Bluetooth access failed: " + reason.toString());
        this.reason = reason;
    }

    public BluetoothSPPException(final Reason reason, final Throwable cause) {
        super("Bluetooth access failed: " + reason.toString(), cause);
        this.reason = reason;
    }
}
