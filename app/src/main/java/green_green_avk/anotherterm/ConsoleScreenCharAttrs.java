package green_green_avk.anotherterm;

import android.graphics.Color;

public final class ConsoleScreenCharAttrs {
    public static final int DEF_FG_COLOR = Color.rgb(255, 255, 255);
    public static final int DEF_BG_COLOR = Color.rgb(0, 0, 0);

    public int fgColor;
    public int bgColor;
    public boolean inverse;
    public boolean bold;
    public boolean italic;
    public boolean underline;
    public boolean blinking;

    public ConsoleScreenCharAttrs() {
        reset();
    }

    public ConsoleScreenCharAttrs(final ConsoleScreenCharAttrs aa) {
        set(aa);
    }

    public void reset() {
        fgColor = DEF_FG_COLOR;
        bgColor = DEF_BG_COLOR;
        inverse = false;
        bold = false;
        italic = false;
        underline = false;
        blinking = false;
    }

    public void resetFg() {
        fgColor = DEF_FG_COLOR;
    }

    public void resetBg() {
        bgColor = DEF_BG_COLOR;
    }

    public void set(final ConsoleScreenCharAttrs aa) {
        fgColor = aa.fgColor;
        bgColor = aa.bgColor;
        inverse = aa.inverse;
        bold = aa.bold;
        italic = aa.italic;
        underline = aa.underline;
        blinking = aa.blinking;
    }
}
