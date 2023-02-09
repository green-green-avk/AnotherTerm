package green_green_avk.anotherterm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import green_green_avk.anotherterm.ui.InlineImageSpan;
import green_green_avk.anotherterm.utils.CollectionsViewSet;
import green_green_avk.anotherterm.utils.FileDrawableSource;
import green_green_avk.anotherterm.utils.PackageDrawableSource;
import green_green_avk.anotherterm.utils.PackageResourceSource;
import green_green_avk.anotherterm.utils.ProfileManager;

public final class BackgroundsManager extends ProfileManager<BackgroundProfile> {
    private final Set<BuiltIn<BackgroundProfile>> defaultSet = new HashSet<>();
    @NonNull
    private final FileDrawableSource localSource;
    @NonNull
    private final PackageDrawableSource remoteSource;
    private final Set<BuiltIn<BackgroundProfile>> localSet = new HashSet<>();
    private final Set<BuiltIn<BackgroundProfile>> remoteSet = new HashSet<>();
    @NonNull
    private final Set<BuiltIn<? extends BackgroundProfile>> builtIns;

    private void reloadLocalSource() {
        final Set<?> keys = localSource.enumerate();
        localSet.clear();
        for (final Object key : keys) {
            final Callable<Drawable> data;
            try {
                data = localSource.get(key);
            } catch (final Exception e) {
                continue;
            }
            localSet.add(new BuiltIn<>(" " + key, key.toString(),
                    new SourceBackgroundProfile(data), 0x10));
        }
    }

    private void reloadRemoteSource() {
        final Set<?> keys = remoteSource.enumerate();
        remoteSet.clear();
        for (final Object key : keys) {
            final Callable<Drawable> data;
            try {
                data = remoteSource.get(key);
            } catch (final Exception e) {
                continue;
            }
            final CharSequence title;
            if (key instanceof PackageResourceSource.Key &&
                    ((PackageResourceSource.Key) key).applicationInfo != null) {
                final PackageManager pm = context.getPackageManager();
                final CharSequence label =
                        ((PackageResourceSource.Key) key).applicationInfo.loadLabel(pm);
                final Drawable icon =
                        ((PackageResourceSource.Key) key).applicationInfo.loadIcon(pm);
                final SpannableStringBuilder builder =
                        new SpannableStringBuilder();
                builder.append('X').setSpan(new InlineImageSpan(icon),
                        0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(' ').append(label);
                title = builder;
            } else {
                title = key.toString();
            }
            remoteSet.add(new BuiltIn<>(" " + key, title,
                    new SourceBackgroundProfile(data), 0x20));
        }
    }

    @NonNull
    private final Context context;

    private final PackageResourceSource.Enumerator remoteEnumerator =
            new PackageResourceSource.Enumerator() {
                @Override
                @NonNull
                public Set<PackageResourceSource.Key> onEnumerate() {
                    final PackageManager pm = context.getPackageManager();
                    final List<ResolveInfo> pkgs = pm.queryIntentActivities(
                            new Intent(BuildConfig.ACTION_EXTRAS_INFO),
                            PackageManager.GET_META_DATA
                    );
                    if (pkgs == null)
                        return Collections.emptySet();
                    final Set<PackageResourceSource.Key> r = new HashSet<>();
                    for (final ResolveInfo pkg : pkgs) {
                        if (pkg.activityInfo == null)
                            continue;
                        if (pkg.activityInfo.applicationInfo.metaData == null)
                            continue;
                        final int extrasId = pkg.activityInfo.applicationInfo.metaData
                                .getInt("background-extras");
                        if (extrasId == ResourcesCompat.ID_NULL)
                            continue;
                        final Resources resources;
                        try {
                            resources = pm
                                    .getResourcesForApplication(pkg.activityInfo.applicationInfo);
                        } catch (final PackageManager.NameNotFoundException e) {
                            continue;
                        }
                        final TypedArray extras = resources.obtainTypedArray(extrasId);
                        try {
                            for (int i = 0; i < extras.length(); i++) {
                                final int id = extras.getResourceId(i,
                                        ResourcesCompat.ID_NULL);
                                if (id == ResourcesCompat.ID_NULL)
                                    continue;
                                r.add(new PackageResourceSource.Key(
                                        pkg.activityInfo.packageName,
                                        id,
                                        pkg.activityInfo.applicationInfo
                                ));
                            }
                        } finally {
                            extras.recycle();
                        }
                    }
                    return r;
                }

                private final Set<Runnable> listeners =
                        Collections.newSetFromMap(new WeakHashMap<>());
                private final BroadcastReceiver packagesChangeReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(final Context context, final Intent intent) {
                        for (final Runnable listener : listeners)
                            listener.run();
                    }
                };

                @Override
                public void onRegister(@NonNull final Runnable listener) {
                    listeners.add(listener);
                    final IntentFilter intentFilter =
                            new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
                    intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
                    intentFilter.addDataScheme("package");
                    context.registerReceiver(packagesChangeReceiver, intentFilter);
                }

                @Override
                public void onUnregister(@NonNull final Runnable listener) {
                    listeners.remove(listener);
                    if (listeners.isEmpty())
                        context.unregisterReceiver(packagesChangeReceiver);
                }

                @Override
                protected void finalize() throws Throwable {
                    context.unregisterReceiver(packagesChangeReceiver);
                    super.finalize();
                }
            };

