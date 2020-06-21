package green_green_avk.anotherterm.utils;

import androidx.annotation.Nullable;

public final class FloatCaster implements Caster {
    @Override
    @Nullable
    public Object cast(@Nullable final Object v) {
        if (v instanceof String) return Float.parseFloat((String) v);
        if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
        if (v instanceof Integer) return (float) (int) v;
        if (v instanceof Long) return (float) (long) v;
        if (v instanceof Float) return v;
        if (v instanceof Double) return (float) (double) v;
        return null;
    }
}
