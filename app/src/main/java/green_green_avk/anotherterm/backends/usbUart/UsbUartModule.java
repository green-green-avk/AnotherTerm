package green_green_avk.anotherterm.backends.usbUart;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.SimpleBiDirHashMap;

public final class UsbUartModule extends BackendModule {

    @Keep
    public static final Meta meta = new Meta(UsbUartModule.class, "uart") {
        @Override
        @NonNull
        public Map<String, ?> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque()) throw new ParametersUriParseException();
            final Map<String, Object> params = new HashMap<>();
            for (final String k : uri.getQueryParameterNames()) {
                // TODO: '+' decoding issue before Jelly Bean
                params.put(k, uri.getQueryParameter(k));
            }
            final List<String> ps = uri.getPathSegments();
            if (ps != null) {
                try {
                    try {
                        params.put("baudrate", Integer.parseInt(ps.get(0)));
                    } catch (final NumberFormatException e) {
                        throw new ParametersUriParseException("Invalid baudrate", e);
                    }
                    params.put("databits", ps.get(1));
                    params.put("stopbits", ps.get(2));
                    params.put("parity", ps.get(3));
                    params.put("flowcontrol", ps.get(4));
                } catch (final IndexOutOfBoundsException ignored) {
                }
            }
            return params;
        }

        @Override
        @NonNull
        public Uri toUri(@NonNull final Map<String, ?> params) {
            final Uri.Builder b = new Uri.Builder()
                    .scheme(getUriSchemes().iterator().next());
            b.appendPath(params.get("baudrate").toString());
            b.appendPath(params.get("databits").toString());
            b.appendPath(params.get("stopbits").toString());
            b.appendPath(params.get("parity").toString());
            b.appendPath(params.get("flowcontrol").toString());
            for (final String k : params.keySet()) {
                switch (k) {
                    case "baudrate":
                    case "databits":
                    case "stopbits":
                    case "parity":
                    case "flowcontrol":
                        break;
                    default:
                        b.appendQueryParameter(k, params.get(k).toString());
                }
            }
            return b.build();
        }
    };

//    private enum DataBits {_8, _9}

//    private enum StopBits {_1, _1_5, _2}

//    private enum Parity {OFF, POSITIVE, NEGATIVE}

