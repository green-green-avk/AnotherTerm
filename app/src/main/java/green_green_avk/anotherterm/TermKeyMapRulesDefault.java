package green_green_avk.anotherterm;

import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class TermKeyMapRulesDefault {
    private TermKeyMapRulesDefault() {
    }

    private static final int NONE = TermKeyMap.APP_MODE_NONE;
    private static final int CURSOR = TermKeyMap.APP_MODE_CURSOR;
    private static final int NUMPAD = TermKeyMap.APP_MODE_NUMPAD;
    private static final int DECBKM = TermKeyMap.APP_MODE_DECBKM;

    private static final int VT52 = TermKeyMap.KEYCODES_VT52;

    private static final class KeyMap {
        public final int appMode;
        @Nullable
        public final String[] normal;
        @Nullable
        public final String normalFmt;
        @Nullable
        public final String[] app;
        @Nullable
        public final String appFmt;

        public KeyMap(final int am, @Nullable final String[] n, @Nullable final String nf,
                      @Nullable final String[] a, @Nullable final String af) {
            appMode = am;
            normal = n;
            normalFmt = nf;
            app = a;
            appFmt = af;
        }
    }

    private static final SparseArray<KeyMap> keyCodes = new SparseArray<>();
    private static final Set<Integer> supportedKeys;

    private static String[] S(final String v) {
        return new String[]{v};
    }

    static {
        keyCodes.put(KeyEvent.KEYCODE_ESCAPE, new KeyMap(NONE, new String[]{"\u001B", null, "\u001B\u001B"}, null, null, null));

        keyCodes.put(KeyEvent.KEYCODE_DPAD_UP, new KeyMap(CURSOR, S("\u001B[A"), "\u001B[1;%dA", S("\u001BOA"), null));
        keyCodes.put(KeyEvent.KEYCODE_DPAD_DOWN, new KeyMap(CURSOR, S("\u001B[B"), "\u001B[1;%dB", S("\u001BOB"), null));
        keyCodes.put(KeyEvent.KEYCODE_DPAD_LEFT, new KeyMap(CURSOR, S("\u001B[D"), "\u001B[1;%dD", S("\u001BOD"), null));
        keyCodes.put(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyMap(CURSOR, S("\u001B[C"), "\u001B[1;%dC", S("\u001BOC"), null));

        keyCodes.put(KeyEvent.KEYCODE_SPACE, new KeyMap(NONE, new String[]{" ", null, "\u001B ", null, "\0", null, "\u001B\0"}, null, S("\u001BO "), null));
        keyCodes.put(KeyEvent.KEYCODE_TAB, new KeyMap(NONE, new String[]{"\t", "\u001B[Z", "\u001B\t"}, null, S("\u001BOI"), null));
        keyCodes.put(KeyEvent.KEYCODE_ENTER, new KeyMap(NONE, new String[]{"\r", null, "\u001B\r"}, null, S("\u001BOM"), null));
        keyCodes.put(KeyEvent.KEYCODE_DEL, new KeyMap(DECBKM, new String[]{"\u007F", null, "\u001B\u007F"}, null, new String[]{"\b", "\b", "\u001B\b", "\u001B\b", "\b", "\b", "\u001B\b", "\u001B\b"}, null));

        keyCodes.put(KeyEvent.KEYCODE_PAGE_UP, new KeyMap(NONE, S("\u001B[5~"), "\u001B[5;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_PAGE_DOWN, new KeyMap(NONE, S("\u001B[6~"), "\u001B[6;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_MOVE_HOME, new KeyMap(CURSOR, S("\u001B[H"), "\u001B[1;%dH", S("\u001BOH"), null));
        keyCodes.put(KeyEvent.KEYCODE_MOVE_END, new KeyMap(CURSOR, S("\u001B[F"), "\u001B[1;%dF", S("\u001BOF"), null));
        keyCodes.put(KeyEvent.KEYCODE_INSERT, new KeyMap(NONE, S("\u001B[2~"), "\u001B[2;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_FORWARD_DEL, new KeyMap(NONE, S("\u001B[3~"), "\u001B[3;%d~", null, null));

        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_0, new KeyMap(NUMPAD, S("0"), null, S("\u001B[2~"), "\u001B[2;%d~"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_1, new KeyMap(NUMPAD, S("1"), null, S("\u001BOF"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_2, new KeyMap(NUMPAD, S("2"), null, S("\u001B[B"), "\u001B[1;%dB"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_3, new KeyMap(NUMPAD, S("3"), null, S("\u001B[6~"), "\u001B[6;%d~"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_4, new KeyMap(NUMPAD, S("4"), null, S("\u001B[D"), "\u001B[1;%dD"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_5, new KeyMap(NUMPAD, S("5"), null, S("\u001B[E"), "\u001B[2;%dE"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_6, new KeyMap(NUMPAD, S("6"), null, S("\u001B[C"), "\u001B[1;%dC"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_7, new KeyMap(NUMPAD, S("7"), null, S("\u001BOH"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_8, new KeyMap(NUMPAD, S("8"), null, S("\u001B[A"), "\u001B[1;%dA"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_9, new KeyMap(NUMPAD, S("9"), null, S("\u001B[5~"), "\u001B[5;%d~"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_DIVIDE, new KeyMap(NUMPAD, S("/"), null, S("\u001BOo"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, new KeyMap(NUMPAD, S("*"), null, S("\u001BOj"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, new KeyMap(NUMPAD, S("-"), null, S("\u001BOm"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_ADD, new KeyMap(NUMPAD, S("+"), null, S("\u001BOk"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_DOT, new KeyMap(NUMPAD, S("."), null, S("\u001B[3~"), "\u001B[3;%d~"));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_COMMA, new KeyMap(NUMPAD, S(","), null, S("\u001BOl"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_ENTER, new KeyMap(NUMPAD, new String[]{"\r", null, "\u001B\r"}, null, S("\u001BOM"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_EQUALS, new KeyMap(NUMPAD, S("="), null, S("\u001BOX"), null));

        keyCodes.put(KeyEvent.KEYCODE_F1, new KeyMap(NONE, S("\u001BOP"), "\u001B[1;%dP", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F2, new KeyMap(NONE, S("\u001BOQ"), "\u001B[1;%dQ", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F3, new KeyMap(NONE, S("\u001BOR"), "\u001B[1;%dR", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F4, new KeyMap(NONE, S("\u001BOS"), "\u001B[1;%dS", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F5, new KeyMap(NONE, S("\u001B[15~"), "\u001B[15;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F6, new KeyMap(NONE, S("\u001B[17~"), "\u001B[17;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F7, new KeyMap(NONE, S("\u001B[18~"), "\u001B[18;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F8, new KeyMap(NONE, S("\u001B[19~"), "\u001B[19;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F9, new KeyMap(NONE, S("\u001B[20~"), "\u001B[20;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F10, new KeyMap(NONE, S("\u001B[21~"), "\u001B[21;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F11, new KeyMap(NONE, S("\u001B[23~"), "\u001B[23;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_F12, new KeyMap(NONE, S("\u001B[24~"), "\u001B[24;%d~", null, null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_UP, new KeyMap(CURSOR, S("\u001B[A"), null, S("\u001BOA"), null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_DOWN, new KeyMap(CURSOR, S("\u001B[B"), null, S("\u001BOB"), null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_LEFT, new KeyMap(CURSOR, S("\u001B[D"), null, S("\u001BOD"), null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_RIGHT, new KeyMap(CURSOR, S("\u001B[C"), null, S("\u001BOC"), null));

        keyCodes.put(KeyEvent.KEYCODE_ESCAPE | VT52, new KeyMap(NONE, S("\u001B"), null, null, null));

        keyCodes.put(KeyEvent.KEYCODE_DPAD_UP | VT52, new KeyMap(CURSOR, S("\u001BA"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_DPAD_DOWN | VT52, new KeyMap(CURSOR, S("\u001BB"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_DPAD_LEFT | VT52, new KeyMap(CURSOR, S("\u001BD"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_DPAD_RIGHT | VT52, new KeyMap(CURSOR, S("\u001BC"), null, null, null));

        keyCodes.put(KeyEvent.KEYCODE_SPACE | VT52, new KeyMap(NONE, new String[]{" ", null, null, null, "\0"}, null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_TAB | VT52, new KeyMap(NONE, S("\t"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_ENTER | VT52, new KeyMap(NONE, S("\r"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_DEL | VT52, new KeyMap(DECBKM, S("\b"), null, null, null));

        keyCodes.put(KeyEvent.KEYCODE_FORWARD_DEL | VT52, new KeyMap(NONE, S("\u007F"), null, null, null));

        keyCodes.put(TermKeyMap.KEYCODE_LINEFEED | VT52, new KeyMap(NONE, S("\n"), null, null, null));

        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_0 | VT52, new KeyMap(NUMPAD, S("0"), null, S("\u001B?p"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_1 | VT52, new KeyMap(NUMPAD, S("1"), null, S("\u001B?q"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_2 | VT52, new KeyMap(NUMPAD, S("2"), null, S("\u001B?r"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_3 | VT52, new KeyMap(NUMPAD, S("3"), null, S("\u001B?s"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_4 | VT52, new KeyMap(NUMPAD, S("4"), null, S("\u001B?t"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_5 | VT52, new KeyMap(NUMPAD, S("5"), null, S("\u001B?u"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_6 | VT52, new KeyMap(NUMPAD, S("6"), null, S("\u001B?v"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_7 | VT52, new KeyMap(NUMPAD, S("7"), null, S("\u001B?w"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_8 | VT52, new KeyMap(NUMPAD, S("8"), null, S("\u001B?x"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_9 | VT52, new KeyMap(NUMPAD, S("9"), null, S("\u001B?y"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_DOT | VT52, new KeyMap(NUMPAD, S("."), null, S("\u001B?n"), null));
        keyCodes.put(KeyEvent.KEYCODE_NUMPAD_ENTER | VT52, new KeyMap(NUMPAD, S("\r"), null, S("\u001B?M"), null));

        keyCodes.put(KeyEvent.KEYCODE_F1 | VT52, new KeyMap(NONE, S("\u001BP"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_F2 | VT52, new KeyMap(NONE, S("\u001BQ"), null, null, null));
        keyCodes.put(KeyEvent.KEYCODE_F3 | VT52, new KeyMap(NONE, S("\u001BR"), null, null, null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_UP | VT52, new KeyMap(CURSOR, S("\u001BA"), null, null, null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_DOWN | VT52, new KeyMap(CURSOR, S("\u001BB"), null, null, null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_LEFT | VT52, new KeyMap(CURSOR, S("\u001BD"), null, null, null));
        keyCodes.put(TermKeyMap.KEYCODE_APP_SCROLL_RIGHT | VT52, new KeyMap(CURSOR, S("\u001BC"), null, null, null));

        final Set<Integer> _supportedKeys = new HashSet<>();
        for (int i = 0; i < keyCodes.size(); ++i) {
            _supportedKeys.add(keyCodes.keyAt(i));
        }
        supportedKeys = Collections.unmodifiableSet(_supportedKeys);
    }

    @NonNull
    public static Set<Integer> getSupportedKeys() {
        return supportedKeys;
    }

    public static boolean contains(final int code) {
        return keyCodes.indexOfKey(code) >= 0;
    }

    public static int getAppMode(final int code) {
        final KeyMap m = keyCodes.get(code);
        if (m == null) return TermKeyMap.APP_MODE_DEFAULT;
        return m.appMode;
    }

    @Nullable
    public static String get(final int code, final int modifiers, final int appMode) {
        final KeyMap m = keyCodes.get(code);
        if (m == null) return null;
        String r = null;
        if ((m.appMode & appMode) != 0) {
            if (m.app != null && modifiers < m.app.length) {
                r = m.app[modifiers];
            }
            if (r == null) {
                if (m.appFmt != null) {
                    r = String.format(m.appFmt, modifiers + 1);
                }
            }
        }
        if (r == null) {
            if (m.normal != null && modifiers < m.normal.length) {
                r = m.normal[modifiers];
            }
            if (r == null) {
                if (m.normalFmt != null) {
                    r = String.format(m.normalFmt, modifiers + 1);
                } else if (m.normal != null) {
                    r = m.normal[modifiers & (m.normal.length - 1)];
                }
            }
        }
        return r;
    }
}
