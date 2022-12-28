package name.green_green_avk.compatcolorpicker;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Size;

final class ChannelColorBox {
    @NonNull
    @Size(4)
    public final int[] bgra;
    @IntRange(from = 0, to = 3)
    public int channel = 0;

    ChannelColorBox(@NonNull final int[] bgra) {
        this.bgra = bgra;
    }
}
