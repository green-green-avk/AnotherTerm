package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class Settings {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Param {
        @AnyRes int defRes() default 0;
    }

    protected SharedPreferences.OnSharedPreferenceChangeListener onChange =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                                      final String key) {
                    try {
                        set(key, sharedPreferences, get(key));
                    } catch (final NoSuchElementException ignored) {
                    } catch (final IllegalArgumentException ignored) {
                    }
                }
            };

    public void init(@NonNull final Context ctx, @NonNull final SharedPreferences sp) {
        final SharedPreferences.Editor editor = sp.edit(); // for repair
        final Resources rr = ctx.getResources();
        final Field[] ff = getClass().getFields();
        for (final Field f : ff) {
            final Param a = f.getAnnotation(Param.class);
            if (a == null) continue;
            Object v;
            try {
                v = f.get(this);
            } catch (final IllegalAccessException e) {
                continue;
            }
            final Class c = f.getType();
            if (c.equals(String.class)) {
                if (a.defRes() != 0)
                    v = rr.getString(a.defRes());
                try {
                    v = sp.getString(f.getName(), (String) v);
                } catch (ClassCastException e) {
                    editor.putString(f.getName(), (String) v);
                }
            } else if (c.equals(Integer.TYPE)) {
                if (a.defRes() != 0)
                    v = rr.getInteger(a.defRes());
                try {
                    v = sp.getInt(f.getName(), (int) v);
                } catch (ClassCastException e) {
                    editor.putInt(f.getName(), (int) v);
                }
            } else if (c.equals(Boolean.TYPE)) {
                if (a.defRes() != 0)
                    v = rr.getBoolean(a.defRes());
                try {
                    v = sp.getBoolean(f.getName(), (boolean) v);
                } catch (ClassCastException e) {
                    editor.putBoolean(f.getName(), (boolean) v);
                }
            } else continue;
            try {
                f.set(this, v);
            } catch (final IllegalAccessException ignored) {
            }
        }
        editor.apply();
        sp.registerOnSharedPreferenceChangeListener(onChange);
    }

    public Object get(@NonNull final String k) {
        final Field f;
        try {
            f = getClass().getField(k);
        } catch (final NoSuchFieldException e) {
            throw new NoSuchElementException();
        }
        if (f.getAnnotation(Param.class) == null) return null;
        try {
            return f.get(this);
        } catch (final IllegalAccessException e) {
            throw new NoSuchElementException();
        }
    }

    public void set(@NonNull final String k, @Nullable final Object v) {
        final Field f;
        try {
            f = getClass().getField(k);
        } catch (NoSuchFieldException e) {
            throw new NoSuchElementException();
        }
        if (f.getAnnotation(Param.class) == null) throw new NoSuchElementException();
        try {
            f.set(this, v);
        } catch (final IllegalAccessException e) {
            throw new NoSuchElementException();
        }
    }

    public void set(@NonNull final String k, @NonNull final SharedPreferences sp,
                    @Nullable final Object dv) {
        final Field f;
        try {
            f = getClass().getField(k);
        } catch (final NoSuchFieldException e) {
            throw new NoSuchElementException();
        }
        if (f.getAnnotation(Param.class) == null) throw new NoSuchElementException();
        final Object v;
        final Class c = f.getType();
        try {
            if (c.equals(String.class)) {
                v = sp.getString(k, (String) dv);
            } else if (c.equals(Integer.TYPE)) {
                v = sp.getInt(k, (int) dv);
            } else if (c.equals(Boolean.TYPE)) {
                v = sp.getBoolean(k, (boolean) dv);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException("Wrong field type");
        }
        try {
            f.set(this, v);
        } catch (final IllegalAccessException e) {
            throw new NoSuchElementException();
        }
    }

    public void fill(@NonNull final Map<String, ?> map) {
        final Field[] ff = getClass().getFields();
        for (final Field f : ff) {
            if (map.containsKey(f.getName()))
                set(f.getName(), map.get(f.getName()));
        }
    }
}
