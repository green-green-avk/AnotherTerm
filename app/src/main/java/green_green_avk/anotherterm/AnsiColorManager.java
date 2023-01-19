package green_green_avk.anotherterm;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import green_green_avk.anotherterm.utils.SimpleProfileManager;

public final class AnsiColorManager extends SimpleProfileManager<AnsiColorProfile> {
    @NonNull
    private final SharedPreferences sp;

    public AnsiColorManager(@NonNull final Context ctx, @NonNull final String name) {
        final Context ac = ctx.getApplicationContext();
        sp = ac.getSharedPreferences(ac.getPackageName() + ".colors." + name,
                Context.MODE_PRIVATE);
    }

    @Override
    @NonNull
    protected List<BuiltIn<? extends AnsiColorProfile>> onInitBuiltIns() {
        return Collections.singletonList(
                new BuiltIn<>("", R.string.profile_title_default,
                        ConsoleScreenCharAttrs.DEFAULT_COLOR_PROFILE, 0)
        );
    }

    @Override
    @Nullable
    protected AnsiColorProfile.Editable onLoad(@NonNull final String name) {
        final String s = sp.getString(name, null);
        if (s == null) {
            return null;
        }
        try {
            return new ConsoleScreenCharAttrs.TabularColorProfile(parse(s));
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    @NonNull
    public Set<? extends Meta> enumerateCustom() {
        return new AbstractSet<Meta>() {
            @Override
            @NonNull
            public Iterator<Meta> iterator() {
                return new Iterator<Meta>() {
                    private final Iterator<String> i = sp.getAll().keySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public Meta next() {
                        return getMeta(i.next());
                    }
                };
            }

            @Override
            public int size() {
                return sp.getAll().size();
            }
        };
    }

    @Override
    public boolean containsCustom(@NonNull final String name) {
        return sp.getAll().containsKey(name);
    }

    @NonNull
    public AnsiColorProfile.Editable getForEdit(@NonNull final String name) {
        final AnsiColorProfile.Editable data = (AnsiColorProfile.Editable) get(name);
        final Meta meta = getMeta(data);
        if (meta != null && meta.isBuiltIn) {
            return data.clone();
        }
        return data;
    }

    /**
     * @param name         color profile name
     * @param colorProfile color profile
     */
    public void set(@NonNull final String name,
                    @NonNull final AnsiColorProfile.Editable colorProfile) {
        if (colorProfile instanceof ConsoleScreenCharAttrs.TabularColorProfile) {
            final SharedPreferences.Editor ed = sp.edit();
            ed.putString(name, serialize(
                    ((ConsoleScreenCharAttrs.TabularColorProfile) colorProfile).basic));
            ed.apply();
        } else {
            throw new UnsupportedOperationException("Unsupported AnsiColorProfile implementation");
        }
        execOnChangeListeners();
    }

    @Override
    public void remove(@NonNull final String name) {
        final SharedPreferences.Editor ed = sp.edit();
        ed.remove(name);
        ed.apply();
        execOnChangeListeners();
    }

    private static final String uriScheme = "termcolormap";

    @NonNull
    public static AnsiColorProfile.Editable fromUri(@NonNull final Uri uri) {
        final ConsoleScreenCharAttrs.TabularColorProfile r =
                new ConsoleScreenCharAttrs.TabularColorProfile();
        fromUri(r, uri);
        return r;
    }

    public static void fromUri(@NonNull final AnsiColorProfile.Editable out,
                               @NonNull final Uri uri) {
        if (!(out instanceof ConsoleScreenCharAttrs.TabularColorProfile)) {
            throw new UnsupportedOperationException("Unsupported AnsiColorProfile implementation");
        }
        if (uriScheme.equals(uri.getScheme())) {
            final List<String> path = uri.getPathSegments();
            if (path.size() == 2) {
                switch (path.get(0)) {
                    case "v1":
                        ((ConsoleScreenCharAttrs.TabularColorProfile) out).basic =
                                parse(path.get(1));
                        return;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported URI format");
    }

    @NonNull
    public static Uri toUri(@NonNull final AnsiColorProfile.Editable colorProfile) {
        final String serialized;
        if (colorProfile instanceof ConsoleScreenCharAttrs.TabularColorProfile) {
            serialized =
                    serialize(((ConsoleScreenCharAttrs.TabularColorProfile) colorProfile).basic);
        } else {
            throw new UnsupportedOperationException("Unsupported AnsiColorProfile implementation");
        }
        final Uri.Builder b = new Uri.Builder()
                .scheme(uriScheme)
                .path("/v1/" + serialized);
        return b.build();
    }

    @NonNull
    private static int[] parse(@NonNull final String v) {
        if (v.length() != ConsoleScreenCharAttrs.BASIC_COLORS_NUM << 3) {
            throw new IllegalArgumentException();
        }
        final int[] r = new int[v.length() >> 3];
        for (int i = 0; i < r.length; i++) {
            r[i] = Integer.parseUnsignedInt(v.substring(i << 3, i + 1 << 3), 16);
        }
        return r;
    }

    @NonNull
    private static String serialize(@NonNull final int[] v) {
        final StringBuilder b = new StringBuilder();
        for (final int c : v) {
            b.append(String.format(Locale.ROOT, "%08X", c));
        }
        return b.toString();
    }
}
