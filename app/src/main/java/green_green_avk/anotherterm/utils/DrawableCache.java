package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.arch.core.util.Function;

public final class DrawableCache
        extends SoftRefCache<Function<? super Context, ? extends Drawable>> {
    private DrawableCache() {
    }

    public static final DrawableCache instance = new DrawableCache();
}
