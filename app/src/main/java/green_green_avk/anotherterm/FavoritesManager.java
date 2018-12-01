package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.utils.SharedPreferencesSet;

public final class FavoritesManager {
    private FavoritesManager() {
    }

    @SuppressLint("StaticFieldLeak")
    private static final SharedPreferencesSet prefs = new SharedPreferencesSet();

    private static final Set<Runnable> onChangeListeners =
            Collections.newSetFromMap(new WeakHashMap<Runnable, Boolean>());

    private static void execOnChangeListeners() {
        for (final Runnable r : onChangeListeners) {
            r.run();
        }
    }

    public static void init(@NonNull final Context context) {
        final Context ac = context.getApplicationContext();
        prefs.init(ac, ac.getPackageName() + "_fav_");
    }

    @NonNull
    public static Set<String> enumerate() {
        return prefs.enumerate();
    }

    public static boolean contains(@NonNull final String key) {
        return prefs.contains(key);
    }

    @NonNull
    public static PreferenceStorage get(@NonNull final String name) {
        final PreferenceStorage ps = new PreferenceStorage();
        ps.load(prefs.peek(name));
        return ps;
    }

    public static void set(@NonNull final String name, @NonNull final PreferenceStorage ps) {
        ps.save(prefs.get(name));
        execOnChangeListeners();
    }

    public static void remove(@NonNull final String name) {
        prefs.remove(name);
        execOnChangeListeners();
    }

    public static void move(@NonNull final String from, @NonNull final String to) {
        prefs.move(from, to);
        execOnChangeListeners();
    }

    public static void copy(@NonNull final String from, @NonNull final String to) {
        prefs.copy(from, to);
        execOnChangeListeners();
    }

    public static void addOnChangeListener(@NonNull final Runnable runnable) {
        onChangeListeners.add(runnable);
    }
}
