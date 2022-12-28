package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class SimpleProfileManager<T> extends BaseProfileManager<T> {
    @NonNull
    protected abstract List<BuiltIn<? extends T>> onInitBuiltIns();

    @Nullable
    protected abstract T onLoad(@NonNull String name);

    private final Set<BuiltIn<? extends T>> builtIns;
    private final BuiltIn<? extends T> defaultBuiltIn;
    private final Map<String, BuiltIn<? extends T>> builtInsByName;

    private final Map<T, Meta> meta = new WeakHashMap<>();

    {
        final List<BuiltIn<? extends T>> src = onInitBuiltIns();
        assert !src.isEmpty();
        builtIns = Collections.unmodifiableSet(new HashSet<>(src));
        defaultBuiltIn = src.get(0);
        final Map<String, BuiltIn<? extends T>> _builtInsByName = new HashMap<>();
        for (final BuiltIn<? extends T> builtIn : src) {
            _builtInsByName.put(builtIn.name, builtIn);
        }
        builtInsByName = Collections.unmodifiableMap(_builtInsByName);
    }

    @Override
    @NonNull
    protected Map<String, BuiltIn<? extends T>> onGetBuiltIns() {
        return builtInsByName;
    }

    @Override
    @NonNull
    protected BuiltIn<? extends T> onGetDefaultBuiltIn() {
        return defaultBuiltIn;
    }

    @Override
    @NonNull
    public Set<BuiltIn<? extends T>> enumerateBuiltIn() {
        return builtIns;
    }

    @Override
    @Nullable
    public Meta getMeta(@Nullable final String name) {
        final BuiltIn<? extends T> b = getBuiltIn(name);
        if (b != null)
            return b;
        return new Meta(name, name, false);
    }

    @Override
    @Nullable
    public Meta getMeta(@NonNull final T data) {
        final BuiltIn<? extends T> b = getBuiltIn(data);
        if (b != null)
            return b;
        return meta.get(data);
    }

    @Override
    @NonNull
    public T get(@Nullable final String name) {
        final BuiltIn<? extends T> b = getBuiltIn(name);
        if (b != null)
            return b.data;
        assert name != null;
        final T c = onLoad(name);
        if (c != null) {
            meta.put(c, new Meta(name, name, false));
            return c;
        }
        return onGetDefaultBuiltIn().data;
    }
}
