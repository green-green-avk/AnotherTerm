package green_green_avk.anotherterm;

import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Arrays;

public final class ConsoleScreenCharAttrs {
    private static final int[] DEF_BASIC_COLORS = new int[]{
            // Normal
            android.graphics.Color.rgb(0x00, 0x00, 0x00),
            android.graphics.Color.rgb(0xEE, 0x33, 0x33),
            android.graphics.Color.rgb(0x33, 0xCC, 0x33),
            android.graphics.Color.rgb(0xCC, 0xAA, 0x33),
            android.graphics.Color.rgb(0x33, 0x33, 0xCC),
            android.graphics.Color.rgb(0xCC, 0x33, 0xAA),
            android.graphics.Color.rgb(0x33, 0xAA, 0xAA),
            android.graphics.Color.rgb(0xCC, 0xCC, 0xCC),
            // Bold
            android.graphics.Color.rgb(0x77, 0x77, 0x77),
            android.graphics.Color.rgb(0xFF, 0x88, 0x88),
            android.graphics.Color.rgb(0x88, 0xFF, 0x88),
            android.graphics.Color.rgb(0xFF, 0xFF, 0x88),
            android.graphics.Color.rgb(0x88, 0x88, 0xFF),
            android.graphics.Color.rgb(0xFF, 0x88, 0xFF),
            android.graphics.Color.rgb(0x88, 0xFF, 0xFF),
            android.graphics.Color.rgb(0xFF, 0xFF, 0xFF),
            // Faint
            android.graphics.Color.rgb(0x11, 0x11, 0x11),
            android.graphics.Color.rgb(0xCC, 0x22, 0x22),
            android.graphics.Color.rgb(0x22, 0xAA, 0x22),
            android.graphics.Color.rgb(0xAA, 0x88, 0x22),
            android.graphics.Color.rgb(0x22, 0x22, 0xAA),
            android.graphics.Color.rgb(0xAA, 0x22, 0x88),
            android.graphics.Color.rgb(0x22, 0x88, 0x88),
            android.graphics.Color.rgb(0x77, 0x77, 0x77),
            // Default foreground normal / bold / faint / background
            android.graphics.Color.rgb(0xCC, 0xCC, 0xCC),
            android.graphics.Color.rgb(0xFF, 0xFF, 0xFF),
            android.graphics.Color.rgb(0x77, 0x77, 0x77),
            android.graphics.Color.argb(0xC0, 0x00, 0x00, 0x00)
    };
    public static final int BASIC_COLORS_NUM = DEF_BASIC_COLORS.length;

    private static final int[] DEF_8BIT_COLORS = new int[256];

    static {
        System.arraycopy(DEF_BASIC_COLORS, 0,
                DEF_8BIT_COLORS, 0, 16);
        for (int i = 0; i < 216; i++) {
            DEF_8BIT_COLORS[i + 16] = android.graphics.Color.rgb(
                    (i / 36) * 51,
                    ((i / 6) % 6) * 51,
                    (i % 6) * 51
            );
        }
        for (int i = 0; i < 24; i++) {
            final int l = 8 + 10 * i;
            DEF_8BIT_COLORS[i + 232] = android.graphics.Color.rgb(l, l, l);
        }
    }

    public static class TabularColorProfile implements AnsiColorProfile.Editable {
        @NonNull
        private int[] basic = DEF_BASIC_COLORS;
        @NonNull
        private int[] _8bit = DEF_8BIT_COLORS;

        @NonNull
        int[] getRawBasic() {
            return basic;
        }

        void setRawBasic(@NonNull final int[] v) {
            basic = v;
            invalidate();
        }

        @NonNull
        int[] getRaw8bit() {
            return _8bit;
        }

        void setRaw8bit(@NonNull final int[] v) {
            _8bit = v;
            invalidate();
        }

        public TabularColorProfile() {
        }

        public TabularColorProfile(@NonNull final int[] basic) {
            this.basic = basic;
        }

        @Override
        @NonNull
        public TabularColorProfile clone() {
            final TabularColorProfile r;
            try {
                r = (TabularColorProfile) super.clone();
            } catch (final CloneNotSupportedException e) {
                throw new AssertionError();
            }
            r.basic = basic.clone();
            r._8bit = _8bit.clone();
            return r;
        }

        @Override
        public void set(@NonNull final Editable that) {
            if (that instanceof TabularColorProfile) {
                System.arraycopy(((TabularColorProfile) that).basic, 0,
                        basic, 0, basic.length);
                System.arraycopy(((TabularColorProfile) that)._8bit, 0,
                        _8bit, 0, _8bit.length);
                invalidate();
                return;
            }
            throw new IllegalArgumentException("Wrong type");
        }

        @Override
        public boolean dataEquals(@NonNull final Editable that) {
            if (this == that)
                return true;
            if (that instanceof TabularColorProfile)
                return Arrays.equals(basic, ((TabularColorProfile) that).basic) &&
                        Arrays.equals(_8bit, ((TabularColorProfile) that)._8bit);
            return false;
        }

