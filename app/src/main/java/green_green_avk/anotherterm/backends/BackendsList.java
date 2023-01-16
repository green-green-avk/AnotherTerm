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
import green_green_avk.anotherterm.backends.uart.UartModule;

public final class BackendsList {
    private BackendsList() {
    }

    private static final Item[] list = {
            new Item(LocalModule.class, "local", true,
                    R.layout.local_params_content,
                    R.string.conntype_local,
                    R.drawable.ic_smartphone),
            new Item(UartModule.class, "uart", false,
                    R.layout.uart_params_content,
                    R.string.conntype_uart,
                    R.drawable.ic_uart),
            new Item(SshModule.class, "ssh", false,
                    R.layout.ssh_params_content,
                    R.string.conntype_ssh,
                    R.drawable.ic_computer_key),
            new Item(TelnetModule.class, "telnet", false,
                    R.layout.telnet_params_content,
                    R.string.conntype_telnet,
                    R.drawable.ic_computer)
    };

    @NonNull
    public static Iterable<Item> get() {
        return Arrays.asList(list);
    }

    public static final class Item {
        @NonNull
        public final Class<? extends BackendModule> impl;
        @NonNull
        public final BackendModule.Meta meta;
        @NonNull
        public final String typeStr;
        /* Externally instantiable sessions */
        public final boolean exportable;
        @LayoutRes
        public final int settingsLayout;
        @StringRes
        public final int title;
        @DrawableRes
        public final int icon;

        private Item(@NonNull final Class<? extends BackendModule> impl,
                     @NonNull final String typeStr, final boolean exportable,
                     @LayoutRes final int settingsLayout, @StringRes final int title,
                     @DrawableRes final int icon) {
            this.impl = impl;
            this.meta = BackendModule.getMeta(impl, typeStr);
            this.typeStr = typeStr;
            this.exportable = exportable;
            this.settingsLayout = settingsLayout;
            this.title = title;
            this.icon = icon;
        }
    }

    private static final Map<Class<? extends BackendModule>, Integer> mImpls = new HashMap<>();
    private static final Map<String, Integer> mTypeStrs = new HashMap<>();
    private static final Map<String, Integer> mSchemes = new HashMap<>();

    static {
        for (int i = 0; i < list.length; ++i) {
            mImpls.put(list[i].impl, i);
            mTypeStrs.put(list[i].typeStr, i);
            for (final String scheme : list[i].meta.getUriSchemes()) mSchemes.put(scheme, i);
        }
    }

    public static int getId(final Class<? extends BackendModule> v) {
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
        if (v instanceof String) return getId((String) v);
        return -1;
    }

    public static int getIdByScheme(final String scheme) {
        final Integer id = mSchemes.get(scheme);
        if (id == null) return -1;
        return id;
    }

    @NonNull
    public static Item get(final int i) {
        return list[i];
    }

    @NonNull
    public static Item get(final Class<? extends BackendModule> v) {
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
    public static Map<String, Object> getDefaultParameters(final String type) {
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
    public static Map<String, Object> fromUri(@NonNull final Uri uri) {
        final String scheme = uri.getScheme();
        if (scheme == null) throw new BackendModule.ParametersUriParseException();
        final int id = getIdByScheme(scheme);
        if (id < 0) throw new BackendModule.ParametersUriParseException();
        final Map<String, Object> params = get(id).meta.fromUri(uri);
        params.put("type", get(id).typeStr);
        return params;
    }
}
