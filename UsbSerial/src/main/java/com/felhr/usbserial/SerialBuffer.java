package com.felhr.usbserial;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import okio.Buffer;

public class SerialBuffer {
    static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    static final int MAX_BULK_BUFFER = 16 * 1024;
    private ByteBuffer readBuffer;

    private final SynchronizedBuffer writeBuffer;
    private byte[] readBufferCompatible; // Read buffer for android < 4.2
    private boolean debugging = false;

    public SerialBuffer(final boolean version) {
        writeBuffer = new SynchronizedBuffer();
        if (version) {
            readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);

        } else {
            readBufferCompatible = new byte[DEFAULT_READ_BUFFER_SIZE];
        }
    }

    /*
     * Print debug messages
     */
    public void debug(final boolean value) {
        debugging = value;
    }

    public ByteBuffer getReadBuffer() {
        synchronized (this) {
            return readBuffer;
        }
    }


    public byte[] getDataReceived() {
        synchronized (this) {
            final byte[] dst = new byte[readBuffer.position()];
            readBuffer.position(0);
            readBuffer.get(dst, 0, dst.length);
            if (debugging)
                UsbSerialDebugger.printReadLogGet(dst, true);
            return dst;
        }
    }

    public void clearReadBuffer() {
        synchronized (this) {
            readBuffer.clear();
        }
    }

    public byte[] getWriteBuffer() throws InterruptedException {
        return writeBuffer.get();
    }

    public void putWriteBuffer(final byte[] data) {
        writeBuffer.put(data);
    }


    public byte[] getBufferCompatible() {
        return readBufferCompatible;
    }

    public byte[] getDataReceivedCompatible(final int numberBytes) {
        return Arrays.copyOfRange(readBufferCompatible, 0, numberBytes);
    }

    private class SynchronizedBuffer {
        private final Buffer buffer = new Buffer();

        synchronized void put(final byte[] src) {
            if (src == null || src.length == 0)
                return;

            if (debugging)
                UsbSerialDebugger.printLogPut(src, true);

            buffer.write(src);
            notify();
        }

        synchronized byte[] get() throws InterruptedException {
            while (buffer.size() == 0)
                wait();
            final byte[] dst;
            if (buffer.size() <= MAX_BULK_BUFFER) {
                dst = buffer.readByteArray();
            } else {
                try {
                    dst = buffer.readByteArray(MAX_BULK_BUFFER);
                } catch (final EOFException e) {
                    throw new Error(e);
                }
            }

            if (debugging)
                UsbSerialDebugger.printLogGet(dst, true);

            return dst;
        }
    }
}
