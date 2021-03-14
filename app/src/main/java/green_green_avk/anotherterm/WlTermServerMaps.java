package green_green_avk.anotherterm;

import android.view.MotionEvent;

import green_green_avk.wayland.protocol.wayland.wl_pointer;

final class WlTermServerMaps {
    private WlTermServerMaps() {
    }

    static final class TranslationException extends RuntimeException {
        public TranslationException() {
        }

        public TranslationException(final String message) {
            super(message);
        }

        public TranslationException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public TranslationException(final Throwable cause) {
            super(cause);
        }
    }

    // As Linux evdev
    static final int BTN_LEFT = 0x110;
    static final int BTN_RIGHT = 0x111;
    static final int BTN_MIDDLE = 0x112;

    static int getPointerButtonId(final int v) {
        switch (v) {
            case MotionEvent.BUTTON_PRIMARY:
                return BTN_LEFT;
            case MotionEvent.BUTTON_SECONDARY:
                return BTN_RIGHT;
            case MotionEvent.BUTTON_TERTIARY:
                return BTN_MIDDLE;
        }
        throw new TranslationException("Bad pointer button ID: " + v);
    }

    static int getAxisId(final int v) {
        switch (v) {
            case MotionEvent.AXIS_VSCROLL:
                return wl_pointer.Enums.Axis.vertical_scroll;
            case MotionEvent.AXIS_HSCROLL:
                return wl_pointer.Enums.Axis.horizontal_scroll;
        }
        throw new TranslationException("Bad pointer axis ID: " + v);
    }

    // As Linux evdev
    private static final int[] keyIds = {
            -1, -1, -1, -1, -1, -1, -1,
            11, 2, 3, 4, 5, 6, 7, 8, 9, 10, // 0 - 9
            -1, -1, // * / #
            103, 108, 105, 106, -1, // UP / DOWN / LEFT / RIGHT / CENTER
            115, 114, -1, // VOLUME[UP/DOWN] / POWER
            -1, -1, // CAMERA / CLEAR
            //A   B   C   D   E   F   G   H   I   J   K   L   M   N   O   P   Q   R   S   T   U   V   W   X   Y   Z
            30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50, 49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44, // A - Z
            51, 52, // , / .
            56, 100, 42, 54, // ALT[L/R] / SHIFT[L/R]
            15, 57, -1, -1, -1, 28, 14, // TAB / SPACE / SYM / EXPLORER / ENVELOPE / ENTER / BACKSPACE
            41, 12, 13, 26, 27, 43, 39, 40, 53, // `-=[]\;'/
            -1, // @
            -1, -1, -1, -1, -1, -1, -1, // NUM / HEADSETHOOK / CAMERA_FOCUS / + / MENU / NOTIFICATION / SEARCH
            -1, -1, -1, -1, -1, -1, // MEDIA[PLAY_PAUSE / STOP / NEXT / PREVIOUS / REWIND / FAST_FORWARD]
            248, // MIC_MUTE
            104, 109, // PAGE[UP/DOWN]
            -1, -1, // PICTSYMBOLS / SWITCH_CHARSET
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // Game Controller
            1, 111,  // ESC / DELETE
            29, 97, 58, 70, 125, 126, -1, // CTRL[L/R] / CAPS_LOCK / SCROLL_LOCK / META[L/R] / FUNCTION
            99, 0x19B, // SYSRQ / BREAK
            102, 107, 110, // HOME / END / INSERT
            -1, // NAV_FORWARD
            -1, -1, -1, -1, -1, // MEDIA[PLAY / PAUSE / CLOSE / EJECT / RECORD]
            59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 87, 88, // F1 - F12
            69, 82, 79, 80, 81, 75, 76, 77, 71, 72, 73, 98, 55, 74, 78, 83, 121, 96, 117, 179, 180, // NUM_LOCK / NUM[0-9/*-+.,ENTER=()]
            113 // VOLUME_MUTE
    };

    static int getKeyId(final int keyCode) {
        if (keyCode < 0 || keyCode >= keyIds.length) return -1;
        return keyIds[keyCode];
    }
}
