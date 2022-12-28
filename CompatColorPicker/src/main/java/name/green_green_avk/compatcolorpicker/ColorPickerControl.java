package name.green_green_avk.compatcolorpicker;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

interface ColorPickerControl {
    @ColorInt
    int getValue();

    /**
     * Sets a callback to be called when the value being set by the user.
     *
     * @param v the callback
     */
    void setOnValueChanged(@Nullable ColorPickerView.OnValueChanged v);

    void setValue(@ColorInt int v);
}
