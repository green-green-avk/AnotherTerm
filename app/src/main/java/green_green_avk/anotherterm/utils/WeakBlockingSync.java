package green_green_avk.anotherterm.utils;

import java.lang.ref.WeakReference;

public final class WeakBlockingSync<T> {
    private WeakReference<T> mValue = new WeakReference<>(null);

    public synchronized T get() throws InterruptedException {
        T v;
        while ((v = mValue.get()) == null) wait();
        return v;
    }

    public synchronized T getNoBlock() {
        return mValue.get();
    }

    public synchronized void set(final T value) {
        if (mValue.get() == null && value != null) notifyAll();
        mValue = new WeakReference<>(value);
    }
}
