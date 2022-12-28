package green_green_avk.anotherterm;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.io.Serializable;

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

    interface Editable extends AnsiColorProfile, Cloneable, Serializable {
        @NonNull
        Editable clone();

        void set(@NonNull Editable that);

        boolean dataEquals(@NonNull Editable that);

        @ColorInt
        int getDefaultFgNormal();

        void setDefaultFgNormal(@ColorInt int color);

        @ColorInt
        int getDefaultFgBold();

        void setDefaultFgBold(@ColorInt int color);

        @ColorInt
        int getDefaultFgFaint();

        void setDefaultFgFaint(@ColorInt int color);

        @ColorInt
        int getDefaultBg();

        void setDefaultBg(@ColorInt int color);

        @ColorInt
        int getBasicNormal(@IntRange(from = 0, to = 7) int idx);

        void setBasicNormal(@IntRange(from = 0, to = 7) int idx, @ColorInt int color);

        @ColorInt
        int getBasicBold(@IntRange(from = 0, to = 7) int idx);

        void setBasicBold(@IntRange(from = 0, to = 7) int idx, @ColorInt int color);

        @ColorInt
        int getBasicFaint(@IntRange(from = 0, to = 7) int idx);

        void setBasicFaint(@IntRange(from = 0, to = 7) int idx, @ColorInt int color);

        @ColorInt
        int get8bit(@IntRange(from = 0, to = 255) int idx);

        void set8bit(@IntRange(from = 0, to = 255) int idx, @ColorInt int color);
    }
}
