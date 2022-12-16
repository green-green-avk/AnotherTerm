package green_green_avk.anotherterm.ui;

import androidx.annotation.Nullable;

public interface ParameterView<T> extends ReadonlyParameterView<T> {
    interface OnValueChanged<T> {
        void onValueChanged(@Nullable T v);
    }

    @Nullable
    T getValue();

    /**
     * @param v a callback to execute when value changed by user
     */
    void setOnValueChanged(@Nullable OnValueChanged<? super T> v);
}
