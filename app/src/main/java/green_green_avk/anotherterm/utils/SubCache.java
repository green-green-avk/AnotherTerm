package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SubCache<T> implements Cache<T> {
    public interface KeyMapper {
        @NonNull
        Object map(@NonNull Object itemKey);
    }

    @NonNull
    private final Cache<T> base;
    @NonNull
    private final KeyMapper keyMapper;

    public SubCache(@NonNull final Cache<T> base, @NonNull final KeyMapper keyMapper) {
        this.base = base;
        this.keyMapper = keyMapper;
    }

    @Override
    @Nullable
    public T get(@NonNull final Object key) {
        return base.get(keyMapper.map(key));
    }

    @Override
    public void put(@NonNull final Object key, @NonNull final T item) {
        base.put(keyMapper.map(key), item);
    }

    @Override
    public void remove(@NonNull final Object key) {
        base.remove(keyMapper.map(key));
    }
}
