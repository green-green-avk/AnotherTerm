package green_green_avk.anotherterm;

import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.DefaultConsoleFontProvider;
import green_green_avk.anotherterm.ui.FontProvider;

public final class ConsoleFontProvider implements FontProvider {
    @NonNull
    private final Typeface[] tfs = FontsManager.consoleTypefaces; // Just preserve them yet

    @Override
    public void getGlyphSize(@NonNull final MonospaceMetrics out, final float fontSize) {
        final Paint p = new Paint();
        populatePaint(p, Typeface.NORMAL);
        p.setTextSize(fontSize);
        DefaultConsoleFontProvider.getGlyphSizeDefault(out, p);
    }

    @Override
    public void populatePaint(@NonNull final Paint out, final int style) {
        FontsManager.populatePaint(out, tfs, style);
    }
}
