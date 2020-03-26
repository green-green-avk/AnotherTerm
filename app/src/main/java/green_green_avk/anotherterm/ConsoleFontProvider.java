package green_green_avk.anotherterm;

import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.FontProvider;

public final class ConsoleFontProvider implements FontProvider {
    @NonNull
    private final Typeface[] tfs = FontsManager.consoleTypefaces; // Just preserve them yet

    @Override
    public void setPaint(@NonNull final Paint paint, final int style) {
        FontsManager.setPaint(paint, tfs, style);
    }
}
