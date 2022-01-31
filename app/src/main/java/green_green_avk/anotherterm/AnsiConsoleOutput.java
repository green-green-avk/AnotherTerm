package green_green_avk.anotherterm;

import android.view.MotionEvent;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.ExtKeyboard;
import green_green_avk.anotherterm.utils.Misc;

public final class AnsiConsoleOutput {
    public enum MouseTracking {NONE, X10, X11, HIGHLIGHT, BUTTON_EVENT, ANY_EVENT}

    public enum MouseProtocol {NORMAL, SGR, URXVT, UTF8}

    @NonNull
    private Charset charset = Charset.defaultCharset();
    private boolean _8BitMode = false;
    private boolean _vt52 = false;
    @NonNull
    private TermKeyMapRules keyMap = TermKeyMapManager.defaultKeyMap;
    @Nullable
    public EventBasedBackendModuleWrapper backendModule = null;

    public boolean appCursorKeys = false; // DECCKM
    public boolean appNumKeys = false; // DECNKM / DECKPNM / DECKPAM
    public boolean appDECBKM = false; // DECBKM
    public boolean keyAutorepeat = true; // DECARM
    public boolean bracketedPasteMode = false;
    @NonNull
    public MouseTracking mouseTracking = MouseTracking.NONE;
    @NonNull
    public MouseProtocol mouseProtocol = MouseProtocol.NORMAL;
    public boolean mouseFocusInOut = false;

    @NonNull
    public Charset getCharset() {
        return charset;
    }

    public void setCharset(@NonNull final Charset ch) {
        charset = ch;
    }

    public boolean get8BitMode() {
        return _8BitMode;
    }

    public void set8BitMode(final boolean v) {
        _8BitMode = v;
    }

    public boolean getVt52() {
        return _vt52;
    }

    public void setVt52(final boolean _vt52) {
        this._vt52 = _vt52;
    }

    @NonNull
    public TermKeyMapRules getKeyMap() {
        return keyMap;
    }

    public void setKeyMap(@NonNull final TermKeyMapRules km) {
        keyMap = km;
    }

    @NonNull
    public String csi() {
        return _8BitMode && !_vt52 ? "\u009B" : "\u001B[";
    }

    @NonNull
    public String st() {
        return _8BitMode && !_vt52 ? "\u009C" : "\u001B\\";
    }

    @NonNull
    public String osc() {
        return _8BitMode && !_vt52 ? "\u009D" : "\u001B]";
    }

    @NonNull
    private String fixC1(@NonNull final String v) {
        if (_vt52 || !_8BitMode)
            return v;
        int p;
        p = v.indexOf('\u001B');
        if (p < 0 || p + 1 == v.length())
            return v;
        final StringBuilder sb = new StringBuilder(v.length());
        int pp = 0;
        do {
            sb.append(v, pp, p);
            p++;
            if (p >= v.length())
                return sb.append('\u001B').toString();
            final char c = v.charAt(p);
            if (c >= 0x40 && c < 0x60) {
                sb.append((char) (c + 0x40));
                p++;
                pp = p;
            } else
                pp = p - 1;
            p = v.indexOf('\u001B', p);
        } while (p >= 0);
        return sb.append(v, pp, v.length()).toString();
    }

    @Nullable
    private String getKeySeq(final int code, final int modifiers) {
        final int appMode = (appCursorKeys ? TermKeyMap.APP_MODE_CURSOR : 0)
                | (appNumKeys ? TermKeyMap.APP_MODE_NUMPAD : 0)
                | (appDECBKM ? TermKeyMap.APP_MODE_DECBKM : 0);
        final String r = keyMap.get(_vt52 ? code | TermKeyMap.KEYCODES_VT52 : code,
                modifiers, appMode);
        return r != null ? fixC1(r) : null;
    }

    public boolean getKeyAutorepeat() {
        return keyAutorepeat;
    }

    @AnyRes
    public int getLayoutRes() {
        return _vt52 ? R.array.vt52_keyboard : R.array.ansi_keyboard;
    }

    @Nullable
    public String getKeySeq(final int code,
                            final boolean shift, final boolean alt, final boolean ctrl) {
        return getKeySeq(code, (shift ? 1 : 0) | (alt ? 2 : 0) | (ctrl ? 4 : 0));
    }

    public void feed(final int code, final boolean shift, final boolean alt, final boolean ctrl) {
        if (code == ExtKeyboard.KEYCODE_NONE) return;
        if (code < 0) {
            char c = (char) -code;
            if (c < 0x80) {
                if (ctrl) {
                    if (c >= 0x40) {
                        c &= 0x1F;
                    } else if (c == ' ') { // Mapped earlier; added just in case.
                        c = 0x00;
                    } else if (c == '/') { // xterm
                        c = 0x1F;
                    } else if (c == '?') {
                        c = 0x7F;
                    }
                }
            }
            if (alt) {
                feed("\u001B" + c);
                return;
            }
            feed(Character.toString(c));
            return;
        }
        final String r = getKeySeq(code, shift, alt, ctrl);
        if (r != null) feed(r);
    }

    public void feedEsc(@NonNull final String v) {
        if (backendModule != null) {
            backendModule.write(fixC1(v).getBytes(charset));
        }
    }

    public void feed(@NonNull final String v) {
        if (backendModule != null) {
            backendModule.write(v.getBytes(charset));
        }
    }

    public void feed(@NonNull final byte[] v) {
        if (backendModule != null) {
            backendModule.write(v);
        }
    }

