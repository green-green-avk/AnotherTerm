package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.utils.BaseProfileManager;
import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.utils.SharedPreferencesSet;

public final class TermKeyMapManager extends BaseProfileManager<TermKeyMapRules> {
    private TermKeyMapManager() {
    }

    public static final TermKeyMapManager instance = new TermKeyMapManager();

    public static final TermKeyMap defaultKeyMap = new TermKeyMap();

    private static final Map<TermKeyMapRules, Meta> meta = new WeakHashMap<>();
    private static final Map<String, TermKeyMap> cache = new HashMap<>();

    private static final Map<String, BuiltIn<? extends TermKeyMapRules>> builtInsByName;
    private static final BuiltIn<? extends TermKeyMapRules> defaultBuiltIn =
            new BuiltIn<>("", R.string.profile_title_default, defaultKeyMap, 0);

    static {
        final Map<String, BuiltIn<? extends TermKeyMapRules>> r = new HashMap<>();
        r.put(defaultBuiltIn.name, defaultBuiltIn);
        builtInsByName = Collections.unmodifiableMap(r);
    }

    private static final Set<BuiltIn<? extends TermKeyMapRules>> builtIns =
            Collections.unmodifiableSet(new HashSet<>(builtInsByName.values()));

    @SuppressLint("StaticFieldLeak")
    private static final SharedPreferencesSet maps = new SharedPreferencesSet();

    public static void init(@NonNull final Context context) {
        final Context ac = context.getApplicationContext();
        maps.init(ac, ac.getPackageName() + "_keymap_");
    }

    @Override
    @NonNull
    protected Map<String, BuiltIn<? extends TermKeyMapRules>> onGetBuiltIns() {
        return builtInsByName;
    }

    @Override
    @NonNull
    protected BuiltIn<? extends TermKeyMapRules> onGetDefaultBuiltIn() {
        return defaultBuiltIn;
    }

    @Override
    @NonNull
    public Set<BuiltIn<? extends TermKeyMapRules>> enumerateBuiltIn() {
        return builtIns;
    }

    @Override
    @NonNull
    public Set<? extends Meta> enumerateCustom() {
        return new AbstractSet<Meta>() {
            @Override
            @NonNull
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

    @Override
    public boolean containsCustom(@NonNull final String key) {
        return maps.contains(key);
    }

    @NonNull
    public TermKeyMapRules.Editable getForEdit(@Nullable final String name) {
        if (isBuiltIn(name))
            return TermKeyMapRulesParser.getNew();
        final PreferenceStorage ps = new PreferenceStorage();
        ps.load(maps.peek(name));
        return TermKeyMapRulesParser.fromSP(ps.get());
    }

    @Override
    @Nullable
    public Meta getMeta(@Nullable final String name) {
        final BuiltIn<? extends TermKeyMapRules> b = getBuiltIn(name);
        if (b != null)
            return b;
        return new Meta(name, name, false);
    }

    @Override
    @Nullable
    public Meta getMeta(@NonNull final TermKeyMapRules rules) {
        final BuiltIn<? extends TermKeyMapRules> b = getBuiltIn(rules);
        if (b != null)
            return b;
        return meta.get(rules);
    }

    @Override
    @NonNull
    public TermKeyMapRules get(@Nullable final String name) {
        final BuiltIn<? extends TermKeyMapRules> b = getBuiltIn(name);
        if (b != null)
            return b.data;
        final TermKeyMapRules r = cache.get(name);
        if (r != null)
            return r;
        final TermKeyMap km = defaultKeyMap.copy().append(getForEdit(name));
        cache.put(name, km);
        meta.put(km, new Meta(name, name, false));
        return km;
    }

    public void set(@NonNull final String name, @NonNull final TermKeyMapRules rules) {
        final PreferenceStorage ps = new PreferenceStorage();
        ps.putAll(TermKeyMapRulesParser.toSP(rules));
        ps.save(maps.get(name));
        final TermKeyMap r = cache.get(name);
        if (r != null)
            r.reinit(defaultKeyMap).append(rules);
        execOnChangeListeners();
    }

    @Override
    public void remove(@NonNull final String key) {
        cache.remove(key);
        maps.remove(key);
        execOnChangeListeners();
    }

    public void move(@NonNull final String from, @NonNull final String to) {
        cache.remove(from);
        cache.remove(to);
        maps.move(from, to);
        execOnChangeListeners();
    }

    public void copy(@NonNull final String from, @NonNull final String to) {
        cache.remove(to);
        maps.copy(from, to);
        execOnChangeListeners();
    }
}
