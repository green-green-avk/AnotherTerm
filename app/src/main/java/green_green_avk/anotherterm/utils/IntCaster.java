package green_green_avk.anotherterm.utils;

import androidx.annotation.Nullable;

public final class IntCaster implements Caster {
    @Override
    @Nullable
    public Object cast(@Nullable final Object v) {
        if (v instanceof String) return Integer.parseInt((String) v);
        if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
        if (v instanceof Integer) return v;
        if (v instanceof Long) return (int) (long) v;
        if (v instanceof Float) return (int) (float) v;
        if (v instanceof Double) return (int) (double) v;
        return null;
    }
}
