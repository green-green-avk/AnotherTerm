package green_green_avk.anotherterm.backends.uart;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.bluetoothspp.BluetoothSPP;
import green_green_avk.bluetoothspp.BluetoothSPPException;

final class BtImpl extends Impl {

    @NonNull
    private static String getDeviceDesc(@NonNull final BluetoothDevice dev) {
        return String.format(Locale.ROOT, "%s %s", dev.getAddress(), dev.getName());
    }

    @NonNull
    static Map<String, Integer> getAdapters() {
        final Map<String, Integer> r = new HashMap<>();
        try {
            for (final BluetoothDevice dev : BluetoothSPP.getDeviceList()) {
                r.put(getDeviceDesc(dev), activeDevices.contains(dev) ?
                        BackendModule.Meta.ADAPTER_ALREADY_IN_USE
                        : BackendModule.Meta.ADAPTER_READY);
            }
        } catch (final BluetoothSPPException ignored) {
        }
        return r;
    }

    private static final Set<BluetoothDevice> activeDevices =
            Collections.synchronizedSet(Collections.newSetFromMap(
                    new WeakHashMap<BluetoothDevice, Boolean>()));
    private static final Object commonLock = new Object();
    private static final Object deviceLock = new Object();

    private volatile BluetoothDevice device = null;
    private final BluetoothSPP spp = new BluetoothSPP();

    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) throws IOException {
            final OutputStream out = spp.getOutput();
            if (out != null && spp.isConnected()) {
                out.write(b);
                out.flush();
            }
        }

        @Override
        public void write(@NonNull final byte[] b, final int off, final int len) throws IOException {
            final OutputStream out = spp.getOutput();
            if (out != null && spp.isConnected()) {
                out.write(b, off, len);
                out.flush();
            }
        }

        @Override
        public void write(@NonNull final byte[] b) throws IOException {
            final OutputStream out = spp.getOutput();
            if (out != null && spp.isConnected()) {
                out.write(b);
                out.flush();
            }
        }
    };

    BtImpl(@NonNull final UartModule base) {
        super(base);
    }

    @NonNull
    @Override
    OutputStream getOutputStream() {
        return input;
    }

    @NonNull
    private BluetoothDevice obtainDevice() throws UartModule.AdapterNotFoundException {
        final Set<BluetoothDevice> list;
        try {
            list = BluetoothSPP.getDeviceList();
        } catch (final BluetoothSPPException e) {
            throw new BackendException(e.getMessage());
        }
        if (!"*".equals(base.adapter)) {
            for (final BluetoothDevice dev : list) {
                if (base.adapter.equals(dev.getAddress())) {
                    if (activeDevices.contains(dev)) throw new BackendException("Device is busy");
                    return dev;
                }
            }
            throw new UartModule.AdapterNotFoundException();
        }
        for (final BluetoothDevice dev : list) {
            if (activeDevices.contains(dev)) continue;
            return dev;
        }
        throw new UartModule.AdapterNotFoundException();
    }

    @Override
    boolean isConnected() {
        return spp.isConnected();
    }

    private final Runnable reader = new Runnable() {
        final byte[] buf = new byte[8192];

        @Override
        public void run() {
            final OutputStream out = base.output;
            final InputStream in = spp.getInput();
            if (out == null || in == null) {
                base.reportError(new BackendException("Streams failure"));
                return;
            }
            while (true) {
                if (!spp.isConnected()) return;
                try {
                    final int r = in.read(buf);
                    if (r < 0) return;
                    out.write(buf, 0, r);
                } catch (final IOException e) {
                    base.reportError(e);
                    return;
                }
            }
        }
    };

    private Thread readerThread = null;

    @Override
    void connect() throws UartModule.AdapterNotFoundException {
        synchronized (commonLock) {
            synchronized (deviceLock) {
                device = obtainDevice();
                activeDevices.add(device);
            }
            try {
                spp.connect(device, base.insecure);
            } catch (final IOException e) {
                activeDevices.remove(device);
                throw new BackendException(e.getMessage());
            }
            readerThread = new Thread(reader);
            readerThread.setDaemon(true);
            readerThread.start();
        }
    }

    @Override
    void disconnect() {
        synchronized (commonLock) {
            if (!spp.isConnected()) return;
            try {
                spp.disconnect();
            } catch (final IOException e) {
                throw new BackendException(e.getMessage());
            }
            try {
                readerThread.join();
            } catch (final InterruptedException ignored) {
            }
            readerThread = null;
            activeDevices.remove(device);
            device = null;
        }
    }

    @Override
    @NonNull
    String getSubDesc() {
        final BluetoothDevice dev = device;
        return dev != null ? getDeviceDesc(dev) : "-";
    }

    @Override
    protected void finalize() throws Throwable {
        if (device != null) activeDevices.remove(device);
        super.finalize();
    }
}
