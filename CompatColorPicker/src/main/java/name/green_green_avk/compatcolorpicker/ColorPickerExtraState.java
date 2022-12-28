package name.green_green_avk.compatcolorpicker;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

interface ColorPickerExtraState {
    @Nullable
    Object getExtraState();

    void setExtraState(@Nullable Object s, @ColorInt int v);
}
