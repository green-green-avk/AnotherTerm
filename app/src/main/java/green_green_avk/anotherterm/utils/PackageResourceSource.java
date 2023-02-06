package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.Set;

public abstract class PackageResourceSource<T> extends DynamicResourceSource<T> {
    public static final class Key {
        @NonNull
        public final String packageName;
        public final int resourceId;
        @Nullable
        public final ApplicationInfo applicationInfo;

        public Key(@NonNull final String packageName, final int resourceId,
                   @Nullable final ApplicationInfo applicationInfo) {
            this.packageName = packageName;
            this.resourceId = resourceId;
            this.applicationInfo = applicationInfo;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Key))
                return false;
            final Key key = (Key) o;
            return resourceId == key.resourceId && packageName.equals(key.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, resourceId);
        }

        @Override
        @NonNull
        public String toString() {
            return packageName + '/' + resourceId;
        }
    }

    public interface Enumerator {
        @NonNull
        Set<Key> onEnumerate();

        void onRegister(@NonNull Runnable listener);

        void onUnregister(@NonNull Runnable listener);
    }

    @NonNull
    protected final Context context;
    @NonNull
    private final Enumerator enumerator;

    protected PackageResourceSource(@NonNull final Context context,
                                    @NonNull final Enumerator enumerator) {
        this.context = context;
        this.enumerator = enumerator;
    }

    @Override
    @NonNull
    protected Cache<T> getCache() {
        return Caches.noCache();
    }

    @Override
    @NonNull
    public Set<Key> enumerate() {
        return enumerator.onEnumerate();
    }

    private final Runnable callOnChanged = this::callOnChanged;

    @Override
    public void setOnChanged(@Nullable final OnChanged v) {
        if (v != null)
            enumerator.onRegister(callOnChanged);
        else
            enumerator.onUnregister(callOnChanged);
        super.setOnChanged(v);
    }
}
