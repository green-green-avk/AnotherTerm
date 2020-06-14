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

    @Nullable
    private static UUID getPreferredUUID(@Nullable final BluetoothDevice dev) {
        if (dev == null) return null;
        boolean hasSPP = false;
        boolean hasAndroid = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            final ParcelUuid[] uuids = dev.getUuids();
            if (uuids == null) return null;
            for (final ParcelUuid uuid : uuids) {
                if (UUID_SPP.equals(uuid.getUuid())) hasSPP = true;
                if (UUID_ANDROID.equals(uuid.getUuid())) hasAndroid = true;
            }
            return hasSPP ? UUID_SPP : hasAndroid ? UUID_ANDROID : null;
        }
        return UUID_SPP; // :(
    }

    private static boolean isSupportedDevice(@NonNull final BluetoothDevice dev) {
        return getPreferredUUID(dev) != null;
    }

    public static Set<BluetoothDevice> getDeviceList() throws BluetoothSPPException {
        final BluetoothAdapter a = getAdapter();
        final Set<BluetoothDevice> devs = a.getBondedDevices();
        final Set<BluetoothDevice> r = new HashSet<>();
        for (final BluetoothDevice dev : devs) if (isSupportedDevice(dev)) r.add(dev);
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

    @Nullable
    public BluetoothDevice getDevice() {
        final BluetoothSocket sock = socket;
        if (sock == null) return null;
        return sock.getRemoteDevice();
    }

    public boolean isConnected() {
        final BluetoothSocket sock = socket;
        return sock != null && sock.isConnected();
    }

    public void listen(final boolean insecure, final boolean asAndroid) throws IOException {
        final BluetoothAdapter adapter = getAdapter();
        final UUID uuid = asAndroid ? UUID_ANDROID : UUID_SPP;
        final BluetoothServerSocket bss;
        if (insecure)
            bss = adapter.listenUsingInsecureRfcommWithServiceRecord(NAME_SECURE, uuid);
        else
            bss = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, uuid);
        final BluetoothSocket sock = bss.accept();
        sock.connect();
        socket = sock;
        input = sock.getInputStream();
        output = sock.getOutputStream();
    }

    public void connect(@NonNull final BluetoothDevice dev, final boolean insecure)
            throws IOException {
        final UUID uuid = getPreferredUUID(dev);
        final BluetoothSocket sock;
        if (insecure)
            sock = dev.createInsecureRfcommSocketToServiceRecord(uuid);
        else
            sock = dev.createRfcommSocketToServiceRecord(uuid);
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