    public void paste(@NonNull final String v) {
        feed(bracketedPasteMode ? csi() + "200~" + v + csi() + "201~" : v);
    }

    public void vScroll(int rows) {
        if (rows == 0) return;
        if (rows < 0) while (rows++ < 0)
            feed(TermKeyMap.KEYCODE_APP_SCROLL_UP, false, false, false);
        else while (rows-- > 0)
            feed(TermKeyMap.KEYCODE_APP_SCROLL_DOWN, false, false, false);
    }

    public void hScroll(int cols) {
        if (cols == 0) return;
        if (cols < 0) while (cols++ < 0)
            feed(TermKeyMap.KEYCODE_APP_SCROLL_LEFT, false, false, false);
        else while (cols-- > 0)
            feed(TermKeyMap.KEYCODE_APP_SCROLL_RIGHT, false, false, false);
    }

    public boolean isMouseSupported() {
        return mouseTracking != MouseTracking.NONE && mouseTracking != MouseTracking.HIGHLIGHT &&
                !_vt52;
    }

    private boolean mHasMouseFocus = false;

    public boolean hasMouseFocus() {
        return mHasMouseFocus;
    }

    public void setMouseFocus(final boolean v) {
        if (v == mHasMouseFocus)
            return;
        mHasMouseFocus = v;
        if (mouseFocusInOut && !_vt52)
            if (v)
                feed(csi() + "I");
            else
                feed(csi() + "O");
    }

    public enum MouseEventType {PRESS, RELEASE, MOVE, VSCROLL}

    public static final int MOUSE_LEFT = MotionEvent.BUTTON_PRIMARY;
    public static final int MOUSE_RIGHT = MotionEvent.BUTTON_SECONDARY;
    public static final int MOUSE_MIDDLE = MotionEvent.BUTTON_TERTIARY;

    public void feed(@NonNull final MouseEventType type,
                     final int buttons, final int x, final int y) {
        feed(type, buttons, x, y, false, false, false); // TODO: fix
    }

    public void feed(@NonNull final MouseEventType type,
                     final int buttons, final int x, final int y,
                     final boolean shift, final boolean alt, final boolean ctrl) {
        if (!isMouseSupported())
            return;
        if (mouseTracking == MouseTracking.X10 && type != MouseEventType.PRESS)
            return;
        final boolean wheel = type == MouseEventType.VSCROLL;
        if (wheel && buttons == 0)
            return;
        final int code;
        if (type == MouseEventType.MOVE) {
            if ((mouseTracking != MouseTracking.BUTTON_EVENT || buttons == 0)
                    && mouseTracking != MouseTracking.ANY_EVENT)
                return;
            code = 32;
        } else
            code = 0;
        final int button =
                ((mouseProtocol == MouseProtocol.NORMAL || mouseProtocol == MouseProtocol.URXVT)
                        && type == MouseEventType.RELEASE) ? 3 :
                        (wheel && buttons > 0) ? 64 :
                                (wheel && buttons < 0) ? 65 :
                                        (buttons & MOUSE_LEFT) != 0 ? 0 :
                                                (buttons & MOUSE_RIGHT) != 0 ? 2 :
                                                        (buttons & MOUSE_MIDDLE) != 0 ? 1 : 3;
        final int modifiers = (mouseTracking == MouseTracking.X10) ? 0 :
                ((shift ? 4 : 0) |
                        (alt ? 8 : 0) |
                        (ctrl ? 16 : 0));
        byte[] out;
        switch (mouseProtocol) {
            case NORMAL: {
                if (x < 0 || x > 222 || y < 0 || y > 222)
                    return;
                out = ArrayUtils.addAll((csi() + "M").getBytes(charset), // or ASCII?
                        (byte) (code + button + modifiers + 32),
                        (byte) (x + 33), (byte) (y + 33));
                break;
            }
            case SGR:
                if (x < 0 || x > 9999 || y < 0 || y > 9999)
                    return;
                out = String.format(Locale.ROOT, csi() + "<%d;%d;%d%c",
                        code + button + modifiers, x + 1, y + 1,
                        type == MouseEventType.RELEASE ? 'm' : 'M').getBytes(charset);
                break;
            case URXVT:
                if (x < 0 || x > 9999 || y < 0 || y > 9999)
                    return;
                out = String.format(Locale.ROOT, csi() + "%d;%d;%dM",
                        code + button + modifiers + 32, x + 1, y + 1).getBytes(charset);
                break;
            case UTF8: {
                if (x < 0 || y < 0)
                    return;
                final byte[] buf = new byte[6];
                int p = 0;
                try {
                    p += utf8Encode(buf, p, code + button + modifiers + 32);
                    p += utf8Encode(buf, p, x + 33);
                    p += utf8Encode(buf, p, y + 33);
                } catch (final IllegalArgumentException e) {
                    return;
                }
                final byte[] prefix = (csi() + "M").getBytes(charset); // or UTF8?
                out = Arrays.copyOf(prefix, prefix.length + p);
                System.arraycopy(buf, 0, out, prefix.length, p);
                break;
            }
            default:
                return;
        }
        if (wheel) out = Misc.repeat(out, Math.min(Math.abs(buttons), 32));
        feed(out);
    }

    private static int utf8Encode(@NonNull final byte[] buf, final int p, final int v) {
        if (v < 0 || v > 2047) throw new IllegalArgumentException();
        if (v < 128) {
            buf[p] = (byte) v;
            return 1;
        }
        buf[p] = (byte) ((v >> 6) & 0x1F | 0xC0);
        buf[p + 1] = (byte) (v & 0x3F | 0x80);
        return 2;
    }
}
