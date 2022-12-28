package name.green_green_avk.compatcolorpicker;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

final class HSVColorBox {
    @NonNull
    @Size(3)
    public final float[] hsv;

    public HSVColorBox(@NonNull @Size(3) final float[] hsv) {
        this.hsv = hsv;
    }

    public void from(@NonNull final HSVColorBox v) {
        hsv[0] = v.hsv[0];
        hsv[1] = v.hsv[1];
        hsv[2] = v.hsv[2];
    }
}
