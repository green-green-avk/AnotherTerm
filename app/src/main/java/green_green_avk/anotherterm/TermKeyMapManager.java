package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.utils.SharedPreferencesSet;

public final class TermKeyMapManager {
    private TermKeyMapManager() {
    }

    private static final Map<String, TermKeyMap> cache = new HashMap<>();

    public static final TermKeyMap defaultKeyMap = new TermKeyMap();

    public static class Meta {
        final String name;
        final Object title;
        final boolean isBuiltIn;
        final int order;

        public Meta(final String name, final Object title, final boolean isBuiltIn,
                    final int order) {
            this.name = name;
            this.title = title;
            this.isBuiltIn = isBuiltIn;
            this.order = order;
        }

        public Meta(final String name, final String title, final boolean isBuiltIn) {
            this(name, title, isBuiltIn, 0);
        }

        @NonNull
        public String getTitle(@NonNull final Context ctx) {
            if (title instanceof Integer) return ctx.getString((Integer) title);
            return title.toString();
        }

        @NonNull
        public TermKeyMapRules getKeyMap() {
            if (this instanceof BuiltIn) return ((BuiltIn) this).keyMap;
            return TermKeyMapManager.get(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (obj instanceof Meta) return isBuiltIn == ((Meta) obj).isBuiltIn
                    && name.equals(((Meta) obj).name);
            if (obj instanceof String) return obj.equals(name);
            return super.equals(obj);
        }

        @NonNull
        public static Object byName(final String name) {
            return new Object() {
                @Override
                public int hashCode() {
                    return name == null ? super.hashCode() : name.hashCode();
                }

                @Override
                public boolean equals(@Nullable final Object obj) {
                    if (obj instanceof Meta) return ((Meta) obj).name.equals(name);
                    if (name == null) return super.equals(obj);
                    return name.equals(obj);
                }
            };
        }
    }

    private static final Map<TermKeyMapRules, Meta> meta = new WeakHashMap<>();

    public static final class BuiltIn extends Meta {
        final TermKeyMap keyMap;

        public BuiltIn(final String name, @StringRes final int title, final TermKeyMap keyMap,
                       final int order) {
            super(name, title, true, order);
            this.keyMap = keyMap;
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (obj instanceof TermKeyMap) return obj.equals(keyMap);
            return super.equals(obj);
        }
    }

    public static final Map<String, BuiltIn> builtIns;
    public static final BuiltIn defaultBuiltIn =
            new BuiltIn("", R.string.keymap_title_default, defaultKeyMap, 0);

    static {
        final Map<String, BuiltIn> _builtIns = new HashMap<>();
        _builtIns.put("", defaultBuiltIn);
        builtIns = Collections.unmodifiableMap(_builtIns);
    }

    @SuppressLint("StaticFieldLeak")
    private static final SharedPreferencesSet maps = new SharedPreferencesSet();

    private static final Set<Runnable> onChangeListeners =
            Collections.newSetFromMap(new WeakHashMap<Runnable, Boolean>());

    private static void execOnChangeListeners() {
        for (final Runnable r : onChangeListeners) {
            r.run();
        }
    }

    public static void init(@NonNull final Context context) {
        final Context ac = context.getApplicationContext();
        maps.init(ac, ac.getPackageName() + "_keymap_");
    }

    @NonNull
    public static Set<Meta> enumerate() {
        return new AbstractSet<Meta>() {
            @Override
            public Iterator<Meta> iterator() {
                return new Iterator<Meta>() {
                    int i = 0;
                    final Iterator<? extends Meta>[] ii = new Iterator[]{
                            builtIns.values().iterator(),
                            enumerateCustom().iterator()
                    };

                    @Override
                    public boolean hasNext() {
                        while (i < ii.length) {
                            if (ii[i].hasNext()) return true;
                            ++i;
                        }
                        return false;
                    }

                    @Override
                    public Meta next() {
                        if (hasNext()) return ii[i].next();
                        return null;
                    }
                };
            }

            @Override
            public int size() {
                return builtIns.size() + maps.enumerate().size();
            }
        };
    }

    public static Set<Meta> enumerateCustom() {
        return new AbstractSet<Meta>() {
            @Override
            public Iterator<Meta> iterator() {
                return new Iterator<Meta>() {
                    final Iterator<String> i = maps.enumerate().iterator();

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
                return maps.enumerate().size();
            }
        };
    }

    public static boolean contains(@NonNull final String key) {
        return maps.contains(key);
    }

    public static boolean isBuiltIn(@Nullable final String name) {
        return name == null || name.isEmpty() || name.charAt(0) == ' ';
    }

    @NonNull
    public static TermKeyMapRules getRules(@Nullable final String name) {
        if (isBuiltIn(name)) return TermKeyMapRulesParser.getNew();
        final PreferenceStorage ps = new PreferenceStorage();
        ps.load(maps.peek(name));
        return TermKeyMapRulesParser.fromSP(ps.get());
    }

    @Nullable
    private static BuiltIn getBuiltIn(@Nullable final String name) {
        if (isBuiltIn(name)) {
            final BuiltIn builtIn = builtIns.get(name);
            return builtIn != null ? builtIn : defaultBuiltIn;
        }
        return null;
    }

    @Nullable
    private static BuiltIn getBuiltIn(@NonNull final TermKeyMapRules rules) {
        for (final BuiltIn builtIn : builtIns.values()) {
            if (builtIn.keyMap.equals(rules)) return builtIn;
        }
        return null;
    }

    @Nullable
    public static Meta getMeta(@Nullable final String name) {
        final BuiltIn b = getBuiltIn(name);
        if (b != null) return b;
        return new Meta(name, name, false);
    }

    @Nullable
    public static Meta getMeta(@NonNull final TermKeyMapRules rules) {
        final BuiltIn b = getBuiltIn(rules);
        if (b != null) return b;
        return meta.get(rules);
    }

    @NonNull
    public static String getTitle(@Nullable final String name, @NonNull final Context ctx) {
        final BuiltIn b = getBuiltIn(name);
        return b == null ? name : b.getTitle(ctx);
    }

    @NonNull
    public static TermKeyMapRules get(@Nullable final String name) {
        final BuiltIn b = getBuiltIn(name);
        if (b != null) return b.keyMap;
        final TermKeyMapRules r = cache.get(name);
        if (r != null) return r;
        final TermKeyMap km = defaultKeyMap.copy().append(getRules(name));
        cache.put(name, km);
        meta.put(km, new Meta(name, name, false));
        return km;
    }

    public static void set(@NonNull final String name, @NonNull final TermKeyMapRules rules) {
        final PreferenceStorage ps = new PreferenceStorage();
        ps.putAll(TermKeyMapRulesParser.toSP(rules));
        ps.save(maps.get(name));
        final TermKeyMap r = cache.get(name);
        if (r != null) r.reinit(defaultKeyMap).append(rules);
        execOnChangeListeners();
    }

    public static void remove(@NonNull final String key) {
        cache.remove(key);
        maps.remove(key);
        execOnChangeListeners();
    }

    public static void move(@NonNull final String from, @NonNull final String to) {
        cache.remove(from);
        cache.remove(to);
        maps.move(from, to);
        execOnChangeListeners();
    }

    public static void copy(@NonNull final String from, @NonNull final String to) {
        cache.remove(to);
        maps.copy(from, to);
        execOnChangeListeners();
    }

    public static void addOnChangeListener(@NonNull final Runnable runnable) {
        onChangeListeners.add(runnable);
    }
}
