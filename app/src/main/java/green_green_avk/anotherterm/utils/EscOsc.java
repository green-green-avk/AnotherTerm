package green_green_avk.anotherterm.utils;

import android.support.annotation.NonNull;

import java.nio.CharBuffer;

public final class EscOsc {
    public final String body;
    public final String[] args;

    public EscOsc(@NonNull final CharBuffer v) throws IllegalArgumentException {
        if (v.charAt(v.length() - 1) == '\\') {
            body = v.subSequence(2, v.length() - 2).toString();
        } else {
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
