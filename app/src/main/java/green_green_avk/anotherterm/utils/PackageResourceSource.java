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
            if (source == null)
                return;
            final String packageName = intent.getData().getSchemeSpecificPart();
            boolean isChanged = false;
            if (trackedPackages.remove(packageName)) {
                final Iterator<Key> it = trackedKeys.iterator();
                while (it.hasNext()) {
                    final Key key = it.next();
                    if (key.packageName.equals(packageName)) {
                        it.remove();
                        source.invalidate(key);
                        isChanged = true;
                    }
                }
            }
            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                isChanged |= source.updateKeys();
            }
            if (isChanged) {
                source.callOnChanged();
            }
        }
    }

    private final PackageWatcher<T> watcher = new PackageWatcher<>(this);

    private boolean updateKeys() {
        boolean r = false;
        final Set<Key> keys = enumerator.onEnumerate();
        for (final Key key : keys) {
            watcher.trackedPackages.add(key.packageName);
            r |= watcher.trackedKeys.add(key);
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

        updateKeys();
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
