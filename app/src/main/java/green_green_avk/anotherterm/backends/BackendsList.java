package green_green_avk.anotherterm.backends;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.local.LocalModule;
import green_green_avk.anotherterm.backends.ssh.SshModule;
import green_green_avk.anotherterm.backends.telnet.TelnetModule;
import green_green_avk.anotherterm.backends.usbUart.UsbUartModule;

public final class BackendsList {
    private BackendsList() {
    }

    private static final Item[] list = {
            new Item(LocalModule.class, "local",
                    R.layout.local_params_content, R.string.conntype_local, R.drawable.ic_smartphone),
            new Item(UsbUartModule.class, "uart",
                    R.layout.uart_params_content, R.string.conntype_uart, R.drawable.ic_usb),
            new Item(SshModule.class, "ssh",
                    R.layout.ssh_params_content, R.string.conntype_ssh, R.drawable.ic_computer_key),
            new Item(TelnetModule.class, "telnet",
                    R.layout.telnet_params_content, R.string.conntype_telnet, R.drawable.ic_computer)
    };

    public static Iterable<Item> get() {
        return Arrays.asList(list);
    }

    public static final class Item {
        public final Class<?> impl;
        public final BackendModule.Meta meta;
        public final String typeStr;
        @LayoutRes
        public final int settingsLayout;
        @StringRes
        public final int title;
        @DrawableRes
        public final int icon;

        public Item(@NonNull final Class<?> impl, @NonNull final String typeStr,
                    @LayoutRes final int settingsLayout, @StringRes final int title,
                    @DrawableRes final int icon) {
            this.impl = impl;
            this.meta = BackendModule.getMeta(impl, typeStr);
            this.typeStr = typeStr;
            this.settingsLayout = settingsLayout;
            this.title = title;
            this.icon = icon;
        }
    }

    private static final Map<Class<?>, Integer> mImpls = new HashMap<>();
    private static final Map<String, Integer> mTypeStrs = new HashMap<>();
    private static final Map<String, Integer> mSchemes = new HashMap<>();

    static {
        for (int i = 0; i < list.length; ++i) {
            mImpls.put(list[i].impl, i);
            mTypeStrs.put(list[i].typeStr, i);
            for (final String scheme : list[i].meta.getUriSchemes()) mSchemes.put(scheme, i);
        }
    }

    public static int getId(final Class v) {
        final Integer id = mImpls.get(v);
        if (id == null) return -1;
        return id;
    }

    public static int getId(final String v) {
        final Integer id = mTypeStrs.get(v);
        if (id == null) return -1;
        return id;
    }

    public static int getId(final Object v) {
        if (v instanceof String) {
            return getId((String) v);
        }
        return -1;
    }

    public static int getIdByScheme(final String scheme) {
        final Integer id = mSchemes.get(scheme);
        if (id == null) return -1;
        return id;
    }

    public static Item get(final int i) {
        return list[i];
    }

    public static Item get(final Class v) {
        return get(getId(v));
    }

    @NonNull
    public static List<String> getTitles(@NonNull final Context ctx) {
        // configuration related, don't cache
        return new AbstractList<String>() {
            @Override
            public String get(final int index) {
                return ctx.getString(list[index].title);
            }

            @Override
            public int size() {
                return list.length;
            }
        };
    }

    @NonNull
    public static Set<String> getSchemes() {
        return mSchemes.keySet();
    }

    @NonNull
    public static Map<String, ?> getDefaultParameters(final String type) {
        final int id = getId(type);
        return get(id).meta.getDefaultParameters();
    }

    @Nullable
    public static Map<String, String> checkParameters(@NonNull final Map<String, ?> params) {
        final String type = (String) params.get("type");
        final int id = getId(type);
        return get(id).meta.checkParameters(params);
    }

    @NonNull
    public static Uri toUri(@NonNull final Map<String, ?> params) {
        final String type = (String) params.get("type");
        final int id = getId(type);
        params.remove("type");
        return get(id).meta.toUri(params);
    }

    @NonNull
    public static Map<String, ?> fromUri(@NonNull final Uri uri) {
        final String scheme = uri.getScheme();
        if (scheme == null) throw new BackendModule.ParametersUriParseException();
        final int id = getIdByScheme(scheme);
        if (id < 0) throw new BackendModule.ParametersUriParseException();
        final Map<String, Object> params = (Map<String, Object>) get(id).meta.fromUri(uri);
        params.put("type", get(id).typeStr);
        return params;
    }
}
