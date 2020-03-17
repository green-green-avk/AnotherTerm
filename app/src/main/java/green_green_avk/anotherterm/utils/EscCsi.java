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

    public EscCsi(@NonNull final CharBuffer v) throws IllegalArgumentException {
        final int end_i = v.length() - 1;
        type = v.charAt(end_i);
        int suf_i;
        for (suf_i = end_i - 1; suf_i >= 0; suf_i--) {
            final char c = v.charAt(suf_i);
            if (c < 0x20 || c > 0x2F) break;
        }
        suf_i++;
        if (suf_i == end_i) suffix = "";
        else suffix = v.subSequence(suf_i, end_i).toString();
        final char pre = v.charAt(2);
        if (pre >= 60 && pre <= 63) {
            prefix = pre;
            body = v.subSequence(3, suf_i).toString();
        } else {
            prefix = 0;
            body = v.subSequence(2, suf_i).toString();
        }
        args = body.split(";");
    }

    public int getIntArg(final int n, final int def) {
        if (args.length <= n) return def;
        try {
            return Integer.parseInt(args[n]);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
