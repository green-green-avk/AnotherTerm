package green_green_avk.anotherterm;

import android.util.SparseIntArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.HwKeyMap;

public final class HwKeyMapTable extends HwKeyMap {
    public static final int BUILT_IN = 0;
    public static final int EXTERNAL = 1;
    public static final int DEV_IDS_NUM = 2;

    private final SparseIntArray map = new SparseIntArray();

    private int key(final int keycode, final int devId) {
        return (keycode & 0xFFFF) | ((devId & 0xFFFF) << 16);
    }

    @Override
    public int getDevId(@NonNull final KeyEvent event) {
        if (event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD) return -1; // Of no use.
        return event.getDeviceId() <= KeyCharacterMap.SPECIAL_FUNCTION ? BUILT_IN : EXTERNAL;
    }

    @Override
    public int get(final int keycode, final int devId) {
        if (devId < 0) return KEYCODE_ACTION_DEFAULT;
        return map.get(key(keycode, devId), KEYCODE_ACTION_DEFAULT);
    }

    public void set(final int keycode, final int devId, final int toKeycode) {
        if (devId < 0) return;
        if (toKeycode == KEYCODE_ACTION_DEFAULT) map.delete(key(keycode, devId));
        else map.put(key(keycode, devId), toKeycode);
    }

    static final class Entry {
        public final int keycode;
        public final int devId;
        public final int toKeycode;

        private Entry(final int key, final int toKeycode) {
            keycode = key & 0xFFFF;
            devId = key >>> 16;
            this.toKeycode = toKeycode;
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
        return new Entry(map.keyAt(i), map.valueAt(i));
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
}
