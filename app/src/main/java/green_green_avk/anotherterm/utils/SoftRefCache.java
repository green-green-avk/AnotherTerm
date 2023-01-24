package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SoftRefCache<T> implements Cache<T> {
    private static final class Entry<T> extends SoftReference<T> {
        @NonNull
        private final Object key;

        private Entry(@NonNull final T referent, @NonNull final Object key,
                      final ReferenceQueue<? super T> q) {
            super(referent, q);
            this.key = key;
        }
    }

    private final ReferenceQueue<Object> expired = new ReferenceQueue<>();
    private final Map<Object, Entry<T>> cache = new HashMap<>();

    private void purge() {
        Entry<T> entry;
        while ((entry = (Entry<T>) expired.poll()) != null) {
            cache.remove(entry.key);
        }
    }

    public int size() {
        purge();
        return cache.size();
    }

    @NonNull
    public Set<Object> enumerate() {
        purge();
        return cache.keySet();
    }

    @Override
    @Nullable
    public T get(@NonNull final Object key) {
        purge();
        final Entry<T> v = cache.get(key);
        return v != null ? v.get() : null;
    }

    @Override
    public void put(@NonNull final Object key, @NonNull final T item) {
        purge();
        cache.put(key, new Entry<>(item, key, expired));
    }

    @Override
    public void remove(@NonNull final Object key) {
        purge();
        cache.remove(key);
    }
}
