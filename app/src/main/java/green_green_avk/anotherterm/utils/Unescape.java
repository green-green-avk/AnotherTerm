package green_green_avk.anotherterm.utils;

import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;
import org.apache.commons.text.translate.LookupTranslator;
import org.apache.commons.text.translate.OctalUnescaper;
import org.apache.commons.text.translate.UnicodeUnescaper;

import java.util.HashMap;
import java.util.Map;

public final class Unescape {
    private Unescape() {
    }

    private static final CharSequenceTranslator c_base;

    static {
        final Map<CharSequence, CharSequence> m = new HashMap<>();
        m.put("\\", "");
        c_base = new AggregateTranslator(
                new OctalUnescaper(),
                new UnicodeUnescaper(),
                new LookupTranslator(EntityArrays.invert(Escape.C_BASE)),
                new LookupTranslator(m)
        );
    }

    public static String c(final CharSequence v) {
        return c_base.translate(v);
    }
}
