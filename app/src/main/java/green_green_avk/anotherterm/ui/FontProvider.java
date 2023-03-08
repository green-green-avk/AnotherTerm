package green_green_avk.anotherterm.ui;

import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface FontProvider {
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @IntDef(flag = true,
            value = {Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC})
    @interface Style {
    }

    final class MonospaceMetrics {
        public float width = 0;
        public float height = 0;
        public float ascent = 0;
    }

    void getGlyphSize(@NonNull MonospaceMetrics out, float fontSize);

    void populatePaint(@NonNull Paint out, @Style int style);
}
