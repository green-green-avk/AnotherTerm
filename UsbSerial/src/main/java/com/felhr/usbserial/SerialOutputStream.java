package com.felhr.usbserial;

import java.io.OutputStream;

public class SerialOutputStream extends OutputStream {
    private int timeout = 0;

    protected final UsbSerialInterface device;

    public SerialOutputStream(final UsbSerialInterface device) {
        this.device = device;
    }

    @Override
    public void write(final int b) {
        device.syncWrite(new byte[]{(byte) b}, timeout);
    }

    @Override
    public void write(final byte[] b) {
        device.syncWrite(b, timeout);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        if (off < 0) {
            throw new IndexOutOfBoundsException("Offset must be >= 0");
        }

        if (len < 0) {
            throw new IndexOutOfBoundsException("Length must positive");
        }

        if (off + len > b.length) {
            throw new IndexOutOfBoundsException("off + length greater than buffer length");
        }

        if (off == 0 && len == b.length) {
            write(b);
            return;
        }

        device.syncWrite(b, off, len, timeout);
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }
}
