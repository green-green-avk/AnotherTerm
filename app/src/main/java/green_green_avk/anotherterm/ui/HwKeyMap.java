package green_green_avk.anotherterm.ui;

import android.view.KeyEvent;

import androidx.annotation.NonNull;

/**
 * A hardware key mapping base class.
 * {@link #get(int, int)}() can return:
 * {@link #KEYCODE_ACTION_DEFAULT} - application default behavior;
 * {@link #KEYCODE_ACTION_BYPASS} - do not use as a terminal key and enable keyboard default behavior;
 * {@link KeyEvent#KEYCODE_UNKNOWN} - do not use as a terminal key and disable keyboard default behavior;
 * <i>any other key code</i> - use as a terminal key and disable keyboard default behavior;
 * mapping to itself can be used to override default toggle terminal behavior.
 * <b>Example:</b> {@link KeyEvent#KEYCODE_ALT_LEFT} => {@link #KEYCODE_ACTION_DEFAULT} for <b>SCH-I415</b>.
 */
public abstract class HwKeyMap {
    public static final int KEYCODE_ACTION_DEFAULT = -1;
    public static final int KEYCODE_ACTION_BYPASS = -2;

    // Modifiers toggle modes:
    public static final int TOGGLE_NONE = 0;
    public static final int TOGGLE_ONESHOT = 1; // Till first non-modifier or same modifier key press.
    public static final int TOGGLE_ON_OFF = 2; // On / off by modifier key.

    public static final HwKeyMap DEFAULT = new HwKeyMap() {
        @Override
        public int getDevType(@NonNull KeyEvent event) {
            return -1;
        }

        @Override
        public int get(final int keycode, final int devType) {
            return KEYCODE_ACTION_DEFAULT;
        }

        @Override
        public int getToggleMode(final int keycode, final int devType) {
            return TOGGLE_NONE;
        }
    };

    /**
     * Keyboard type classification implementation.
     *
     * @param event
     * @return if < 0, avoid mapping
     */
    public abstract int getDevType(@NonNull KeyEvent event);

    /**
     * Mapping implementation.
     *
     * @param keycode
     * @param devType
     * @return
     */
    public abstract int get(int keycode, int devType);

    public int get(@NonNull final KeyEvent event) {
        final int id = getDevType(event);
        return id < 0 ? KEYCODE_ACTION_DEFAULT : get(event.getKeyCode(), id);
    }

    public abstract int getToggleMode(int keycode, int devType);

    public int getToggleMode(@NonNull final KeyEvent event) {
        final int id = getDevType(event);
        return id < 0 ? TOGGLE_NONE : getToggleMode(event.getKeyCode(), id);
    }
}