//    private enum FlowControl {OFF, XON_XOFF, RTS_CTS}

    private static final SimpleBiDirHashMap<String, Integer> dataBitsOpts
            = new SimpleBiDirHashMap<>();
    private static final SimpleBiDirHashMap<String, Integer> stopBitsOpts
            = new SimpleBiDirHashMap<>();
    private static final SimpleBiDirHashMap<String, Integer> parityOpts
            = new SimpleBiDirHashMap<>();
    private static final SimpleBiDirHashMap<String, Integer> flowControlOpts
            = new SimpleBiDirHashMap<>();

    static {
        dataBitsOpts.put("8", UsbSerialInterface.DATA_BITS_8);
        dataBitsOpts.put("7", UsbSerialInterface.DATA_BITS_7);
        dataBitsOpts.put("6", UsbSerialInterface.DATA_BITS_6);
        dataBitsOpts.put("5", UsbSerialInterface.DATA_BITS_5);
        stopBitsOpts.put("1", UsbSerialInterface.STOP_BITS_1);
        stopBitsOpts.put("1.5", UsbSerialInterface.STOP_BITS_15);
        stopBitsOpts.put("2", UsbSerialInterface.STOP_BITS_2);
        parityOpts.put("none", UsbSerialInterface.PARITY_NONE);
        parityOpts.put("even", UsbSerialInterface.PARITY_EVEN);
        parityOpts.put("odd", UsbSerialInterface.PARITY_ODD);
        parityOpts.put("mark", UsbSerialInterface.PARITY_MARK);
        parityOpts.put("space", UsbSerialInterface.PARITY_SPACE);
        flowControlOpts.put("off", UsbSerialInterface.FLOW_CONTROL_OFF);
        flowControlOpts.put("xon_xoff", UsbSerialInterface.FLOW_CONTROL_XON_XOFF);
        flowControlOpts.put("rts_cts", UsbSerialInterface.FLOW_CONTROL_RTS_CTS);
        flowControlOpts.put("dsr_dtr", UsbSerialInterface.FLOW_CONTROL_DSR_DTR);
    }

    private int baudrate = 9600;
    private int dataBits = UsbSerialInterface.DATA_BITS_8;
    private int stopBits = UsbSerialInterface.STOP_BITS_1;
    private int parity = UsbSerialInterface.PARITY_NONE;
    private int flowControl = UsbSerialInterface.FLOW_CONTROL_OFF;

    private static final Set<UsbDevice> activeDevices = new HashSet<>();

    private boolean mIsConnected = false;

    private UsbDevice device = null;
    private UsbDeviceConnection connection = null;
    private UsbSerialDevice serialPort = null;

    private OutputStream out;
    private OutputStream in = new OutputStream() {
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

    private OnMessageListener onMessageListener = null;

    private void reportError(@NonNull final Throwable e) {
        if (onMessageListener != null) onMessageListener.onMessage(e);
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
                    usbAccessGranted.set(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
                    break;
                case ACTION_USB_ATTACHED: {
                    // reconnect
                    final UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mIsConnected && device.equals(dev)) {
                        getUi().showToast(context.getString(
                                R.string.msg_usd_serial_port_s_reconnected,
                                dev.getDeviceName()));
                        final Thread t = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    makeConnection(true);
                                } catch (final BackendException e) {
                                    reportError(e);
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
                        getUi().showToast(context.getString(
                                R.string.msg_usd_serial_port_s_disconnected,
                                dev.getDeviceName()));
                    }
                    break;
            }
        }
    };

    private final BlockingSync<Boolean> usbAccessGranted = new BlockingSync<>();

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        baudrate = pp.getInt("baudrate", baudrate);
        dataBits = pp.getFromMap("databits", dataBitsOpts, dataBits);
        stopBits = pp.getFromMap("stopbits", stopBitsOpts, stopBits);
        parity = pp.getFromMap("parity", parityOpts, parity);
        flowControl = pp.getFromMap("flowcontrol", flowControlOpts, flowControl);
    }

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        out = stream;
    }

    @NonNull
    @Override
    public OutputStream getOutputStream() {
        return in;
    }

    @Override
    public void setOnMessageListener(final OnMessageListener l) {
    }

    @Override
    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public void connect() {
        if (mIsConnected) return;
        obtainDevice();
        final IntentFilter iflt = new IntentFilter(ACTION_USB_PERMISSION);
        iflt.addAction(ACTION_USB_ATTACHED);
        iflt.addAction(ACTION_USB_DETACHED);
        context.registerReceiver(mUsbReceiver, iflt);
        makeConnection(false);
        activeDevices.add(device);
        mIsConnected = true;
    }

    @Override
    public void disconnect() {
        if (!mIsConnected) return;
        mIsConnected = false;
        activeDevices.remove(device);
        if (serialPort != null) serialPort.close();
        serialPort = null;
        if (connection != null) connection.close();
        connection = null;
        if (device != null) device = null;
        context.unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void finalize() throws Throwable {
        if (device != null) activeDevices.remove(device);
        super.finalize();
    }

    @Override
    public void resize(final int col, final int row, final int wp, final int hp) {
    }

    @NonNull
    @Override
    public String getConnDesc() {
        final Map<String, Object> pp = new HashMap<>();
        pp.put("baudrate", baudrate);
        pp.put("databits", dataBitsOpts.rev.get(dataBits));
        pp.put("stopbits", stopBitsOpts.rev.get(stopBits));
        pp.put("parity", parityOpts.rev.get(parity));
        pp.put("flowcontrol", flowControlOpts.rev.get(flowControl));
        return meta.toUri(pp).toString();
    }

    private void obtainDevice() {
        final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) throw new BackendException("Cannot obtain USB service");
        final Map<String, UsbDevice> devs = usbManager.getDeviceList();
        for (final UsbDevice dev : devs.values()) {
            if (activeDevices.contains(dev)) continue;
            if (!UsbSerialDevice.isSupported(dev)) continue;
            device = dev;
            return;
        }
        throw new BackendException("No supported USB serial ports found");
    }

    private void makeConnection(final boolean reconnect) {
        final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            disconnect();
            throw new BackendException("Cannot obtain USB service");
        }
        usbAccessGranted.clear();
        usbManager.requestPermission(device, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
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
            throw new BackendException("Device " + device + "not supported");
        }
        if (!serialPort.open()) {
            disconnect();
            throw new BackendException("Device " + device + "driver error");
        }
        serialPort.setBaudRate(baudrate);
        serialPort.setDataBits(dataBits);
        serialPort.setStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setFlowControl(flowControl);
        serialPort.read(readCallback);
    }

    private UsbSerialInterface.UsbReadCallback readCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(final byte[] bytes) {
            if (out != null) {
                try {
                    out.write(bytes);
                } catch (final IOException e) {
                    disconnect();
                    Log.e("USB", "Unexpected frontend problem", e);
                    reportError(new BackendException("Frontend is inaccessible"));
                }
            }
        }
    };
}
