package green_green_avk.anotherterm;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public interface AnsiColorProfile {
    @ColorInt
    int getColor(@NonNull ConsoleScreenCharAttrs.Color color);

    /**
     * For profile specific tweaks.
     */
    @ColorInt
    int getFgColor(@NonNull ConsoleScreenCharAttrs attrs, boolean screenInverse);

    /**
     * For profile specific tweaks.
     */
    @ColorInt
    int getBgColor(@NonNull ConsoleScreenCharAttrs attrs, boolean screenInverse);
}
