package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anothertermshellpluginutils.Auth;
import green_green_avk.anothertermshellpluginutils.Plugin;

public final class PluginsManager {
    private PluginsManager() {
    }

    public static List<PackageInfo> getPlugins() {
        final PackageManager pm = ctx.getPackageManager();
        final List<PackageInfo> pkgs = pm.getInstalledPackages(0);
        final List<PackageInfo> plugins = new ArrayList<>();
        for (final PackageInfo pkg : pkgs)
            if (Plugin.getComponent(ctx, pkg.packageName) != null) {
                try {
                    plugins.add(pm.getPackageInfo(pkg.packageName, PackageManager.GET_SIGNATURES));
                } catch (final PackageManager.NameNotFoundException ignored) {
                }
            }
        return Collections.unmodifiableList(plugins);
    }

    private static final BroadcastReceiver packagesChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (onChanged.size() == 0) return;
            final List<PackageInfo> plugins = getPlugins();
            for (final OnChanged l : onChanged) {
                l.onPackagesChanged(plugins);
            }
        }
    };

    private static final SharedPreferences.OnSharedPreferenceChangeListener onSettingsChanged =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                                      final String key) {
                    for (final OnChanged l : onChanged) {
                        l.onSettingsChanged();
                    }
                }
            };

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final Runnable rTryUnregister = new Runnable() {
        @Override
        public void run() {
            if (onChanged.size() == 0) {
                try {
                    ctx.unregisterReceiver(packagesChangeReceiver);
                } catch (final IllegalArgumentException ignored) {
                }
                trustedPluginsPrefs.unregisterOnSharedPreferenceChangeListener(onSettingsChanged);
            }
        }
    };

    public static abstract class OnChanged {
        public abstract void onPackagesChanged(@NonNull final List<PackageInfo> plugins);

        public abstract void onSettingsChanged();

        @Override
        protected void finalize() throws Throwable {
            mainHandler.post(rTryUnregister);
            super.finalize();
        }
    }

    private static final Set<OnChanged> onChanged =
            Collections.newSetFromMap(new WeakHashMap<OnChanged, Boolean>());

    public static void registerOnChanged(@NonNull final OnChanged l) {
        onChanged.add(l);
        final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addDataScheme("package");
        ctx.registerReceiver(packagesChangeReceiver, intentFilter);
        trustedPluginsPrefs.registerOnSharedPreferenceChangeListener(onSettingsChanged);
    }

    public static boolean verify(@NonNull final PackageInfo pkg) {
        final Set<String> fps = trustedPluginsPrefs.getStringSet(pkg.packageName, Collections.<String>emptySet());
        for (final Signature s : pkg.signatures)
            if (!fps.contains(Auth.getFingerprint(s))) return false;
        return true;
    }

    public static boolean verify(@NonNull final String pkgName) {
        final PackageManager pm = ctx.getPackageManager();
        try {
            return verify(pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES));
        } catch (final PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void grant(@NonNull final PackageInfo pkg) {
        final SharedPreferences.Editor editor = trustedPluginsPrefs.edit();
        final Set<String> fps = new HashSet<>();
        for (final Signature s : pkg.signatures)
            fps.add(Auth.getFingerprint(s));
        editor.putStringSet(pkg.packageName, fps);
        editor.apply();
    }

    public static void revoke(@NonNull final PackageInfo pkg) {
        final SharedPreferences.Editor editor = trustedPluginsPrefs.edit();
        editor.remove(pkg.packageName);
        editor.apply();
    }

    @SuppressLint("StaticFieldLeak")
    private static Context ctx = null;
    private static SharedPreferences trustedPluginsPrefs = null;

    public static void init(@NonNull final Context context) {
        ctx = context.getApplicationContext();
        trustedPluginsPrefs = ctx.getSharedPreferences(
                ctx.getPackageName() + "_trusted_plugins",
                Context.MODE_PRIVATE
        );
    }
}
