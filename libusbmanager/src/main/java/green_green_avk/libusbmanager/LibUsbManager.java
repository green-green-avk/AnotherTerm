package green_green_avk.libusbmanager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Map;

public final class LibUsbManager {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    private static final class ParseException extends RuntimeException {
        private ParseException(final String message) {
            super(message);
        }
    }

    private static final class ProcessException extends RuntimeException {
        private ProcessException(final String message) {
            super(message);
        }
    }

    private final Context ctx;
    private final Handler uiHandler;
    private final Thread lth;

    private void showError(@NonNull final Throwable e) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, "LibUSB helper: " + e.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    @NonNull
    private UsbManager getUsbManager() {
        final UsbManager usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) throw new ProcessException("Cannot obtain USB service");
        return usbManager;
    }

    private final Object obtainUsbPermissionLock = new Object();

    private void obtainUsbPermission(@NonNull final UsbDevice dev) {
        synchronized (obtainUsbPermissionLock) {
            final Object lock = new Object();
            final UsbManager usbManager = getUsbManager();
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final String action = intent.getAction();
                    if (!ACTION_USB_PERMISSION.equals(action)) return;
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            };
            synchronized (lock) {
                ctx.registerReceiver(receiver, new IntentFilter(ACTION_USB_PERMISSION));
                usbManager.requestPermission(dev, PendingIntent.getBroadcast(
                        ctx, 0, new Intent(ACTION_USB_PERMISSION), 0));
                try {
                    lock.wait();
                } catch (final InterruptedException ignored) {
                } finally {
                    ctx.unregisterReceiver(receiver);
                }
            }
        }
    }

    private static final int DEV_ATTACHED = 0;
    private static final int DEV_DETACHED = 1;
    private static final int DEV_EXISTS = 2;

    private void tryWriteDeviceName(@NonNull final LocalSocket socket,
                                    @Nullable final UsbDevice dev, final int state) {
        try {
            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            if (state != DEV_EXISTS) out.writeByte(state);
            out.writeUTF(dev == null ? "" : dev.getDeviceName());
        } catch (final Throwable e) {
            showError(e);
        }
    }

    private void devListClient(@NonNull final LocalSocket socket) throws IOException {
        final HandlerThread outTh = new HandlerThread("libUsbEventsOutput");
        outTh.start();
        final Handler outH = new Handler(outTh.getLooper());
        outH.post(new Runnable() {
            @Override
            public void run() {
                for (final UsbDevice dev : getUsbManager().getDeviceList().values())
                    tryWriteDeviceName(socket, dev, DEV_EXISTS);
                tryWriteDeviceName(socket, null, DEV_EXISTS);
            }
        });
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case ACTION_USB_ATTACHED:
                        tryWriteDeviceName(socket,
                                (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE),
                                DEV_ATTACHED);
                        break;
                    case ACTION_USB_DETACHED:
                        tryWriteDeviceName(socket,
                                (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE),
                                DEV_DETACHED);
                        break;
                }
            }
        };
        final IntentFilter iflt = new IntentFilter(ACTION_USB_ATTACHED);
        iflt.addAction(ACTION_USB_DETACHED);
        ctx.registerReceiver(receiver, iflt, null, outH);
        try {
            while (socket.getInputStream().read() != -1) ; // Wait for closing by the client...
        } finally {
            ctx.unregisterReceiver(receiver);
            outTh.quit();
        }
    }

    private void client(@NonNull final LocalSocket socket) {
        UsbDeviceConnection devConn = null;
        try {
            if (Process.myUid() != socket.getPeerCredentials().getUid())
                throw new ParseException("Spoofing detected!"); // TODO: Or not to check?
            final InputStream cis = socket.getInputStream();
            final DataInputStream dis = new DataInputStream(cis);
            final String devName = dis.readUTF();
            if (devName.length() <= 0) {
                devListClient(socket);
                return;
            }
            final UsbManager usbManager = getUsbManager();
            final Map<String, UsbDevice> devList = usbManager.getDeviceList();
            final UsbDevice dev = devList.get(devName);
            if (dev == null) throw new ProcessException("No device found: " + devName);
            obtainUsbPermission(dev);
            devConn = usbManager.openDevice(dev);
            if (devConn == null) throw new ProcessException("Unable to open device: " + devName);
            socket.setFileDescriptorsForSend(new FileDescriptor[]{
                    ParcelFileDescriptor.adoptFd(devConn.getFileDescriptor()).getFileDescriptor()
            });
            socket.getOutputStream().write(0);
            while (cis.read() != -1) ; // Wait for closing by the client...
        } catch (final InterruptedIOException ignored) {
        } catch (final SecurityException | IOException |
                ParseException | ProcessException e) {
            showError(e);
        } finally {
            if (devConn != null) devConn.close();
        }
    }

    private final Runnable server = new Runnable() {
        @Override
        public void run() {
            LocalServerSocket serverSocket = null;
            try {
                serverSocket = new LocalServerSocket(ctx.getPackageName() + ".libusb");
                while (!Thread.interrupted()) {
                    final LocalSocket socket = serverSocket.accept();
                    final Thread cth = new Thread() {
                        @Override
                        public void run() {
                            try {
                                client(socket);
                            } finally {
                                try {
                                    socket.close();
                                } catch (final IOException ignored) {
                                }
                            }
                        }
                    };
                    cth.setDaemon(false);
                    cth.start();
                }
            } catch (final InterruptedIOException ignored) {
            } catch (final IOException e) {
                Log.e("LibUsbServer", "IO", e);
            }
            if (serverSocket != null)
                try {
                    serverSocket.close();
                } catch (final IOException ignored) {
                }
        }
    };

    @UiThread
    public LibUsbManager(@NonNull final Context ctx) {
        this.ctx = ctx.getApplicationContext();
        uiHandler = new Handler();
        lth = new Thread(server, "LibUsbServer");
        lth.setDaemon(true);
        lth.start();
    }

    @Override
    protected void finalize() throws Throwable {
        lth.interrupt();
        super.finalize();
    }
}
