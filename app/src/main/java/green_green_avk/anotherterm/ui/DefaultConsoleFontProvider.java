package green_green_avk.anotherterm.ui;

import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

public final class DefaultConsoleFontProvider implements FontProvider {
    public static final Typeface[] typefaces = {
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
    };

    @Override
    public void setPaint(@NonNull final Paint paint, final int style) {
        paint.setTypeface(typefaces[style]);
    }
}