    public BackgroundsManager(@NonNull final Context ctx) {
        context = ctx;
        defaultSet.add(new BuiltIn<>("", R.string.profile_title_builtin,
                new LocalBackgroundProfile(ctx,
                        R.drawable.bg_term_screen_blank), 0));
        defaultSet.add(new BuiltIn<>(" lines", R.string.profile_title_builtin,
                new LocalBackgroundProfile(ctx,
                        R.drawable.bg_term_screen_lines), 1));
        defaultSet.add(new BuiltIn<>(" lines_fade", R.string.profile_title_builtin,
                new LocalBackgroundProfile(ctx,
                        R.drawable.bg_term_screen_lines_fade), 2));
        localSource = new FileDrawableSource(ctx, "backgrounds");
        localSource.setOnChanged(() -> {
            reloadLocalSource();
            execOnChangeListeners();
        });
        remoteSource = new PackageDrawableSource(ctx, remoteEnumerator);
        remoteSource.setOnChanged(() -> {
            reloadRemoteSource();
            execOnChangeListeners();
        });
        builtIns = new CollectionsViewSet<>(defaultSet, localSet, remoteSet);
        reloadLocalSource();
        reloadRemoteSource();
    }

    @Override
    public boolean containsCustom(@NonNull final String name) {
        return false;
    }

    @Override
    @NonNull
    public Set<? extends Meta> enumerate() {
        return enumerateBuiltIn();
    }

    @Override
    @NonNull
    public Set<BuiltIn<? extends BackgroundProfile>> enumerateBuiltIn() {
        return builtIns;
    }

    @Override
    @NonNull
    public Set<? extends Meta> enumerateCustom() {
        return Collections.emptySet();
    }

    @Override
    @Nullable
    public Meta getMeta(@Nullable final String name) {
        for (final BuiltIn<? extends BackgroundProfile> builtIn : builtIns) {
            if (builtIn.name.equals(name)) {
                return builtIn;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Meta getMeta(@NonNull final BackgroundProfile data) {
        for (final BuiltIn<? extends BackgroundProfile> builtIn : builtIns) {
            if (builtIn.data.equals(data)) {
                return builtIn;
            }
        }
        return null;
    }

    @Override
    @NonNull
    public BackgroundProfile get(@Nullable final String name) {
        final Meta r = getMeta(name);
        return r instanceof BuiltIn ?
                ((BuiltIn<? extends BackgroundProfile>) r).data : defaultSet.iterator().next().data;
    }

    @Override
    public void remove(@NonNull final String name) {
        // Not editable
    }
}
