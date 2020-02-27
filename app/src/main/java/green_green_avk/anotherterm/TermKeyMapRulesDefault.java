package green_green_avk.anotherterm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.KeyEvent;

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

    private static final class KeyMap {
        public final int appMode;
        public final String[] normal;
        public final String normalFmt;
        public final String[] app;
        public final String appFmt;

        public KeyMap(int am, String[] n, String nf, String[] a, String af) {
            appMode = am;
            normal = n;
            normalFmt = nf;
            app = a;
            appFmt = af;
        }
    }

    private static final SparseArray<KeyMap> keyCodes = new SparseArray<>();
    private static final Set<Integer> supportedKeys;

    private static String[] S(String v) {
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
        keyCodes.put(KeyEvent.KEYCODE_DEL, new KeyMap(DECBKM, new String[]{"\u007F", null, "\u001B\u007F"}, null, new String[]{"\b", null, "\u001B\b"}, null));

        keyCodes.put(KeyEvent.KEYCODE_PAGE_UP, new KeyMap(NONE, S("\u001B[5~"), "\u001B[5;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_PAGE_DOWN, new KeyMap(NONE, S("\u001B[6~"), "\u001B[6;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_MOVE_HOME, new KeyMap(CURSOR, S("\u001B[H"), "\u001B[1;%d~", S("\u001BOH"), null));
        keyCodes.put(KeyEvent.KEYCODE_MOVE_END, new KeyMap(CURSOR, S("\u001B[F"), "\u001B[4;%d~", S("\u001BOF"), null));
        keyCodes.put(KeyEvent.KEYCODE_INSERT, new KeyMap(NONE, S("\u001B[2~"), "\u001B[2;%d~", null, null));
        keyCodes.put(KeyEvent.KEYCODE_FORWARD_DEL, new KeyMap(NONE, S("\u001B[3~"), "\u001B[3;%d~", null, null));

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
        keyCodes.put(TermKeyMap.KEYCODE_SCROLL_SCREEN_UP, new KeyMap(CURSOR, S("\u001B[A"), null, S("\u001BOA"), null));
        keyCodes.put(TermKeyMap.KEYCODE_SCROLL_SCREEN_DOWN, new KeyMap(CURSOR, S("\u001B[B"), null, S("\u001BOB"), null));

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

    public static boolean contains(int code) {
        return keyCodes.indexOfKey(code) >= 0;
    }

    public static int getAppMode(int code) {
        final KeyMap m = keyCodes.get(code);
        if (m == null) return TermKeyMap.APP_MODE_DEFAULT;
        return m.appMode;
    }

    @Nullable
    public static String get(int code, int modifiers, int appMode) {
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
            if (modifiers < m.normal.length) {
                r = m.normal[modifiers];
            }
            if (r == null) {
                if (m.normalFmt != null) {
                    r = String.format(m.normalFmt, modifiers + 1);
                } else {
                    r = m.normal[modifiers & (m.normal.length - 1)];
                }
            }
        }
        return r;
    }
}