        @ColorInt
        protected int getColor(@NonNull final Color color, final boolean isFg,
                               final boolean bold, final boolean faint) {
            if (color.isDefault()) {
                if (!isFg)
                    return basic[27];
                if (bold == faint)
                    return basic[24];
                if (bold)
                    return basic[25];
                return basic[26];
            }
            switch (color.type) {
                case BASIC:
                    if (bold == faint)
                        return basic[color.value & 0x0F];
                    if (bold)
                        return basic[color.value & 0x0F | 0x08];
                    return basic[color.value & 0x07 | 0x10];
                case _8BIT:
                    return _8bit[color.value & 0xFF];
                default:
                    return color.value;
            }
        }

        @Override
        @ColorInt
        public int getColor(@NonNull final Color color) {
            return getColor(color, true, false, false);
        }

        @Override
        @ColorInt
        public int getFgColor(@NonNull final ConsoleScreenCharAttrs attrs,
                              final boolean screenInverse) {
            if (attrs.inverse != screenInverse) {
                return getColor(attrs.bgColor, false, false, false);
            }
            return getColor(attrs.fgColor, true, attrs.bold, attrs.faint);
        }

        @Override
        @ColorInt
        public int getBgColor(@NonNull final ConsoleScreenCharAttrs attrs,
                              final boolean screenInverse) {
            if (attrs.inverse != screenInverse) {
                return getColor(attrs.fgColor, true, false, false);
            }
            return getColor(attrs.bgColor, false, false, false);
        }

        private boolean dirty = true;
        private boolean isOpaque = false;

        private void update() {
            if (dirty) {
                dirty = false;
                opacityEnd:
                {
                    for (final int c : basic) {
                        if ((c & 0xFF000000) != 0xFF000000) {
                            isOpaque = false;
                            break opacityEnd;
                        }
                    }
                    for (final int c : _8bit) {
                        if ((c & 0xFF000000) != 0xFF000000) {
                            isOpaque = false;
                            break opacityEnd;
                        }
                    }
                    isOpaque = true;
                }
            }
        }

        private void invalidate() {
            dirty = true;
        }

        @Override
        public boolean isOpaque() {
            update();
            return isOpaque;
        }

        @Override
        public int getDefaultFgNormal() {
            return basic[24];
        }

        @Override
        public void setDefaultFgNormal(final int color) {
            basic[24] = color;
            invalidate();
        }

        @Override
        public int getDefaultFgBold() {
            return basic[25];
        }

        @Override
        public void setDefaultFgBold(final int color) {
            basic[25] = color;
            invalidate();
        }

        @Override
        public int getDefaultFgFaint() {
            return basic[26];
        }

        @Override
        public void setDefaultFgFaint(final int color) {
            basic[26] = color;
            invalidate();
        }

        @Override
        public int getDefaultBg() {
            return basic[27];
        }

        @Override
        public void setDefaultBg(final int color) {
            basic[27] = color;
            invalidate();
        }

        @Override
        public int getBasicNormal(final int idx) {
            return basic[idx & 0x07];
        }

        @Override
        public void setBasicNormal(final int idx, final int color) {
            basic[idx & 0x07] = color;
            invalidate();
        }

        @Override
        public int getBasicBold(final int idx) {
            return basic[idx & 0x07 | 0x08];
        }

        @Override
        public void setBasicBold(final int idx, final int color) {
            basic[idx & 0x07 | 0x08] = color;
            invalidate();
        }

        @Override
        public int getBasicFaint(final int idx) {
            return basic[idx & 0x07 | 0x10];
        }

        @Override
        public void setBasicFaint(final int idx, final int color) {
            basic[idx & 0x07 | 0x10] = color;
            invalidate();
        }

        @Override
        public int get8bit(final int idx) {
            return _8bit[idx & 0xFF];
        }

        @Override
        public void set8bit(final int idx, final int color) {
            _8bit[idx & 0xFF] = color;
            invalidate();
        }
    }

    public static final AnsiColorProfile DEFAULT_COLOR_PROFILE = new TabularColorProfile();

    public static final class Color {
        private static final int DEFAULT = 0xFF;

        public enum Type {BASIC, _8BIT, TRUE}

        public int value = DEFAULT;
        @NonNull
        public Type type = Type.BASIC;

        public void set(@NonNull final Color color) {
            type = color.type;
            value = color.value;
        }

        @CheckResult(suggest = "#setDefault()")
        public boolean isDefault() {
            return type == Type.BASIC && value == DEFAULT;
        }

        public void setDefault() {
            type = Type.BASIC;
            value = DEFAULT;
        }
    }

    public final Color fgColor = new Color();
    public final Color bgColor = new Color();
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
        fgColor.setDefault();
        bgColor.setDefault();
        invisible = false;
        inverse = false;
        bold = false;
        faint = false;
        italic = false;
        underline = false;
        crossed = false;
        blinking = false;
    }

    public void set(@NonNull final ConsoleScreenCharAttrs aa) {
        fgColor.set(aa.fgColor);
        bgColor.set(aa.bgColor);
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
