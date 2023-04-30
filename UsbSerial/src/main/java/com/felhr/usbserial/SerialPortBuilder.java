package com.felhr.usbserial;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.felhr.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public final class SerialPortBuilder {
    private static final String ACTION_USB_PERMISSION = "com.felhr.usbserial.USB_PERMISSION";
    private static final int MODE_START = 0;
    private static final int MODE_OPEN = 1;

    @Nullable
    private static SerialPortBuilder serialPortBuilder;

    private List<UsbDeviceStatus> devices;
    private List<UsbSerialDevice> serialDevices = new ArrayList<>();

    private final ArrayBlockingQueue<PendingUsbPermission> queuedPermissions =
            new ArrayBlockingQueue<>(100);
    private volatile boolean processingPermission = false;
    private PendingUsbPermission currentPendingPermission;

    private UsbManager usbManager;
    @NonNull
    private final SerialPortCallback serialPortCallback;

    private int baudRate, dataBits, stopBits, parity, flowControl;
    private int mode = 0;

    private boolean broadcastRegistered = false;

    private SerialPortBuilder(@NonNull final SerialPortCallback serialPortCallback) {
        this.serialPortCallback = serialPortCallback;
    }

    public static void configureInstance(@NonNull final SerialPortCallback serialPortCallback) {
        if (serialPortBuilder != null) {
            throw new IllegalStateException("The instance is already configured");
        }
        serialPortBuilder = new SerialPortBuilder(serialPortCallback);
    }

    @NonNull
    public static SerialPortBuilder getInstance() {
        if (serialPortBuilder == null) {
            throw new IllegalStateException("The instance is not configured yet");
        }
        return serialPortBuilder;
    }

    public List<UsbDevice> getPossibleSerialPorts(final Context context) {

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        return Stream.of(usbManager.getDeviceList().values())
                .filter(UsbSerialDevice::isSupported)
                .toList();
    }

    public boolean getSerialPorts(final Context context) {

        initReceiver(context);

        if (devices == null || devices.isEmpty()) { // No previous devices detected
            devices = Stream.of(getPossibleSerialPorts(context))
                    .map(UsbDeviceStatus::new)
                    .toList();

            if (devices.isEmpty())
                return false;

            for (final UsbDeviceStatus deviceStatus : devices) {
                queuedPermissions.add(createUsbPermission(context, deviceStatus));
            }

        } else { // Previous devices detected and maybe pending permissions intent launched

            final List<UsbDeviceStatus> newDevices = Stream.of(getPossibleSerialPorts(context))
                    .map(UsbDeviceStatus::new)
                    .filter(p -> !devices.contains(p))
                    .toList();

            if (newDevices.isEmpty())
                return false;

            for (final UsbDeviceStatus deviceStatus : newDevices) {
                queuedPermissions.add(createUsbPermission(context, deviceStatus));
            }

            devices.addAll(newDevices);
        }

        if (!processingPermission) {
            launchPermission();
        }

        return true;
    }

    public boolean openSerialPorts(final Context context, final int baudRate, final int dataBits,
                                   final int stopBits, final int parity, final int flowControl) {
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowControl = flowControl;
        this.mode = MODE_OPEN;
        return getSerialPorts(context);
    }

    public boolean disconnectDevice(final UsbSerialDevice usbSerialDevice) {
        usbSerialDevice.syncClose();
        serialDevices = Utils.removeIf(serialDevices, p -> usbSerialDevice.getDeviceId() == p.getDeviceId());
        return true;
    }

    public boolean disconnectDevice(final UsbDevice usbDevice) {
        final List<UsbSerialDevice> devices = Stream.of(serialDevices)
                .filter(p -> usbDevice.getDeviceId() == p.getDeviceId())
                .toList();

        int removedDevices = 0;
        for (final UsbSerialDevice device : devices) {
            device.syncClose();
            serialDevices = Utils.removeIf(serialDevices, p -> usbDevice.getDeviceId() == p.getDeviceId());
            removedDevices++;
        }

        return removedDevices == devices.size();
    }

    public void unregisterListeners(final Context context) {
        if (broadcastRegistered) {
            context.unregisterReceiver(usbReceiver);
            broadcastRegistered = false;
        }
    }

    private PendingUsbPermission createUsbPermission(final Context context,
                                                     final UsbDeviceStatus usbDeviceStatus) {
        final PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
        final PendingUsbPermission pendingUsbPermission = new PendingUsbPermission();
        pendingUsbPermission.pendingIntent = mPendingIntent;
        pendingUsbPermission.usbDeviceStatus = usbDeviceStatus;
        return pendingUsbPermission;
    }


    private void launchPermission() {
        try {
            processingPermission = true;
            currentPendingPermission = queuedPermissions.take();
            usbManager.requestPermission(currentPendingPermission.usbDeviceStatus.usbDevice,
                    currentPendingPermission.pendingIntent);
        } catch (final InterruptedException e) {
            e.printStackTrace();
            processingPermission = false;
        }
    }

    private void initReceiver(final Context context) {
        if (!broadcastRegistered) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            context.registerReceiver(usbReceiver, filter);
            broadcastRegistered = true;
        }
    }

    private void createAllPorts(final UsbDeviceStatus usbDeviceStatus) {
        final int interfaceCount = usbDeviceStatus.usbDevice.getInterfaceCount();
        for (int i = 0; i <= interfaceCount - 1; i++) {
            if (usbDeviceStatus.usbDeviceConnection == null) {
                usbDeviceStatus.usbDeviceConnection =
                        usbManager.openDevice(usbDeviceStatus.usbDevice);
            }

            final UsbSerialDevice usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(
                    usbDeviceStatus.usbDevice,
                    usbDeviceStatus.usbDeviceConnection,
                    i);

            serialDevices.add(usbSerialDevice);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                final boolean granted =
                        intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                final InitSerialPortThread initSerialPortThread;
                if (granted) {
                    createAllPorts(currentPendingPermission.usbDeviceStatus);
                }
                if (!queuedPermissions.isEmpty()) {
                    launchPermission();
                } else {
                    processingPermission = false;
                    if (mode == MODE_START) {
                        serialPortCallback.onSerialPortsDetected(serialDevices);
                    } else {
                        initSerialPortThread = new InitSerialPortThread(serialDevices);
                        initSerialPortThread.start();
                    }
                }
            }
        }
    };

    private class InitSerialPortThread extends Thread {

        private final List<? extends UsbSerialDevice> usbSerialDevices;

        public InitSerialPortThread(final List<? extends UsbSerialDevice> usbSerialDevices) {
            this.usbSerialDevices = usbSerialDevices;
        }

        @Override
        public void run() {
            int n = 1;
            for (final UsbSerialDevice usbSerialDevice : usbSerialDevices) {
                if (!usbSerialDevice.isOpen) {
                    if (usbSerialDevice.syncOpen()) {
                        usbSerialDevice.setBaudRate(baudRate);
                        usbSerialDevice.setDataBits(dataBits);
                        usbSerialDevice.setStopBits(stopBits);
                        usbSerialDevice.setParity(parity);
                        usbSerialDevice.setFlowControl(flowControl);
                        usbSerialDevice.setPortName(UsbSerialDevice.COM_PORT + n);
                        n++;
                    }
                }
            }
            serialPortCallback.onSerialPortsDetected(serialDevices);
        }
    }

    private static class UsbDeviceStatus {
        public UsbDevice usbDevice;
        public UsbDeviceConnection usbDeviceConnection;
        public boolean open;

        public UsbDeviceStatus(final UsbDevice usbDevice) {
            this.usbDevice = usbDevice;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof UsbDeviceStatus))
                return false;
            return ((UsbDeviceStatus) obj).usbDevice.getDeviceId() == usbDevice.getDeviceId();
        }
    }

    private static class PendingUsbPermission {
        public PendingIntent pendingIntent;
        public UsbDeviceStatus usbDeviceStatus;
    }
}
