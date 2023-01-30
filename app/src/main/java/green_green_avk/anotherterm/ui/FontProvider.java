package green_green_avk.anotherterm.ui;

import android.graphics.Paint;

import androidx.annotation.NonNull;

public interface FontProvider {
    final class MonospaceMetrics {
        public float width = 0;
        public float height = 0;
        public float ascent = 0;
    }

    void getGlyphSize(@NonNull MonospaceMetrics out, float fontSize);

    void populatePaint(@NonNull Paint out, int style);
}
