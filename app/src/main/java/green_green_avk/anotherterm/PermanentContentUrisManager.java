package green_green_avk.anotherterm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import green_green_avk.anotherterm.utils.PreferenceStorage;

public final class PermanentContentUrisManager {
    private static final String TAG = PermanentContentUrisManager.class.getSimpleName();

    private PermanentContentUrisManager() {
    }

    private static final Set<String> permUriFavFields = new HashSet<>();

    static {
        permUriFavFields.add("auth_key_uri");
    }

    public static void freeUnused(@NonNull final Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;
        final Set<Uri> inUse = new HashSet<>();
        for (final String entryName : FavoritesManager.enumerate()) {
            final PreferenceStorage ps = FavoritesManager.get(entryName);
            for (final String fieldName : permUriFavFields) {
                final Object v = ps.get(fieldName);
                if (v instanceof String) {
                    inUse.add(Uri.parse((String) v));
                }
            }
        }
        try {
            final ContentResolver cr = ctx.getContentResolver();
            for (final UriPermission perm : cr.getPersistedUriPermissions()) {
                if (perm.isReadPermission() && !inUse.contains(perm.getUri())) {
                    try {
                        cr.releasePersistableUriPermission(perm.getUri(),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (final Exception e) {
                        if (BuildConfig.DEBUG)
                            Log.e(TAG, e.getMessage());
                    }
                }
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, e.getMessage());
        }
    }
}
