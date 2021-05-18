package green_green_avk.anotherterm;

import android.util.SparseIntArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.HwKeyMap;

public final class HwKeyMapTable extends HwKeyMap {
    public static final int BUILT_IN = 0;
    public static final int EXTERNAL = 1;

    private final SparseIntArray map = new SparseIntArray();
    private final SparseIntArray toggleModeMap = new SparseIntArray();

    private static int key(final int keycode, final int devId) {
        return (keycode & 0xFFFF) | (devId << 16);
    }

    @Override
    public int getDevType(@NonNull final KeyEvent event) {
        if (HwKeyMapManager.isVirtual(event)) return -1; // Of no use.
        return HwKeyMapManager.isExternal(event) ? EXTERNAL : BUILT_IN;
    }

    @Override
    public int get(final int keycode, final int devType) {
        if (devType < 0) return KEYCODE_ACTION_DEFAULT;
        return map.get(key(keycode, devType), KEYCODE_ACTION_DEFAULT);
    }

    @Override
    public int getToggleMode(final int keycode, final int devType) {
        if (devType < 0) return TOGGLE_NONE;
        return toggleModeMap.get(key(keycode, devType), TOGGLE_NONE);
    }

    public void set(final int keycode, final int devType, final int toKeycode) {
        if (devType < 0) return;
        if (toKeycode == KEYCODE_ACTION_DEFAULT) map.delete(key(keycode, devType));
        else map.put(key(keycode, devType), toKeycode);
    }

    public void setToggleMode(final int keycode, final int devType, final int toggleMode) {
        if (devType < 0) return;
        if (toggleMode == TOGGLE_NONE) toggleModeMap.delete(key(keycode, devType));
        else toggleModeMap.put(key(keycode, devType), toggleMode);
    }

    static final class Entry {
        public final int keycode;
        public final int devType;
        public final int toKeycode;
        public final int toggleMode;

        private Entry(final int key, final int toKeycode, final int toggleMode) {
            keycode = key & 0xFFFF;
            devType = key >> 16;
            this.toKeycode = toKeycode;
            this.toggleMode = toggleMode;
        }
    }

    /**
     * For UI.
     *
     * @param i
     * @return
     */
    @NonNull
    Entry getEntry(final int i) {
        final int key = map.keyAt(i);
        return new Entry(key, map.valueAt(i), toggleModeMap.get(key, TOGGLE_NONE));
    }

    /**
     * For UI.
     *
     * @return
     */
    int getSize() {
        return map.size();
    }

    /**
     * For storage.
     *
     * @return
     */
    @NonNull
    SparseIntArray getMap() {
        return map;
    }

    @NonNull
    SparseIntArray getToggleModeMap() {
        return toggleModeMap;
    }
}
