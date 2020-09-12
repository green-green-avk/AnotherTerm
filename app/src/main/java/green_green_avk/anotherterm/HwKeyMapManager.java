package green_green_avk.anotherterm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import java.util.Map;

import green_green_avk.anotherterm.ui.HwKeyMap;

public final class HwKeyMapManager {
    private HwKeyMapManager() {
    }

    private static SharedPreferences map = null;

    public static void init(@NonNull final Context context) {
        final Context ac = context.getApplicationContext();
        map = ac.getSharedPreferences(ac.getPackageName() + "_inputkeymap_custom",
                Context.MODE_PRIVATE);
    }

    private static HwKeyMap cache = null;

    private static HwKeyMap load() {
        if (cache != null) return cache;
        final Map<String, ?> m = map.getAll();
        final HwKeyMapTable r = new HwKeyMapTable();
        for (final Map.Entry<String, ?> t : m.entrySet()) {
            if (!(t.getValue() instanceof Integer)) continue;
            final int keycode = (int) (Integer) t.getValue();
            final int key;
            try {
                key = Integer.parseInt(t.getKey());
            } catch (final NumberFormatException e) {
                continue;
            }
            r.getMap().put(key, keycode);
        }
        cache = r;
        return r;
    }

    private static void save(@NonNull final HwKeyMapTable km) {
        final SharedPreferences.Editor editor = map.edit();
        editor.clear();
        final SparseIntArray m = km.getMap();
        for (int i = 0; i < m.size(); i++) {
            editor.putInt(Integer.toString(m.keyAt(i)), m.valueAt(i));
        }
        editor.apply();
        cache = null;
    }

    public static HwKeyMap get() {
        return load();
    }

    public static void set(@NonNull final HwKeyMap hwKeyMap) {
        if (hwKeyMap instanceof HwKeyMapTable) save((HwKeyMapTable) hwKeyMap);
        else throw new IllegalArgumentException("Unable to process this HwKeyMap type");
    }

    public static boolean isBypassKey(@NonNull final KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_ANY & (
                InputDevice.SOURCE_MOUSE
                        | InputDevice.SOURCE_STYLUS
                        | InputDevice.SOURCE_TRACKBALL
        )) != 0) return true; // Mouse right & middle buttons...
        if (event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD) return event.isSystem();
        if (event.getDeviceId() <= KeyCharacterMap.SPECIAL_FUNCTION)
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                case KeyEvent.KEYCODE_VOLUME_UP: // Font size.
                case KeyEvent.KEYCODE_VOLUME_DOWN: // Font size.
                case KeyEvent.KEYCODE_POWER:
                    return true; // Don't prevent default behavior.
            }
        return false;
    }
}
