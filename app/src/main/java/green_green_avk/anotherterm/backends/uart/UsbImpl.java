package green_green_avk.anotherterm.backends.uart;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.utils.BlockingSync;

final class UsbImpl extends Impl {

    @NonNull
    private static UsbManager getUsbManager(@NonNull final Context context) {
        final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) throw new BackendException("Cannot obtain USB service");
        return usbManager;
    }

    @NonNull
    private static String getDeviceDesc(@NonNull final UsbDevice dev) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return String.format(Locale.getDefault(),
                        "%s [%04X], %s [%04X], ver: %s, SN: %s",
                        dev.getManufacturerName(), dev.getVendorId(),
                        dev.getProductName(), dev.getProductId(),
                        dev.getVersion(), dev.getSerialNumber());
            }
            return String.format(Locale.getDefault(),
                    "%s [%04X], %s [%04X], SN: %s",
                    dev.getManufacturerName(), dev.getVendorId(),
                    dev.getProductName(), dev.getProductId(),
                    dev.getSerialNumber());
        }
        return String.format(Locale.getDefault(),
                "%04X:%04X",
                dev.getVendorId(), dev.getProductId());
    }

    @NonNull
    static Map<String, Integer> getAdapters(@NonNull final Context ctx) {
        final UsbManager mgr = getUsbManager(ctx);
        final Map<String, Integer> r = new HashMap<>();
        for (final Map.Entry<String, UsbDevice> ent : mgr.getDeviceList().entrySet()) {
            if (!UsbSerialDevice.isSupported(ent.getValue())) continue;
            r.put(String.format(Locale.ROOT, "%s %s", ent.getKey(),
                    getDeviceDesc(ent.getValue())),
                    activeDevices.contains(ent.getValue()) ?
                            BackendModule.Meta.ADAPTER_ALREADY_IN_USE
                            : BackendModule.Meta.ADAPTER_READY);
        }
        return r;
    }

    private static final Set<UsbDevice> activeDevices =
            Collections.synchronizedSet(Collections.newSetFromMap(
                    new WeakHashMap<UsbDevice, Boolean>()));
    private static final Object deviceLock = new Object();
    private final Object commonLock = new Object();

    private boolean mIsConnected = false;

    private UsbDevice device = null;
    private UsbDeviceConnection connection = null;
    private UsbSerialDevice serialPort = null;

    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) {
            if (mIsConnected) serialPort.write(new byte[]{(byte) b});
        }

        @Override
        public void write(@NonNull final byte[] b, final int off, final int len) {
            if (mIsConnected) serialPort.write(Arrays.copyOfRange(b, off, off + len));
        }

        @Override
        public void write(@NonNull final byte[] b) {
            if (mIsConnected) serialPort.write(b);
        }
    };

    UsbImpl(@NonNull final UartModule base) {
        super(base);
    }

    @NonNull
    @Override
    OutputStream getOutputStream() {
        return input;
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case ACTION_USB_PERMISSION:
                    usbAccessGranted.set(intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false));
                    break;
                case ACTION_USB_ATTACHED: {
                    // reconnect
                    final UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mIsConnected && device.equals(dev)) {
                        base.getUi().showToast(context.getString(
                                R.string.msg_usd_serial_port_s_reconnected,
                                dev.getDeviceName()));
                        final Thread t = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    makeConnection(true);
                                } catch (final BackendException e) {
                                    base.reportError(e);
                                }
                            }
                        };
                        t.start();
                    }
                    break;
                }
                case ACTION_USB_DETACHED:
                    final UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mIsConnected && device.equals(dev)) {
                        base.getUi().showToast(context.getString(
                                R.string.msg_usd_serial_port_s_disconnected,
                                dev.getDeviceName()));
                    }
                    break;
            }
        }
    };

    private final BlockingSync<Boolean> usbAccessGranted = new BlockingSync<>();

    private void obtainDevice() throws UartModule.AdapterNotFoundException {
        final UsbManager usbManager = getUsbManager(base.getContext());
        final Map<String, UsbDevice> devs = usbManager.getDeviceList();
        if (!"*".equals(base.adapter)) {
            final UsbDevice dev = devs.get(base.adapter);
            if (dev == null) throw new UartModule.AdapterNotFoundException();
            if (!UsbSerialDevice.isSupported(dev))
                throw new BackendException("Device is not supported");
            if (activeDevices.contains(dev)) throw new BackendException("Device is busy");
            device = dev;
            return;
        }
        for (final UsbDevice dev : devs.values()) {
            if (!UsbSerialDevice.isSupported(dev)) continue;
            if (activeDevices.contains(dev)) continue;
            device = dev;
            return;
        }
        throw new UartModule.AdapterNotFoundException();
    }

    private void makeConnection(final boolean reconnect) {
        final UsbManager usbManager =
                (UsbManager) base.getContext().getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            disconnect();
            throw new BackendException("Cannot obtain USB service");
        }
        usbAccessGranted.clear();
        usbManager.requestPermission(device, PendingIntent.getBroadcast(base.getContext(),
                0, new Intent(ACTION_USB_PERMISSION), 0));
        try {
            if (!usbAccessGranted.get()) {
                if (reconnect) return;
                disconnect();
                throw new BackendException("Permission denied for device " + device);
            }
        } catch (final InterruptedException e) {
            disconnect();
            throw new BackendException("UI request interrupted");
        }
        connection = usbManager.openDevice(device);
        if (connection == null) {
            disconnect();
            throw new BackendException("Cannot connect to device " + device);
        }
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort == null) {
            disconnect();
            throw new BackendException("Device " + device + " not supported");
        }
        if (!serialPort.open()) {
            disconnect();
            throw new BackendException("Device " + device + " driver error");
        }
        // TODO: com.felhr.usbserial should be extended in order to read current device settings
        if (base.baudrate > 0) serialPort.setBaudRate(base.baudrate);
        if (base.dataBits != UartModule.OPT_PRESERVE) serialPort.setDataBits(base.dataBits);
        if (base.stopBits != UartModule.OPT_PRESERVE) serialPort.setStopBits(base.stopBits);
        if (base.parity != UartModule.OPT_PRESERVE) serialPort.setParity(base.parity);
        if (base.flowControl != UartModule.OPT_PRESERVE)
            serialPort.setFlowControl(base.flowControl);
        serialPort.read(readCallback);
    }

    private UsbSerialInterface.UsbReadCallback readCallback =
            new UsbSerialInterface.UsbReadCallback() {
                @Override
                public void onReceivedData(final byte[] bytes) {
                    if (base.output != null) {
                        try {
                            base.output.write(bytes);
                        } catch (final IOException e) {
                            disconnect();
                            Log.e("USB", "Unexpected frontend problem", e);
                            base.reportError(new BackendException("Frontend is inaccessible"));
                        }
                    }
                }
            };

    @Override
    boolean isConnected() {
        return mIsConnected;
    }

    @Override
    void connect() throws UartModule.AdapterNotFoundException {
        synchronized (commonLock) {
            if (mIsConnected) return;
            synchronized (deviceLock) {
                obtainDevice();
                activeDevices.add(device);
            }
            final IntentFilter iflt = new IntentFilter(ACTION_USB_PERMISSION);
            iflt.addAction(ACTION_USB_ATTACHED);
            iflt.addAction(ACTION_USB_DETACHED);
            base.getContext().registerReceiver(mUsbReceiver, iflt);
            try {
                makeConnection(false);
            } catch (final Throwable e) {
                activeDevices.remove(device);
                throw e;
            }
            mIsConnected = true;
        }
        if (base.isAcquireWakeLockOnConnect()) base.acquireWakeLock();
    }

    @Override
    void disconnect() {
        synchronized (commonLock) {
            if (!mIsConnected) return;
            mIsConnected = false;
            activeDevices.remove(device);
            if (serialPort != null) serialPort.close();
            serialPort = null;
            if (connection != null) connection.close();
            connection = null;
            if (device != null) device = null;
            base.getContext().unregisterReceiver(mUsbReceiver);
        }
    }

    @Override
    @NonNull
    String getSubDesc() {
        final UsbDevice dev = device;
        return dev != null ? getDeviceDesc(dev) : "-";
    }

    @Override
    protected void finalize() throws Throwable {
        if (device != null) activeDevices.remove(device);
        super.finalize();
    }
}
