package green_green_avk.anotherterm.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;

public final class ColorUtils {
    private ColorUtils() {
    }

    @ColorInt
    public static int CMYToRGB(final int cyan, final int magenta, final int yellow) {
        return Color.rgb(255 - cyan, 255 - magenta, 255 - yellow);
    }

    @ColorInt
    public static int CMYKToRGB(final int cyan, final int magenta, final int yellow,
                                final int key) {
        final int _k = 255 - key;
        return Color.rgb(255 - (cyan * _k >> 8) - key,
                255 - (magenta * _k >> 8) - key,
                255 - (yellow * _k >> 8) - key);
    }
}
