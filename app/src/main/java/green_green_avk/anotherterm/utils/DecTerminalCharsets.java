package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.CharBuffer;

public final class DecTerminalCharsets {
    private DecTerminalCharsets() {
    }

    public static final class Table {
        public final int offset;
        public final int symLen;
        @NonNull
        public final char[] table;

        public Table(final int offset, final int symLen, @NonNull final char[] table) {
            this.offset = offset;
            this.symLen = symLen;
            this.table = table;
        }
    }

    public static final Table UK = new Table(0x23, 1, new char[]{
            0x00A3
    });

    public static final Table graphics = new Table(0x5f, 1, new char[]{
            0x00A0,
            0x25C6, 0x2592, 0x2409, 0x240C, 0x240D, 0x240A, 0x00B0, 0x00B1,
            0x2424, 0x240B, 0x2518, 0x2510, 0x250C, 0x2514, 0x253C, 0x23BA,
            0x23BB, 0x2500, 0x23BC, 0x23BD, 0x251C, 0x2524, 0x2534, 0x252C,
            0x2502, 0x2264, 0x2265, 0x03C0, 0x2260, 0x00A3, 0x00B7
    });

    public static final Table vt52Graphics = new Table(0x5e, 2, new char[]{
            0x00A0, 0, 0x00A0, 0,
            0xFFFD, 0, 0x2588, 0, 0x215F, 0, 0x00B3, 0x2044, 0x2075, 0x2044, 0x2077, 0x2044, 0x00B0, 0, 0x00B1, 0,
            0x2192, 0, 0x2026, 0, 0x00F7, 0, 0x2193, 0, 0x2594, 0, 0xD83E, 0xDF76, 0xD83E, 0xDF77, 0xD83E, 0xDF78,
            0xD83E, 0xDF79, 0xD83E, 0xDF7A, 0xD83E, 0xDF7B, 0x2581, 0, 0x2080, 0, 0x2081, 0, 0x2082, 0, 0x2083, 0,
            0x2084, 0, 0x2085, 0, 0x2086, 0, 0x2087, 0, 0x2088, 0, 0x2089, 0, 0x00B6, 0
    });

    @NonNull
    public static CharSequence translate(@NonNull final CharSequence b,
                                         @Nullable final Table table) {
        if (table == null) return b;
        final int len = b.length();
        final CharBuffer r = CharBuffer.allocate(len * table.symLen);
        final char[] ra = r.array();
        final int tEnd = table.table.length / table.symLen + table.offset;
        int j = 0;
        for (int i = 0; i < len; ++i) {
            final char v = b.charAt(i);
            if (v < table.offset || v >= tEnd) {
                ra[j] = v;
                j++;
                continue;
            }
            final int _v = (v - table.offset) * table.symLen;
            ra[j] = table.table[_v];
            j++;
            for (int k = 1; k < table.symLen; k++) {
                if (table.table[_v + k] == 0)
                    break;
                ra[j] = table.table[_v + k];
                j++;
            }
        }
        r.limit(j);
        return r;
    }
}
