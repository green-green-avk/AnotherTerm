package green_green_avk.anotherterm.utils;

import androidx.annotation.Nullable;

public final class StringCaster implements Caster {
    @Override
    @Nullable
    public Object cast(@Nullable final Object v) {
        return v != null ? v.toString() : null;
    }
}
