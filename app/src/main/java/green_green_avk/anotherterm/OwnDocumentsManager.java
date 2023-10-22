package green_green_avk.anotherterm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public final class OwnDocumentsManager {
    private OwnDocumentsManager() {
    }

    private static final String DOCUMENTS_DIR_NAME = "public";

    public static final CharSequence LOCATION_DESC = "$DATA_DIR/" + DOCUMENTS_DIR_NAME;

    private static final String K_ENABLED = "enabled";

    @NonNull
    public static File getRootFile(@NonNull final Context context) {
        return new File(context.getApplicationInfo().dataDir, DOCUMENTS_DIR_NAME);
    }

    @NonNull
    private static SharedPreferences getSP(@NonNull final Context context) {
        final Context appCtx = context.getApplicationContext();
        return appCtx.getSharedPreferences(appCtx.getPackageName() + ".own_documents",
                Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(@NonNull final Context context) {
        final SharedPreferences sp = getSP(context);
        return sp.getBoolean(K_ENABLED, false);
    }

    public static void setEnabled(@NonNull final Context context, final boolean v) {
        final SharedPreferences sp = getSP(context);
        final SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean(K_ENABLED, v);
        spe.apply();
        if (v) {
            try {
                getRootFile(context).mkdirs();
            } catch (final Exception ignored) {
            }
        }
        OwnDocumentsProvider.setPublicRootEnabled(context, v);
    }
}
