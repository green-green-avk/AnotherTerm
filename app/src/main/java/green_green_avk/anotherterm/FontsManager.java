package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

public final class FontsManager {
    private FontsManager() {
    }

    @SuppressLint("StaticFieldLeak")
    private static Context ctx = null;

    public static final Typeface[] defaultTypefaces = {
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
    };

    public static Typeface[] consoleTypefaces = defaultTypefaces;

    public static void init(@NonNull final Context ctx) {
        FontsManager.ctx = ctx.getApplicationContext();
        consoleTypefaces = loadFromAsset("DejaVuSansMono", ".ttf");
    }

    public static void loadFromAsset(@NonNull final Typeface[] tfs,
                                     @NonNull final String name, @NonNull final String ext) {
        final AssetManager am = ctx.getApplicationContext().getAssets();
        final String[] tns = {"-Regular", "-Bold", "-Italic", "-BoldItalic"};
        for (int i = 0; i < 4; ++i) {
            tfs[i] = Typeface.createFromAsset(am, "fonts/" + name + "/" + name + tns[i] + ext);
        }
    }

    @NonNull
    public static Typeface[] loadFromAsset(@NonNull final String name, @NonNull final String ext) {
        final Typeface[] tfs = new Typeface[4];
        loadFromAsset(tfs, name, ext);
        return tfs;
    }
}
