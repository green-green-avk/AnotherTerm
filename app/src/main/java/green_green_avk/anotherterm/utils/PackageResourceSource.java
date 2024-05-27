package green_green_avk.anotherterm.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public abstract class PackageResourceSource<T> extends DynamicResourceSource<T> {
    public static final class Key {
        @NonNull
        public final String packageName;
        public final int resourceId;
        @Nullable
        public final ApplicationInfo applicationInfo;
        /**
         * A stable name if provided or {@code null}.
         */
        @Nullable
        public final String resourceName;

        public Key(@NonNull final String packageName, final int resourceId,
                   @Nullable final ApplicationInfo applicationInfo,
                   @Nullable final String resourceName) {
            this.packageName = packageName;
            this.resourceId = resourceId;
            this.applicationInfo = applicationInfo;
            this.resourceName = resourceName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Key))
                return false;
            final Key that = (Key) o;
            if ((resourceName == null) != (that.resourceName == null))
                return false;
            if (resourceName != null)
                return resourceName.equals(that.resourceName) && packageName.equals(that.packageName);
            return resourceId == that.resourceId && packageName.equals(that.packageName);
        }

        @Override
        public int hashCode() {
            if (resourceName != null)
                return Objects.hash(packageName, resourceName);
            return Objects.hash(packageName, resourceId);
        }

        @Override
        @NonNull
        public String toString() {
            if (resourceName != null)
                return packageName + "/name:" + resourceName;
            return packageName + '/' + resourceId;
        }
    }

    public interface Enumerator {
        @NonNull
        Set<Key> onEnumerate(@Nullable String packageName);
    }

    @NonNull
    protected final Context context;
    @NonNull
    private final Enumerator enumerator;

    private static final class PackageWatcher<T> extends BroadcastReceiver {
        @NonNull
        private final WeakReference<PackageResourceSource<T>> source;

        private PackageWatcher(@NonNull final PackageResourceSource<T> source) {
            this.source = new WeakReference<>(source);
        }

        private final Set<String> trackedPackages = new HashSet<>();
        private final Set<Key> trackedKeys = new HashSet<>();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final PackageResourceSource<T> source = this.source.get();
            if (source == null) {
                return;
            }
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean isRemoved = Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction());
            boolean isChanged = false;
            if (trackedPackages.remove(packageName)) {
                final Iterator<Key> it = trackedKeys.iterator();
                while (it.hasNext()) {
                    final Key key = it.next();
                    if (key.packageName.equals(packageName)) {
                        it.remove();
                        source.invalidate(key);
                        isChanged = true;
                        if (isRemoved) {
                            source.callOnChanged(key);
                        }
                    }
                }
            }
            if (!isRemoved) {
                isChanged |= source.updateKeys(packageName);
            }
            if (isChanged) {
                source.callOnChanged();
            }
        }
    }

    private final PackageWatcher<T> watcher = new PackageWatcher<>(this);

    private boolean updateKeys(@Nullable final String packageName) {
        boolean r = false;
        final Set<Key> keys = enumerator.onEnumerate(packageName);
        for (final Key key : keys) {
            watcher.trackedPackages.add(key.packageName);
            if (watcher.trackedKeys.add(key)) {
                r = true;
                callOnChanged(key);
            }
        }
        return r;
    }

    protected PackageResourceSource(@NonNull final Context context,
                                    @NonNull final Enumerator enumerator) {
        this.context = context;
        this.enumerator = enumerator;

        final IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        context.registerReceiver(watcher, intentFilter);

        updateKeys(null);
        callOnChanged();
    }

    @Override
    @NonNull
    public Set<Key> enumerate() {
        return watcher.trackedKeys;
    }

    @Override
    public void recycle() {
        context.unregisterReceiver(watcher);
        super.recycle();
    }
}
