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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

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
import green_green_avk.anotherterm.utils.Misc;

final class UsbImpl extends Impl {

    @NonNull
    private static UsbManager getUsbManager(@NonNull final Context context) {
        final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) throw new BackendException("Cannot obtain USB service");
        return usbManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private static String getSerialNumber(@NonNull final UsbDevice dev) {
        try {
            return dev.getSerialNumber();
        } catch (final SecurityException e) {
            // Hidden if no read permission given (Android 10 and targetSdkVersion 29)
            return "*";
        }
    }

    @NonNull
    private static String getDeviceDesc(@NonNull final UsbDevice dev) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return String.format(Locale.getDefault(),
                        "%s [%04X], %s [%04X], ver: %s, SN: %s",
                        dev.getManufacturerName(), dev.getVendorId(),
                        dev.getProductName(), dev.getProductId(),
                        dev.getVersion(), getSerialNumber(dev));
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
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Object deviceLock = new Object();
    private final Object commonLock = new Object();

    private volatile boolean mIsConnected = false;
    private volatile boolean mIsConnecting = false;
    private volatile boolean mIsConnInt = false;

    private volatile UsbDevice device = null;
    private volatile UsbDeviceConnection connection = null;
    private volatile UsbSerialDevice serialPort = null;

    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) {
            final UsbSerialDevice p = serialPort;
            if (mIsConnected && p != null)
                p.write(new byte[]{(byte) b});
        }

        @Override
        public void write(@NonNull final byte[] b, final int off, final int len) {
            final UsbSerialDevice p = serialPort;
            if (mIsConnected && p != null)
                p.write(Arrays.copyOfRange(b, off, off + len));
        }

        @Override
        public void write(@NonNull final byte[] b) {
            final UsbSerialDevice p = serialPort;
            if (mIsConnected && p != null)
                p.write(b);
        }
    };

    UsbImpl(@NonNull final UartModule base) {
        super(base);
    }

    @Override
    @NonNull
    OutputStream getOutputStream() {
        return input;
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, @NonNull final Intent intent) {
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
                        device = dev;
                        Misc.runOnThread(() -> {
                            try {
                                synchronized (commonLock) {
                                    makeConnection(true);
                                }
                            } catch (final BackendException e) {
                                base.reportError(e);
                            }
                        });
                    }
                    break;
                }
                case ACTION_USB_DETACHED:
                    final UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mIsConnected && device.equals(dev)) {
                        Misc.runOnThread(() -> {
                            tmpDisconnect();
                            base.getUi().showToast(context.getString(
                                    R.string.msg_usd_serial_port_s_disconnected,
                                    dev.getDeviceName()));
                        });
                    }
                    break;
            }
        }
    };

    private final BlockingSync<Object> usbAccessGranted = new BlockingSync<>();

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
        try {
            final UsbManager usbManager =
                    (UsbManager) base.getContext().getSystemService(Context.USB_SERVICE);
            if (usbManager == null)
                throw new BackendException("Cannot obtain USB service");
            usbAccessGranted.clear();
            if (mIsConnInt)
                return; // Interrupted by disconnect
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    usbManager.requestPermission(device,
                            PendingIntent.getBroadcast(base.getContext(), 0,
                                    new Intent(ACTION_USB_PERMISSION), 0));
                } catch (final Exception e) {
                    usbAccessGranted.set(e);
                }
            });
            try {
                final Object usbAccessStatus = usbAccessGranted.get();
                if (Boolean.FALSE.equals(usbAccessStatus)) {
                    if (mIsConnInt)
                        return; // Interrupted by disconnect
                    if (reconnect) {
                        base.getUi().showToast(base.getContext().getString(
                                R.string.msg_usd_serial_port_s_reconnected_no_perm,
                                device.getDeviceName()));
                        return;
                    }
                    throw new BackendException("Permission denied for device " + device);
                } else if (usbAccessStatus instanceof Throwable) {
                    throw new BackendException("Permission denied for device " + device +
                            " due to " + ((Throwable) usbAccessStatus).getLocalizedMessage());
                }
            } catch (final InterruptedException e) {
                throw new BackendException("UI request interrupted");
            }
            connection = usbManager.openDevice(device);
            if (connection == null)
                throw new BackendException("Cannot connect to device " + device);
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort == null)
                throw new BackendException("Device " + device + " is not supported");
            if (!serialPort.open())
                throw new BackendException("Device " + device + " driver error");
            // TODO: com.felhr.usbserial should be extended in order to read current device settings
            if (base.baudrate > 0) serialPort.setBaudRate(base.baudrate);
            if (base.dataBits != UartModule.OPT_PRESERVE) serialPort.setDataBits(base.dataBits);
            if (base.stopBits != UartModule.OPT_PRESERVE) serialPort.setStopBits(base.stopBits);
            if (base.parity != UartModule.OPT_PRESERVE) serialPort.setParity(base.parity);
            if (base.flowControl != UartModule.OPT_PRESERVE)
                serialPort.setFlowControl(base.flowControl);
            serialPort.read(readCallback);
        } catch (final Throwable e) {
            disconnect();
            throw e;
        }
    }

    private final UsbSerialInterface.UsbReadCallback readCallback =
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
            if (mIsConnecting) return;
            mIsConnecting = true;
            try {
                synchronized (deviceLock) {
                    obtainDevice();
                    activeDevices.add(device);
                }
            } catch (final Throwable e) {
                mIsConnecting = false;
                throw e;
            }
            try {
                final IntentFilter iflt = new IntentFilter(ACTION_USB_PERMISSION);
                iflt.addAction(ACTION_USB_ATTACHED);
                iflt.addAction(ACTION_USB_DETACHED);
                base.getContext().registerReceiver(mUsbReceiver, iflt);
            } catch (final Throwable e) {
                disconnect();
                throw e;
            }
            makeConnection(false);
            mIsConnected = true;
        }
        if (base.isAcquireWakeLockOnConnect()) base.acquireWakeLock();
    }

    private void tmpDisconnect() {
        synchronized (commonLock) {
            if (serialPort != null) serialPort.close();
            serialPort = null;
            if (connection != null) connection.close();
            connection = null;
        }
    }

    @Override
    void disconnect() {
        if (!mIsConnecting) return;
        mIsConnInt = true;
        usbAccessGranted.set(false);
        synchronized (commonLock) {
            mIsConnInt = false;
            mIsConnected = false;
            activeDevices.remove(device);
            tmpDisconnect();
            if (device != null) device = null;
            try {
                base.getContext().unregisterReceiver(mUsbReceiver);
            } catch (final IllegalArgumentException ignored) {
            }
            mIsConnecting = false;
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
        disconnect();
        super.finalize();
    }
}
