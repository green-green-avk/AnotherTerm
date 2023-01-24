package green_green_avk.anotherterm.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConstBiDiMapping<K, V> {
    protected final Map<K, V> mFw = new HashMap<>();
    public final Map<K, V> fw = Collections.unmodifiableMap(mFw);
    protected final Map<V, K> mRev = new HashMap<>();
    public final Map<V, K> rev = Collections.unmodifiableMap(mRev);

    public void put(final K key, final V value) {
        mFw.put(key, value);
        mRev.put(value, key);
    }
}
