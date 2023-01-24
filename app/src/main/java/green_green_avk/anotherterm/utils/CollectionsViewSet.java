package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class CollectionsViewSet<T> extends AbstractSet<T> {
    @NonNull
    private final Collection<? extends T>[] collections;

    public CollectionsViewSet(@NonNull final Collection<? extends T>... collections) {
        this.collections = collections;
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final Iterator<? extends T>[] ii = new Iterator[collections.length];
            private int i = 0;

            {
                int p = 0;
                for (final Collection<? extends T> c : collections) {
                    ii[p] = c.iterator();
                    p++;
                }
            }

            @Override
            public boolean hasNext() {
                for (; i < ii.length; i++) {
                    if (ii[i].hasNext()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                for (; i < ii.length; i++) {
                    if (ii[i].hasNext()) {
                        return ii[i].next();
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public int size() {
        int r = 0;
        for (final Collection<? extends T> c : collections) {
            r += c.size();
        }
        return r;
    }

    @Override
    public boolean contains(@Nullable final Object o) {
        for (final Collection<? extends T> c : collections) {
            if (c.contains(o)) {
                return true;
            }
        }
        return false;
    }
}
