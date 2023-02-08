package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SubCache<T> implements Cache<T> {
    protected abstract @NonNull Object onMapKey(@NonNull Object itemKey);

    @NonNull
    private final Cache<T> base;

    public SubCache(@NonNull final Cache<T> base) {
        this.base = base;
    }

    @Override
    @Nullable
    public T get(@NonNull final Object key) {
        return base.get(onMapKey(key));
    }

    @Override
    public void put(@NonNull final Object key, @NonNull final T item) {
        base.put(onMapKey(key), item);
    }

    @Override
    public void remove(@NonNull final Object key) {
        base.remove(onMapKey(key));
    }
}
