package green_green_avk.anotherterm.utils;

import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PreferenceStorage {
    @NonNull
    private Map<String, Object> prefs;

    public PreferenceStorage() {
        this.prefs = new HashMap<>();
    }

    public PreferenceStorage(@Nullable final Map<String, ?> prefs) {
        this.prefs = prefs != null ? (Map<String, Object>) prefs : new HashMap<String, Object>();
    }

    public void set(@Nullable final Map<String, ?> prefs) {
        if (prefs != null) this.prefs = (Map<String, Object>) prefs;
        else clear();
    }

    public void clear() {
        prefs.clear();
    }

    @CheckResult
    @NonNull
    public Map<String, ?> get() {
        return prefs;
    }

    public void putAll(@Nullable final Map<String, ?> prefs) {
        if (prefs == null) return;
        this.prefs.putAll(prefs);
    }

    public Object get(final String key) {
        return prefs.get(key);
    }

    public void put(final String key, final Object value) {
        prefs.put(key, value);
    }

    public void load(@NonNull final SharedPreferences sp) {
        putAll(sp.getAll());
    }

    public void save(@NonNull final SharedPreferences sp) {
        final SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        putAll(editor, prefs);
        editor.apply();
    }

    public static void putAll(@NonNull final SharedPreferences.Editor dst,
                              @NonNull final Map<String, ?> values) {
        for (final String k : values.keySet()) {
            final Object v = values.get(k);
            if (v instanceof Double || v instanceof Float) {
                dst.putFloat(k, (float) v);
            } else if (v instanceof Long) {
                dst.putLong(k, (long) v);
            } else if (v instanceof Integer) {
                dst.putInt(k, (int) v);
            } else if (v instanceof Boolean) {
                dst.putBoolean(k, (boolean) v);
            } else if (v instanceof String) {
                dst.putString(k, (String) v);
            } else if (v instanceof Set) {
                dst.putStringSet(k, (Set<String>) v);
            } else if (v instanceof Uri) {
                dst.putString(k, v.toString());
            }
        }
    }
}
