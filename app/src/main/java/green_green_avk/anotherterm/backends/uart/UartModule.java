package green_green_avk.anotherterm.backends.uart;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.utils.LogMessage;
import green_green_avk.anotherterm.utils.ResultException;
import green_green_avk.anotherterm.utils.SimpleBiDirHashMap;

public final class UartModule extends BackendModule {

    @Keep
    public static final Meta meta = new Meta(UartModule.class, "uart") {
        private final Collection<Requirement> btRequirements = new ArrayList<>();

        {
            final Set<String> perms = BtImpl.getPermissions();
            if (!perms.isEmpty())
                btRequirements.add(new Requirement.Permissions(
                        R.drawable.ic_bluetooth,
                        R.string.label_req_uart_bt_perms,
                        perms
                ));
        }

        @Override
        @NonNull
        public Collection<Requirement> getRequirements(@NonNull final Context ctx) {
            return BtImpl.isAvailable(ctx) ? btRequirements : Collections.emptySet();
        }

        @Override
        @NonNull
        public Map<String, Integer> getAdapters(@NonNull final Context ctx) {
            final Map<String, Integer> usbList = UsbImpl.getAdapters(ctx);
            final Map<String, Integer> btList = BtImpl.getAdapters(ctx);
            final Map<String, Integer> r = new HashMap<>();
            r.putAll(usbList);
            r.putAll(btList);
            return r;
        }

        @Override
        @NonNull
        public Map<String, Object> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque())
                throw new ParametersUriParseException();
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

    static final int OPT_PRESERVE = -1;

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
        dataBitsOpts.put("-", OPT_PRESERVE);
        dataBitsOpts.put("8", UsbSerialInterface.DATA_BITS_8);
        dataBitsOpts.put("7", UsbSerialInterface.DATA_BITS_7);
        dataBitsOpts.put("6", UsbSerialInterface.DATA_BITS_6);
        dataBitsOpts.put("5", UsbSerialInterface.DATA_BITS_5);
        stopBitsOpts.put("-", OPT_PRESERVE);
        stopBitsOpts.put("1", UsbSerialInterface.STOP_BITS_1);
        stopBitsOpts.put("1.5", UsbSerialInterface.STOP_BITS_15);
        stopBitsOpts.put("2", UsbSerialInterface.STOP_BITS_2);
        parityOpts.put("-", OPT_PRESERVE);
        parityOpts.put("none", UsbSerialInterface.PARITY_NONE);
        parityOpts.put("even", UsbSerialInterface.PARITY_EVEN);
        parityOpts.put("odd", UsbSerialInterface.PARITY_ODD);
        parityOpts.put("mark", UsbSerialInterface.PARITY_MARK);
        parityOpts.put("space", UsbSerialInterface.PARITY_SPACE);
        flowControlOpts.put("-", OPT_PRESERVE);
        flowControlOpts.put("off", UsbSerialInterface.FLOW_CONTROL_OFF);
        flowControlOpts.put("xon_xoff", UsbSerialInterface.FLOW_CONTROL_XON_XOFF);
        flowControlOpts.put("rts_cts", UsbSerialInterface.FLOW_CONTROL_RTS_CTS);
        flowControlOpts.put("dsr_dtr", UsbSerialInterface.FLOW_CONTROL_DSR_DTR);
    }

    int baudrate = OPT_PRESERVE;
    int dataBits = OPT_PRESERVE;
    int stopBits = OPT_PRESERVE;
    int parity = OPT_PRESERVE;
    int flowControl = OPT_PRESERVE;
    boolean insecure = false;
    @NonNull
    String adapter = "*";

    Context getContext() {
        return context;
    }

    private OnMessageListener onMessageListener = null;

    void reportError(@NonNull final Throwable e) {
        if (onMessageListener != null)
            onMessageListener.onMessage(e);
    }

    private Impl impl = null;

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        baudrate = pp.getInt("baudrate", baudrate);
        dataBits = pp.getFromMap("databits", dataBitsOpts, dataBits);
        stopBits = pp.getFromMap("stopbits", stopBitsOpts, stopBits);
        parity = pp.getFromMap("parity", parityOpts, parity);
        flowControl = pp.getFromMap("flowcontrol", flowControlOpts, flowControl);
        insecure = pp.getBoolean("insecure", insecure);
        adapter = pp.getString("adapter", adapter);
        if (adapter.isEmpty())
            adapter = "*";
    }

    OutputStream output = null;
    private final OutputStream input = new OutputStream() {
        @Override
        public void write(final int b) throws IOException {
            if (impl != null)
                impl.getOutputStream().write(b);
        }

        @Override
        public void write(@NonNull final byte[] b, final int off, final int len) throws IOException {
            if (impl != null)
                impl.getOutputStream().write(b, off, len);
        }

        @Override
        public void write(@NonNull final byte[] b) throws IOException {
            if (impl != null)
                impl.getOutputStream().write(b);
        }
    };

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        output = stream;
    }

    @Override
    @NonNull
    public OutputStream getOutputStream() {
        return input;
    }

    @Override
    public void setOnMessageListener(@Nullable final OnMessageListener l) {
        onMessageListener = l;
    }

    static final class AdapterNotFoundException extends ResultException {
        public AdapterNotFoundException() {
            super();
        }

        public AdapterNotFoundException(final String message) {
            super(message);
        }
    }

    @Override
    public boolean isConnected() {
        return impl != null && impl.isConnected();
    }

    @Override
    public void connect() {
        impl = new UsbImpl(this);
        try {
            impl.connect();
        } catch (final AdapterNotFoundException eUsb) {
            impl = new BtImpl(this);
            try {
                impl.connect();
            } catch (final AdapterNotFoundException eBt) {
                if ("*".equals(adapter))
                    throw new BackendException("No UART adapters found (USB or Bluetooth)");
                throw new BackendException(String.format(Locale.getDefault(),
                        "No device at `%s' found at the moment", adapter));
            }
        }
    }

    @Override
    public void disconnect() {
        try {
            if (impl == null)
                return;
            impl.disconnect();
            impl = null;
        } finally {
            if (isReleaseWakeLockOnDisconnect()) releaseWakeLock();
        }
    }

    @Override
    public void resize(final int col, final int row, final int wp, final int hp) {
    }

    @Override
    @Nullable
    public List<LogMessage> getLog() {
        return null;
    }

    @Override
    @NonNull
    public String getConnDesc() {
        final Map<String, Object> pp = new HashMap<>();
        pp.put("baudrate", baudrate);
        pp.put("databits", dataBitsOpts.rev.get(dataBits));
        pp.put("stopbits", stopBitsOpts.rev.get(stopBits));
        pp.put("parity", parityOpts.rev.get(parity));
        pp.put("flowcontrol", flowControlOpts.rev.get(flowControl));
        if (impl != null) {
            return String.format(Locale.getDefault(), "%s\n%s",
                    impl.getSubDesc(), meta.toUri(pp).toString());
        }
        return meta.toUri(pp).toString();
    }
}
