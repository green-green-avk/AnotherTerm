package com.felhr.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import com.felhr.deviceids.CP2130Ids;

public abstract class UsbSpiDevice implements UsbSpiInterface {

    protected static final int USB_TIMEOUT = 5000;

    protected final UsbDevice device;
    protected final UsbDeviceConnection connection;

    protected SerialBuffer serialBuffer;

    protected WriteThread writeThread;
    protected ReadThread readThread;

    // Endpoints for synchronous read and write operations
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    public UsbSpiDevice(final UsbDevice device, final UsbDeviceConnection connection) {
        this.device = device;
        this.connection = connection;
        this.serialBuffer = new SerialBuffer(false);
    }

    public static UsbSpiDevice createUsbSerialDevice(final UsbDevice device,
                                                     final UsbDeviceConnection connection) {
        return createUsbSerialDevice(device, connection, -1);
    }

    public static UsbSpiDevice createUsbSerialDevice(final UsbDevice device,
                                                     final UsbDeviceConnection connection,
                                                     final int iface) {
        final int vid = device.getVendorId();
        final int pid = device.getProductId();

        if (CP2130Ids.isDeviceSupported(vid, pid))
            return new CP2130SpiDevice(device, connection, iface);
        else
            return null;
    }


    @Override
    public abstract boolean connectSPI();

    @Override
    public abstract void writeMOSI(byte[] buffer);

    @Override
    public abstract void readMISO(int lengthBuffer);

    @Override
    public abstract void writeRead(byte[] buffer, int lengthRead);

    @Override
    public abstract void setClock(int clockDivider);

    @Override
    public abstract void selectSlave(int nSlave);

    @Override
    public void setMISOCallback(final UsbMISOCallback misoCallback) {
        readThread.setCallback(misoCallback);
    }

    @Override
    public abstract int getClockDivider();

    @Override
    public abstract int getSelectedSlave();

    @Override
    public abstract void closeSPI();

    protected class WriteThread extends AbstractWorkerThread {
        private UsbEndpoint outEndpoint;

        @Override
        public void doRun() {
            final byte[] data = serialBuffer.getWriteBuffer();
            if (data.length > 0)
                connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
        }

        public void setUsbEndpoint(final UsbEndpoint outEndpoint) {
            this.outEndpoint = outEndpoint;
        }
    }

    protected class ReadThread extends AbstractWorkerThread {
        private UsbMISOCallback misoCallback;
        private UsbEndpoint inEndpoint;

        public void setCallback(final UsbMISOCallback misoCallback) {
            this.misoCallback = misoCallback;
        }

        @Override
        public void doRun() {
            final int numberBytes;
            if (inEndpoint != null) {
                numberBytes = connection.bulkTransfer(inEndpoint, serialBuffer.getBufferCompatible(),
                        SerialBuffer.DEFAULT_READ_BUFFER_SIZE, 0);
            } else {
                numberBytes = 0;
            }

            if (numberBytes > 0) {
                onReceivedData(serialBuffer.getDataReceivedCompatible(numberBytes));
            }
        }

        public void setUsbEndpoint(final UsbEndpoint inEndpoint) {
            this.inEndpoint = inEndpoint;
        }

        private void onReceivedData(final byte[] data) {
            if (misoCallback != null)
                misoCallback.onReceivedData(data);
        }
    }

    protected void setThreadsParams(final UsbEndpoint inEndpoint, final UsbEndpoint outEndpoint) {
        if (writeThread != null)
            writeThread.setUsbEndpoint(outEndpoint);

        if (readThread != null)
            readThread.setUsbEndpoint(inEndpoint);
    }

    /*
     * Kill workingThread; This must be called when closing a device
     */
    protected void killWorkingThread() {
        if (readThread != null) {
            readThread.stopThread();
            readThread = null;
        }
    }

    /*
     * Restart workingThread if it has been killed before
     */
    protected void restartWorkingThread() {
        readThread = new ReadThread();
        readThread.start();
        while (!readThread.isAlive()) {
        } // Busy waiting
    }

    protected void killWriteThread() {
        if (writeThread != null) {
            writeThread.stopThread();
            writeThread = null;
        }
    }

    protected void restartWriteThread() {
        if (writeThread == null) {
            writeThread = new WriteThread();
            writeThread.start();
            while (!writeThread.isAlive()) {
            } // Busy waiting
        }
    }
}
