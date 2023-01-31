package green_green_avk.anotherterm.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class ProfileManager<T> {
    public static class Meta {
        @NonNull
        public final String name;
        @NonNull
        public final Object title;
        public final boolean isBuiltIn;
        public final int order;

        public Meta(@NonNull final String name, @NonNull final Object title,
                    final boolean isBuiltIn,
                    final int order) {
            this.name = name;
            this.title = title;
            this.isBuiltIn = isBuiltIn;
            this.order = order;
        }

        public Meta(final String name, final String title, final boolean isBuiltIn) {
            this(name, title, isBuiltIn, 0);
        }

        @NonNull
        public CharSequence getTitle(@NonNull final Context ctx) {
            if (title instanceof CharSequence) {
                return (CharSequence) title;
            }
            if (title instanceof Integer) {
                return ctx.getString((Integer) title);
            }
            return title.toString();
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Meta) {
                return isBuiltIn == ((Meta) obj).isBuiltIn && name.equals(((Meta) obj).name);
            }
            return false;
        }
    }

    public static final class BuiltIn<T> extends Meta {
        @NonNull
        public final T data;

        public BuiltIn(final String name, @StringRes final int title, @NonNull final T data,
                       final int order) {
            super(name, title, true, order);
            this.data = data;
        }
    }

    public abstract boolean containsCustom(@NonNull String name);

    @NonNull
    public abstract Set<? extends Meta> enumerate();

    @NonNull
    public abstract Set<BuiltIn<? extends T>> enumerateBuiltIn();

    @NonNull
    public abstract Set<? extends Meta> enumerateCustom();

    @Nullable
    public abstract Meta getMeta(@Nullable String name);

    @Nullable
    public abstract Meta getMeta(@NonNull final T data);

    @NonNull
    public T get(@NonNull final Meta meta) {
        if (meta instanceof BuiltIn) {
            return ((BuiltIn<T>) meta).data;
        }
        return get(meta.name);
    }

    @NonNull
    public abstract T get(@Nullable final String name);

    public abstract void remove(@NonNull final String name);

    private final Set<Runnable> onChangeListeners =
            Collections.newSetFromMap(new WeakHashMap<>());

    protected final void execOnChangeListeners() {
        for (final Runnable r : onChangeListeners) {
            r.run();
        }
    }

    public final void addOnChangeListener(@NonNull final Runnable runnable) {
        onChangeListeners.add(runnable);
    }
}
