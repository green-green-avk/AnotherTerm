package green_green_avk.anotherterm;

import android.graphics.Color;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.utils.Misc;

public final class ConsoleScreenCharAttrs {
    private static final int NH = 0xC0;
    private static final int NL = 0x00;
    private static final int BH = 0xFF;
    private static final int BL = 0x80;

    private static final int[] DEF_BASIC_COLORS = new int[16];

    static {
        for (int i = 0; i < 8; i++) {
            DEF_BASIC_COLORS[i] = Color.rgb(
                    Misc.bitsAs(i, 1) ? NH : NL,
                    Misc.bitsAs(i, 2) ? NH : NL,
                    Misc.bitsAs(i, 4) ? NH : NL
            );
        }
        for (int i = 8; i < 16; i++) {
            DEF_BASIC_COLORS[i] = Color.rgb(
                    Misc.bitsAs(i, 1) ? BH : BL,
                    Misc.bitsAs(i, 2) ? BH : BL,
                    Misc.bitsAs(i, 4) ? BH : BL
            );
        }
    }

    private static final boolean DEF_FG_COLOR_INDEXED = true;
    private static final int DEF_FG_COLOR = 7; // index
    private static final int DEF_BG_COLOR = DEF_BASIC_COLORS[0];

    public static int getBasicColor(final int index) {
        return ConsoleScreenCharAttrs.DEF_BASIC_COLORS[index];
    }

    public int fgColor;
    public int bgColor;
    public boolean fgColorIndexed; // Tweak when bold / faint.
    public boolean invisible;
    public boolean inverse;
    public boolean bold;
    public boolean faint;
    public boolean italic;
    public boolean underline;
    public boolean crossed;
    public boolean blinking;

    public ConsoleScreenCharAttrs() {
        reset();
    }

    public ConsoleScreenCharAttrs(@NonNull final ConsoleScreenCharAttrs aa) {
        set(aa);
    }

    public void reset() {
        resetFg();
        resetBg();
        invisible = false;
        inverse = false;
        bold = false;
        faint = false;
        italic = false;
        underline = false;
        crossed = false;
        blinking = false;
    }

    public void resetFg() {
        fgColorIndexed = DEF_FG_COLOR_INDEXED;
        fgColor = DEF_FG_COLOR;
    }

    public void resetBg() {
        bgColor = DEF_BG_COLOR;
    }

    public void set(@NonNull final ConsoleScreenCharAttrs aa) {
        fgColorIndexed = aa.fgColorIndexed;
        fgColor = aa.fgColor;
        bgColor = aa.bgColor;
        invisible = aa.invisible;
        inverse = aa.inverse;
        bold = aa.bold;
        faint = aa.faint;
        italic = aa.italic;
        underline = aa.underline;
        crossed = aa.crossed;
        blinking = aa.blinking;
    }
}
