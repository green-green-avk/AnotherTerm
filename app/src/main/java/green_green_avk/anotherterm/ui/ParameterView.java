package green_green_avk.anotherterm.ui;

import androidx.annotation.Nullable;

public interface ParameterView<T> {
    interface OnValueChanged<T> {
        void onValueChanged(@Nullable T v);
    }

    @Nullable
    T getValue();

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

    void setOnValueChanged(@Nullable OnValueChanged<T> v);
}
