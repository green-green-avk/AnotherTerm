package green_green_avk.bluetoothspp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BluetoothSPP {

    private static final String NAME_SECURE = "Bluetooth Secure";

    private static final UUID UUID_ANDROID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID UUID_SPP =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @NonNull
    private static BluetoothAdapter getAdapter() throws BluetoothSPPException {
        final BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        if (a == null || a.getAddress() == null)
            throw new BluetoothSPPException("Bluetooth is inaccessible");
        return a;
    }

    private static boolean isOurDevice(@NonNull final BluetoothDevice dev) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            for (final ParcelUuid uuid : dev.getUuids())
                if (UUID_SPP.equals(uuid.getUuid()))
                    return true;
            return false;
        } else {
            return true; // :(
        }
    }

    public static Set<BluetoothDevice> getDeviceList() throws BluetoothSPPException {
        final BluetoothAdapter a = getAdapter();
        final Set<BluetoothDevice> devs = a.getBondedDevices();
        final Set<BluetoothDevice> r = new HashSet<>();
        for (final BluetoothDevice dev : devs) if (isOurDevice(dev)) r.add(dev);
        return r;
    }

    @Nullable
    private volatile BluetoothSocket socket = null;
    @Nullable
    private volatile InputStream input = null;
    @Nullable
    private volatile OutputStream output = null;

    @Nullable
    public InputStream getInput() {
        return input;
    }

    @Nullable
    public OutputStream getOutput() {
        return output;
    }

    public boolean isConnected() {
        final BluetoothSocket sock = socket;
        return sock != null && sock.isConnected();
    }

    public void listen(final boolean insecure) throws IOException {
        final BluetoothAdapter adapter = getAdapter();
        final BluetoothServerSocket bss;
        if (insecure)
            bss = adapter.listenUsingInsecureRfcommWithServiceRecord(NAME_SECURE, UUID_SPP);
        else
            bss = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, UUID_SPP);
        final BluetoothSocket sock = bss.accept();
        sock.connect();
        socket = sock;
        input = sock.getInputStream();
        output = sock.getOutputStream();
    }

    public void connect(@NonNull final BluetoothDevice dev, final boolean insecure)
            throws IOException {
        final BluetoothSocket sock;
        if (insecure)
            sock = dev.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
        else
            sock = dev.createRfcommSocketToServiceRecord(UUID_SPP);
        sock.connect();
        socket = sock;
        input = sock.getInputStream();
        output = sock.getOutputStream();
    }

    public void disconnect() throws IOException {
        output = null;
        input = null;
        final BluetoothSocket sock = socket;
        socket = null;
        if (sock == null) return;
        sock.close();
    }
}
