package green_green_avk.anotherterm.ui;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;

public final class KeyIntervalDetector {
    private final LongSparseArray<Long> ts = new LongSparseArray<>();

    private static long key(@NonNull final KeyEvent event) {
        return event.getKeyCode() | ((long) event.getDeviceId() << 32);
    }

    /**
     * @param event Event to register.
     * @return Interval since previous event of this key and device (-1 for no previous event).
     */
    public long sample(@NonNull final KeyEvent event) {
        final long ct = event.getEventTime();
        final long key = key(event);
        final int idx = ts.indexOfKey(key);
        if (idx < 0) {
            ts.put(key, ct);
            return -1;
        }
        final long r = ct - ts.valueAt(idx);
        ts.setValueAt(idx, ct);
        return r;
    }
}
