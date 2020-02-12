package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Only one instance per set must exist
public final class SharedPreferencesSet {
    private Context ctx = null;
    private String prefix = null;
    private Pattern pat = null;

    private final Set<String> keys = new HashSet<>();
    private final Set<String> keysRO = Collections.unmodifiableSet(keys);

    private final Set<Runnable> onChangeListeners =
            Collections.newSetFromMap(new WeakHashMap<Runnable, Boolean>());

    private static String encKey(@NonNull final String v) {
        return URLEncoder.encode(v);
    }

    private static String decKey(@NonNull final String ev) {
        return URLDecoder.decode(ev);
    }

    @NonNull
    private File getDir() {
        return new File(ctx.getApplicationInfo().dataDir, "shared_prefs");
    }

    @NonNull
    private String getName(@NonNull final String key) {
        return prefix + encKey(key);
    }

    @NonNull
    private String getFileName(@NonNull final String key) {
        return getName(key) + ".xml";
    }

    @MainThread
    public void init(@NonNull final Context context, @NonNull final String prefix) {
        if (ctx != null) return;
        reinit(context, prefix);
    }

    @MainThread
    public void reinit(@NonNull final Context context, @NonNull final String prefix) {
        ctx = context;
        this.prefix = prefix;
        pat = Pattern.compile("^" + Pattern.quote(prefix) + "(.*)\\.xml$");

        keys.clear();
        final File dir = getDir();
        if (dir.exists() && dir.isDirectory()) {
            for (final String i : dir.list()) {
                final Matcher m = pat.matcher(i);
                if (m.matches()) {
                    try {
                        keys.add(decKey(m.group(1)));
                    } catch (final IllegalArgumentException e) {
                        // Ignore improper entries.
                        Log.w(this.getClass().getSimpleName(),
                                "Malformed preferences file name", e);
                    }
                }
            }
        }
    }

    @NonNull
    @MainThread
    public Set<String> enumerate() {
        return keysRO;
    }

    @MainThread
    public boolean contains(@NonNull final String key) {
        return keys.contains(key);
    }

    @NonNull
    public SharedPreferences peek(@NonNull final String key) {
        return ctx.getSharedPreferences(getName(key), Context.MODE_PRIVATE);
    }

    @NonNull
    @MainThread
    public SharedPreferences get(@NonNull final String key) {
        final SharedPreferences sp = peek(key);
        if (keys.add(key)) execOnChangeListeners();
        return sp;
    }

    @MainThread
    public void remove(@NonNull final String key) {
        final boolean r = keys.remove(key);
        if (Build.VERSION.SDK_INT >= 24) {
            ctx.deleteSharedPreferences(getName(key));
        } else {
            final SharedPreferences.Editor e = peek(key).edit();
            e.clear();
            e.commit(); // Sorry, we must synchronize before
            final File dir = getDir();
            if (!dir.exists() || !dir.isDirectory()) return;
            final File f = new File(dir, getFileName(key));
            f.delete();
        }
        if (r) execOnChangeListeners();
    }

    @MainThread
    public void move(@NonNull final String from, @NonNull final String to) {
        // We cannot rely on files consistency
        copy(from, to);
        remove(from);
        execOnChangeListeners();
    }

    @MainThread
    public void copy(@NonNull final String from, @NonNull final String to) {
        // We cannot rely on files consistency
        final SharedPreferences spFrom = peek(from);
        final SharedPreferences.Editor spEdTo = peek(to).edit();
        spEdTo.clear();
        PreferenceStorage.putAll(spEdTo, spFrom.getAll());
        spEdTo.apply();
        keys.add(to);
        execOnChangeListeners();
    }

    private void execOnChangeListeners() {
        for (final Runnable r : onChangeListeners) {
            r.run();
        }
    }

    // runnable is stored as a weak reference
    @MainThread
    public void addOnChangeListener(@NonNull final Runnable runnable) {
        onChangeListeners.add(runnable);
    }
}
