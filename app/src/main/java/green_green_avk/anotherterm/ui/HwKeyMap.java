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

    public static final HwKeyMap DEFAULT = new HwKeyMap() {
        @Override
        public int getDevId(@NonNull KeyEvent event) {
            return -1;
        }

        @Override
        public int get(final int keycode, final int devId) {
            return KEYCODE_ACTION_DEFAULT;
        }
    };

    /**
     * Keyboard type classification implementation.
     *
     * @param event
     * @return if < 0, avoid mapping
     */
    public abstract int getDevId(@NonNull KeyEvent event);

    /**
     * Mapping implementation.
     *
     * @param keycode
     * @param devId
     * @return
     */
    public abstract int get(int keycode, int devId);

    public int get(@NonNull final KeyEvent event) {
        final int id = getDevId(event);
        return id < 0 ? KEYCODE_ACTION_DEFAULT : get(event.getKeyCode(), id);
    }
}
