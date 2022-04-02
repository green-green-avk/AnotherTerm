package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.nio.CharBuffer;

public final class EscOsc {
    public final String body;
    public final String[] args;

    public EscOsc(@NonNull final CharBuffer v) throws IllegalArgumentException {
        final int skip = v.charAt(0) == '\u001B' ? 2 : 1; // 7-bit / 8-bit
        if (v.charAt(v.length() - 2) == '\u001B') {
            body = Compat.subSequence(v, skip, v.length() - 2).toString();
        } else {
            body = Compat.subSequence(v, skip, v.length() - 1).toString();
        }
        args = body.split(";", -1);
    }

    public int getIntArg(final int n, final int def) {
        if (args.length <= n) return def;
        try {
            return Integer.parseInt(args[n]);
        } catch (final NumberFormatException e) {
            return def;
        }
    }
}
