package green_green_avk.anotherterm.utils;

import android.support.annotation.NonNull;

import java.nio.CharBuffer;

public final class CharsAutoSelector {
    private static final char F_ASCII = 0x100;

    private static final byte WORD = 15;
    private static final byte FIELD = 14;
    private static final byte SPACE = -1;

    private static final byte[] TABLE = new byte[0x200];

    static {
        TABLE[Character.DECIMAL_DIGIT_NUMBER] = WORD;
        TABLE[Character.LETTER_NUMBER] = WORD;
        TABLE[Character.OTHER_NUMBER] = WORD;
        TABLE[Character.UPPERCASE_LETTER] = WORD;
        TABLE[Character.LOWERCASE_LETTER] = WORD;
        TABLE[Character.MODIFIER_LETTER] = WORD;
        TABLE[Character.TITLECASE_LETTER] = WORD;
        TABLE[Character.OTHER_LETTER] = WORD;
        TABLE[Character.CONNECTOR_PUNCTUATION] = WORD;
        TABLE[Character.DASH_PUNCTUATION] = WORD;
        TABLE['.' | F_ASCII] = FIELD;
        TABLE[Character.SPACE_SEPARATOR] = SPACE;
        TABLE[Character.PARAGRAPH_SEPARATOR] = SPACE;
        TABLE[Character.LINE_SEPARATOR] = SPACE;
    }

    private static byte getCat(final char c) {
        byte cat = 0;
        if (c < 0x100) cat = TABLE[c | F_ASCII];
        if (cat == 0) cat = TABLE[Character.getType(c)];
        return cat;
    }

    private static boolean match(final char c, final byte cat) {
        return getCat(c) >= cat;
    }

    public static void select(@NonNull final char[] v, final int start, final int end,
                              final int ptr, @NonNull final int[] ret) {
        final byte cat = getCat(v[ptr]);
        if (cat < 0) {
            ret[0] = 0;
            ret[1] = end - 1;
            return;
        }
        int i;
        for (i = ptr; i >= start; --i) {
            if (!match(v[i], cat)) break;
        }
        ret[0] = i + 1;
        for (i = ptr; i < end; ++i) {
            if (!match(v[i], cat)) break;
        }
        ret[1] = i - 1;
    }

    public static void select(@NonNull final CharSequence v,
                              final int ptr, @NonNull final int[] ret) {
        if (v instanceof CharBuffer && ((CharBuffer) v).hasArray()) {
            final CharBuffer cb = (CharBuffer) v;
            select(cb.array(), cb.arrayOffset() + cb.position(),
                    cb.arrayOffset() + cb.limit(), ptr, ret);
        } else throw new IllegalArgumentException("Not a CharBuffer with array");
    }
}
