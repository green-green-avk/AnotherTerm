package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public abstract class DynamicResourceSource<T> {
    @NonNull
    protected abstract Cache<T> getCache();

    @NonNull
    protected abstract T onLoad(@NonNull Object key) throws Exception;

    public void invalidate(@NonNull final Object key) {
        getCache().remove(key);
    }

    @NonNull
    public abstract Set<?> enumerate();

    @NonNull
    public T get(@NonNull final Object key) throws Exception {
        T v = getCache().get(key);
        if (v != null) {
            return v;
        }
        v = onLoad(key);
        getCache().put(key, v);
        return v;
    }

    public interface OnChanged {
        void onChanged();
    }

    @Nullable
    private OnChanged onChanged = null;

    protected final void callOnChanged() {
        if (onChanged != null) {
            onChanged.onChanged();
        }
    }

    public void setOnChanged(@Nullable final OnChanged v) {
        onChanged = v;
    }
}
