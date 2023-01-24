package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface Cache<T> {
    @Nullable
    T get(@NonNull Object key);

    void put(@NonNull Object key, @NonNull T item);

    void remove(@NonNull Object key);
}
