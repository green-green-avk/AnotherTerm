package green_green_avk.anotherterm;

import android.graphics.Color;
import android.support.annotation.NonNull;

public final class ConsoleScreenCharAttrs {
    public static final int DEF_FG_COLOR = Color.rgb(127, 127, 127);
    public static final int DEF_BG_COLOR = Color.rgb(0, 0, 0);

    public int fgColor;
    public int bgColor;
    public boolean invisible;
    public boolean inverse;
    public boolean bold;
    public boolean faint;
    public boolean italic;
    public boolean underline;
    public boolean crossed;
    public boolean blinking;
    public boolean richColor; // Don't tweak when bold.

    public ConsoleScreenCharAttrs() {
        reset();
    }

    public ConsoleScreenCharAttrs(@NonNull final ConsoleScreenCharAttrs aa) {
        set(aa);
    }

    public void reset() {
        fgColor = DEF_FG_COLOR;
        bgColor = DEF_BG_COLOR;
        invisible = false;
        inverse = false;
        bold = false;
        faint = false;
        italic = false;
        underline = false;
        crossed = false;
        blinking = false;
        richColor = false;
    }

    public void resetFg() {
        fgColor = DEF_FG_COLOR;
        richColor = false;
    }

    public void resetBg() {
        bgColor = DEF_BG_COLOR;
    }

    public void set(@NonNull final ConsoleScreenCharAttrs aa) {
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
        richColor = aa.richColor;
    }
}
