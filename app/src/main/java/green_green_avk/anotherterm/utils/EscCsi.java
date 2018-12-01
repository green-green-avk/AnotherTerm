package green_green_avk.anotherterm.utils;

import android.support.annotation.NonNull;

import java.nio.CharBuffer;

public final class EscCsi {
    public final char type;
    public final char prefix;
    public final String body;
    public final String[] args;

    public EscCsi(@NonNull final CharBuffer v) throws IllegalArgumentException {
        type = v.charAt(v.length() - 1);
        final char pre = v.charAt(2);
        if (pre >= 60 && pre <= 63) {
            prefix = pre;
            body = v.subSequence(3, v.length() - 1).toString();
        } else {
            prefix = 0;
            body = v.subSequence(2, v.length() - 1).toString();
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
