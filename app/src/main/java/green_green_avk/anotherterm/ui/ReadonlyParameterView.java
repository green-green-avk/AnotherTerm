package green_green_avk.anotherterm.ui;

import androidx.annotation.Nullable;

public interface ReadonlyParameterView<T> {
    void setValue(@Nullable T v);

    default void setValueFrom(@Nullable final Object v) {
        final T _v;
        try {
            _v = (T) v;
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        setValue(_v);
    }
}
