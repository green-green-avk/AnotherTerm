package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/**
 * A common way to load resources from different sources.
 *
 * @param <T> can be a resource itself or some provider to get for
 *            a particular {@link android.content.Context}, etc.
 */
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

        void onChanged(@NonNull Object key);
    }

    @Nullable
    private OnChanged onChanged = null;

    protected final void callOnChanged() {
        if (onChanged != null) {
            onChanged.onChanged();
        }
    }

    protected final void callOnChanged(@NonNull final Object key) {
        if (onChanged != null) {
            onChanged.onChanged(key);
        }
    }

    public final void setOnChanged(@Nullable final OnChanged v) {
        onChanged = v;
    }

    public void recycle() {
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }
}
