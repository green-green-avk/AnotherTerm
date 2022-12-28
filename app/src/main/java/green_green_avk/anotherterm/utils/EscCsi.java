package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.nio.CharBuffer;

public final class EscCsi {
    public final char type;
    public final char prefix;
    @NonNull
    public final String body;
    @NonNull
    public final String[] args;
    public final String suffix;

    /**
     * @param v to parse
     * @throws IllegalArgumentException if malformed
     */
    public EscCsi(@NonNull final CharBuffer v) {
        final int end_i = v.length() - 1;
        type = v.charAt(end_i);
        int suf_i;
        for (suf_i = end_i - 1; suf_i >= 0; suf_i--) {
            final char c = v.charAt(suf_i);
            if (c < 0x20 || c > 0x2F)
                break;
        }
        suf_i++;
        suffix = suf_i == end_i ? "" :
                Compat.subSequence(v, suf_i, end_i).toString();
        final int skip = v.charAt(0) == '\u001B' ? 2 : 1; // 7-bit / 8-bit
        final char pre = v.charAt(skip);
        if (pre >= 60 && pre <= 63) {
            prefix = pre;
            body = Compat.subSequence(v, skip + 1, suf_i).toString();
        } else {
            prefix = 0;
            body = Compat.subSequence(v, skip, suf_i).toString();
        }
        args = body.split(";", -1);
    }

    public int getIntArg(final int n, final int def) {
        return getIntArg(args, n, def);
    }

    public static int getIntArg(@NonNull final String[] args, final int n, final int def) {
        if (args.length <= n)
            return def;
        try {
            return Integer.parseInt(args[n]);
        } catch (final NumberFormatException e) {
            return def;
        }
    }
}
