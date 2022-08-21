package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public final class WeakBlockingSync<T> {
    @NonNull
    private WeakReference<T> mValue = new WeakReference<>(null);

    @NonNull
    public synchronized T get() throws InterruptedException {
        T v;
        while ((v = mValue.get()) == null)
            wait();
        return v;
    }

    @Nullable
    public synchronized T getNoBlock() {
        return mValue.get();
    }

    public synchronized void set(@Nullable final T value) {
        if (mValue.get() == null && value != null)
            notifyAll();
        mValue = new WeakReference<>(value);
    }
}
