package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public final class MappedViewSet<T, F> extends AbstractSet<T> {
    @NonNull
    private final Collection<F> wrapped;
    @NonNull
    private final Function<? super F, ? extends T> mapping;
    @NonNull
    private final Function<? super T, ? extends F> revMapping;

    public MappedViewSet(@NonNull final Collection<F> collection,
                         @NonNull final Function<? super F, ? extends T> mapping,
                         @NonNull final Function<? super T, ? extends F> revMapping) {
        this.wrapped = collection;
        this.mapping = mapping;
        this.revMapping = revMapping;
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final Iterator<F> it = wrapped.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return mapping.apply(it.next());
            }
        };
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    /**
     * @param o element whose presence in this set is to be tested
     * @return {@link Collection#contains(Object)}{@code (revMapping.apply(o))}
     * result of the wrapped collection
     * or {@code false} if {@code revMapping.apply(o)} throws a {@link RuntimeException}
     */
    @Override
    public boolean contains(@Nullable final Object o) {
        final Object wo;
        try {
            wo = revMapping.apply((T) o);
        } catch (final RuntimeException e) {
            return false;
        }
        return wrapped.contains(wo);
    }
}
