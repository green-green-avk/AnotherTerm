package green_green_avk.anotherterm.utils;

import android.graphics.drawable.Drawable;

public final class DrawableCache extends SoftRefCache<Drawable> {
    private DrawableCache() {
    }

    public static final DrawableCache instance = new DrawableCache();
}
