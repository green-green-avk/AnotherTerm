package green_green_avk.anotherterm.utils;

import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.JavaUnicodeEscaper;
import org.apache.commons.text.translate.LookupTranslator;

import java.util.HashMap;
import java.util.Map;

public final class Escape {
    private Escape() {
    }

    static final Map<CharSequence, CharSequence> C_BASE;

    private static final CharSequenceTranslator c_base;

    static {
        C_BASE = new HashMap<>();
        C_BASE.put("\\", "\\\\");
        C_BASE.put("\u001B", "\\e");
        C_BASE.put("\u0007", "\\a");
        C_BASE.put("\b", "\\b");
        C_BASE.put("\t", "\\t");
        C_BASE.put("\n", "\\n");
        C_BASE.put("\u000B", "\\v");
        C_BASE.put("\f", "\\f");
        C_BASE.put("\r", "\\r");
        c_base = new AggregateTranslator(
                new LookupTranslator(C_BASE),
                JavaUnicodeEscaper.outsideOf(32, 0x7e)
        );
    }

    public static String c(final CharSequence v) {
        return c_base.translate(v);
    }
}
