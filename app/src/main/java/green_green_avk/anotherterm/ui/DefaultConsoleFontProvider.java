package green_green_avk.anotherterm.ui;

import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

public final class DefaultConsoleFontProvider implements FontProvider {
    private DefaultConsoleFontProvider() {
    }

    @NonNull
    public static DefaultConsoleFontProvider getInstance() {
        return instance;
    }

    private static final Typeface[] typefaces = {
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
    };

    public static void getGlyphSizeDefault(@NonNull final MonospaceMetrics out,
                                           @NonNull final Paint paint) {
        out.width = paint.measureText("A");
        out.height = paint.getFontSpacing();
        out.ascent = (paint.descent() + paint.ascent() - out.height) / 2;
    }

    @Override
    public void getGlyphSize(@NonNull final MonospaceMetrics out, final float fontSize) {
        final Paint p = new Paint();
        populatePaint(p, Typeface.NORMAL);
        p.setTextSize(fontSize);
        getGlyphSizeDefault(out, p);
    }

    @Override
    public void populatePaint(@NonNull final Paint out, @Style final int style) {
        out.setTypeface(typefaces[style]);
    }

    private static final DefaultConsoleFontProvider instance = new DefaultConsoleFontProvider();
}
