package green_green_avk.anotherterm.utils;

import android.graphics.drawable.Drawable;

import java.util.concurrent.Callable;

public final class DrawableCache extends SoftRefCache<Callable<Drawable>> {
    private DrawableCache() {
    }

    public static final DrawableCache instance = new DrawableCache();
}
