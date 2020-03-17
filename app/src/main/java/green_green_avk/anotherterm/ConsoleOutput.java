package green_green_avk.anotherterm;

import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.Locale;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.ExtKeyboard;

public final class ConsoleOutput {
    private Charset charset;
    private TermKeyMapRules keyMap;
    public EventBasedBackendModuleWrapper backendModule = null;

    public boolean appCursorKeys = false; // DECCKM
    public boolean appNumKeys = false; // DECNKM / DECKPNM / DECKPAM
    public boolean appDECBKM = false; // DECBKM
    public boolean keyAutorepeat = true; // DECARM
    public boolean bracketedPasteMode = false;
    public boolean mouseX10 = false;
    public boolean mouseX11 = false;
    public boolean mouseHighlight = false;
    public boolean mouseButtonEvent = false;
    public boolean mouseAnyEvent = false;
    public boolean mouseUTF8 = false;
    public boolean mouseSGR = false;
    public boolean mouseURXVT = false;

    public ConsoleOutput() {
        setCharset(Charset.defaultCharset());
        setKeyMap(TermKeyMapManager.defaultKeyMap);
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(final Charset ch) {
        charset = ch;
    }

    public TermKeyMapRules getKeyMap() {
        return keyMap;
    }

    public void setKeyMap(final TermKeyMapRules km) {
        keyMap = km;
    }

    @Nullable
    public String getKeySeq(final int code, final int modifiers) {
        final int appMode = (appCursorKeys ? TermKeyMap.APP_MODE_CURSOR : 0)
                | (appNumKeys ? TermKeyMap.APP_MODE_NUMPAD : 0)
                | (appDECBKM ? TermKeyMap.APP_MODE_DECBKM : 0);
        return keyMap.get(code, modifiers, appMode);
    }

    public void feed(final int code, final boolean shift, final boolean alt, final boolean ctrl) {
        if (code == ExtKeyboard.KEYCODE_NONE) return;
        if (code < 0) {
            char c = (char) -code;
            if (ctrl) {
                if ((c & 0x40) != 0) {
                    c &= 0x1F;
                } else if (c == 0x3F) {
                    c = 0x7F;
                }
            }
            if (alt) {
                feed("\u001B" + c);
                return;
            }
            feed(Character.toString(c));
            return;
        }
        final int modifiers =
                ((shift ? 1 : 0) |
                        (alt ? 2 : 0) |
                        (ctrl ? 4 : 0));
        final String r = getKeySeq(code, modifiers);
        if (r != null) feed(r);
    }

    public void feed(@NonNull final KeyEvent event) {
        final int code = event.getKeyCode();
        final int modifiers =
                ((event.isShiftPressed() ? 1 : 0) |
                        (event.isAltPressed() ? 2 : 0) |
                        (event.isCtrlPressed() ? 4 : 0));
        final String r = getKeySeq(code, modifiers);
        if (r != null) {
            feed(r);
            return;
        }
        final char c = (char) event.getUnicodeChar(event.getMetaState() & KeyEvent.META_SHIFT_MASK);
        if (c != 0) {
            feed(-c, event.isShiftPressed(), event.isAltPressed(), event.isCtrlPressed());
        }
    }

    public void feed(@NonNull final String v) {
        if (backendModule != null) {
            backendModule.write(v.getBytes(charset));
        }
    }

    public void paste(@NonNull final String v) {
        feed(bracketedPasteMode ? "\u001B[200~" + v + "\u001B[201~" : v);
    }

    public void vScroll(int lines) {
        if (lines == 0) return;
        if (lines < 0) while (lines++ < 0)
            feed(TermKeyMap.KEYCODE_SCROLL_SCREEN_UP, false, false, false);
        else while (lines-- > 0)
            feed(TermKeyMap.KEYCODE_SCROLL_SCREEN_DOWN, false, false, false);
    }

    public boolean isMouseSupported() {
        return mouseSGR || mouseX11 || mouseX10;
    }

    public void unsetMouse() {
        mouseURXVT = false;
        mouseSGR = false;
        mouseUTF8 = false;
        mouseX11 = false;
        mouseX10 = false;
    }

    public enum MouseEventType {PRESS, RELEASE, MOVE, VSCROLL}

    public static final int MOUSE_LEFT = MotionEvent.BUTTON_PRIMARY;
    public static final int MOUSE_RIGHT = MotionEvent.BUTTON_SECONDARY;
    public static final int MOUSE_MIDDLE = MotionEvent.BUTTON_TERTIARY;

    public void feed(final MouseEventType type, final int buttons, final int x, final int y) {
        if (!isMouseSupported()) return;
        feed(type, buttons, x, y, false, false, false); // TODO: fix
    }

    public void feed(final MouseEventType type, final int buttons, final int x, final int y,
                     final boolean shift, final boolean alt, final boolean ctrl) {
        if (!isMouseSupported()) return;
        final boolean wheel = type == MouseEventType.VSCROLL;
        if (wheel && buttons == 0) return;
        final int button = (!mouseSGR && type == MouseEventType.RELEASE) ? 3 :
                (wheel && buttons > 0) ? 64 :
                        (wheel && buttons < 0) ? 65 :
                                (buttons & MOUSE_LEFT) != 0 ? 0 :
                                        (buttons & MOUSE_RIGHT) != 0 ? 2 :
                                                (buttons & MOUSE_MIDDLE) != 0 ? 1 : 3;
        final int modifiers =
                ((shift ? 4 : 0) |
                        (alt ? 8 : 0) |
                        (ctrl ? 16 : 0));
        int code = 0;
        if (type == MouseEventType.MOVE) code += 32;
        if (mouseSGR) { // TODO: Add some outdated protocols...
            String out = String.format(Locale.ROOT, "\u001B[<%d;%d;%d%c",
                    code + button + modifiers, x + 1, y + 1,
                    type == MouseEventType.RELEASE ? 'm' : 'M');
            if (wheel) out = StringUtils.repeat(out, Math.min(Math.abs(buttons), 32));
            feed(out);
        } else if ((mouseX11 && type != MouseEventType.MOVE)
                || (mouseButtonEvent && !(type == MouseEventType.MOVE && button == 3))
                || mouseAnyEvent) {
            String out = String.format(Locale.ROOT, "\u001B[M%c%c%c",
                    code + button + modifiers + 32, x + 33, y + 33);
            if (wheel) out = StringUtils.repeat(out, Math.min(Math.abs(buttons), 32));
            feed(out);
        } else if (mouseX10 && (type == MouseEventType.PRESS || wheel)) {
            String out = String.format(Locale.ROOT, "\u001B[M%c%c%c",
                    code + button + 32, x + 33, y + 33);
            if (wheel) out = StringUtils.repeat(out, Math.min(Math.abs(buttons), 32));
            feed(out);
        }
    }
}
