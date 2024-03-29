package green_green_avk.anotherterm.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;

public abstract class BaseProfileManager<T> extends ProfileManager<T> {
    @NonNull
    protected abstract Map<String, BuiltIn<? extends T>> onGetBuiltIns();

    @NonNull
    protected abstract BuiltIn<? extends T> onGetDefaultBuiltIn();

    private Set<? extends Meta> enumerator = null; // Subclasses must be initialized before

    @Override
    @NonNull
    public Set<? extends Meta> enumerate() {
        if (enumerator == null) {
            enumerator = new CollectionsViewSet<>(enumerateBuiltIn(), enumerateCustom());
        }
        return enumerator;
    }

    public boolean isBuiltIn(@Nullable final String name) {
        return name == null || name.isEmpty() || name.charAt(0) == ' ';
    }

    @Nullable
    protected BuiltIn<? extends T> getBuiltIn(@Nullable final String name) {
        if (isBuiltIn(name)) {
            final BuiltIn<? extends T> builtIn = onGetBuiltIns().get(name);
            return builtIn != null ? builtIn : onGetDefaultBuiltIn();
        }
        return null;
    }

    @Nullable
    protected BuiltIn<? extends T> getBuiltIn(@NonNull final T data) {
        for (final BuiltIn<? extends T> builtIn : onGetBuiltIns().values()) {
            if (builtIn.data.equals(data))
                return builtIn;
        }
        return null;
    }

    @NonNull
    public CharSequence getTitle(@Nullable final String name, @NonNull final Context ctx) {
        final BuiltIn<? extends T> b = getBuiltIn(name);
        if (b == null) {
            assert name != null;
            return name;
        } else {
            return b.getTitle(ctx);
        }
    }
}
