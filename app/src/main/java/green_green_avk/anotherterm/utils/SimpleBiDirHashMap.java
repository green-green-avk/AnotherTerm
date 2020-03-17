package green_green_avk.anotherterm.utils;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleBiDirHashMap<K, V> extends HashMap<K, V> {
    protected final Map<V, K> m_rev = new HashMap<>();
    public final Map<V, K> rev = Collections.unmodifiableMap(m_rev);

    @Nullable
    @Override
    public V put(final K key, final V value) {
        m_rev.put(value, key);
        return super.put(key, value);
    }

    @Nullable
    @Override
    public V remove(@Nullable final Object key) {
        final V v = super.remove(key);
        m_rev.remove(v);
        return v;
    }

    @Override
    public void clear() {
        super.clear();
        m_rev.clear();
    }
}
