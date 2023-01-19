package green_green_avk.anotherterm.utils;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class HtmlUtils {
    private HtmlUtils() {
    }

    private static void withinStyle(@NonNull final StringBuilder out,
                                    @NonNull final CharSequence text,
                                    final int start, final int end) {
        for (int i = start; i < end; i++) {
            final char c = text.charAt(i);
            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }
                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }

    @Nullable
    public static String toHtml(@Nullable final CharSequence text) {
        if (text == null)
            return null;
        if (text instanceof Spanned) {
            return Html.toHtml((Spanned) text);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            final StringBuilder out = new StringBuilder();
            withinStyle(out, text, 0, text.length());
            return out.toString();
        }
        return Html.escapeHtml(text);
    }
}
