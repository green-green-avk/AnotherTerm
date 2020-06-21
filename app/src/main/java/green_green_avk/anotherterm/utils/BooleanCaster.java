package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BooleanCaster implements Caster {
    public static boolean CAST(@Nullable final Object v) {
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        if (v instanceof Boolean) return (boolean) v;
        if (v instanceof Integer) return ((int) v) != 0;
        if (v instanceof Long) return ((long) v) != 0;
        if (v instanceof Float) return ((float) v) != 0;
        if (v instanceof Double) return ((Double) v) != 0;
        return false;
    }

    @Override
    @NonNull
    public Object cast(@Nullable final Object v) {
        return CAST(v);
    }
}
