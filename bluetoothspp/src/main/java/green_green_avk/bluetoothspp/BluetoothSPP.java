package green_green_avk.bluetoothspp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BluetoothSPP {

    private static final String NAME_SECURE = "Bluetooth Secure";

    private static final UUID UUID_ANDROID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID UUID_SPP =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * @param context a context
     * @return availability of hardware required by this module
     */
    public static boolean isAvailable(@NonNull final Context context) {
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    private static final Set<String> requiredPermissions31 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    Collections.singleton(Manifest.permission.BLUETOOTH_CONNECT) :
                    Collections.emptySet();

    /**
     * @param targetSDK the application target SDK
     * @return an immutable set of permissions required by this module
     */
    @NonNull
    public static Set<String> getRequiredPermissions(final int targetSDK) {
        return targetSDK >= Build.VERSION_CODES.S ?
                requiredPermissions31 :
                Collections.emptySet();
    }

    @SuppressLint("MissingPermission")
    @NonNull
    private static BluetoothAdapter getAdapter()
            throws BluetoothSPPException {
        final BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        if (a == null || !a.isEnabled() ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && a.getAddress() == null))
            throw new BluetoothSPPException(BluetoothSPPException.Reason.INACCESSIBLE);
        return a;
    }

    @Nullable
    private static UUID getPreferredUUID(@Nullable final BluetoothDevice dev)
            throws SecurityException {
        if (dev == null)
            return null;
        boolean hasSPP = false;
        boolean hasAndroid = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            final ParcelUuid[] uuids = dev.getUuids();
            if (uuids == null)
                return null;
            for (final ParcelUuid uuid : uuids) {
                if (UUID_SPP.equals(uuid.getUuid()))
                    hasSPP = true;
                if (UUID_ANDROID.equals(uuid.getUuid()))
                    hasAndroid = true;
            }
            return hasSPP ? UUID_SPP : hasAndroid ? UUID_ANDROID : null;
        }
        return UUID_SPP; // :(
    }

    private static boolean isSupportedDevice(@NonNull final BluetoothDevice dev) {
        return getPreferredUUID(dev) != null;
    }

    @NonNull
    public static Set<BluetoothDevice> getDeviceList()
            throws BluetoothSPPException {
        try {
            final BluetoothAdapter a = getAdapter();
            final Set<BluetoothDevice> devs = a.getBondedDevices();
            final Set<BluetoothDevice> r = new HashSet<>();
            for (final BluetoothDevice dev : devs)
                if (isSupportedDevice(dev))
                    r.add(dev);
            return r;
        } catch (final SecurityException e) {
            throw new BluetoothSPPException(BluetoothSPPException.Reason.SECURITY, e);
        }
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
        if (sock == null)
            return null;
        return sock.getRemoteDevice();
    }

    public boolean isConnected() {
        final BluetoothSocket sock = socket;
        return sock != null && sock.isConnected();
    }

    public void listen(final boolean insecure, final boolean asAndroid)
            throws IOException {
        try {
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
        } catch (final SecurityException e) {
            throw new BluetoothSPPException(BluetoothSPPException.Reason.SECURITY, e);
        }
    }

    public void connect(@NonNull final BluetoothDevice dev, final boolean insecure)
            throws IOException {
        try {
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
        } catch (final SecurityException e) {
            throw new BluetoothSPPException(BluetoothSPPException.Reason.SECURITY, e);
        }
    }

    public void disconnect()
            throws IOException {
        output = null;
        input = null;
        final BluetoothSocket sock = socket;
        socket = null;
        if (sock == null)
            return;
        sock.close();
    }
}
