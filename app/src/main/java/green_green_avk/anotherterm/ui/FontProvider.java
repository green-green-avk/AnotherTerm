package green_green_avk.anotherterm.ui;

import android.graphics.Paint;

import androidx.annotation.NonNull;

public interface FontProvider {
    void populatePaint(@NonNull Paint out, int style);
}
