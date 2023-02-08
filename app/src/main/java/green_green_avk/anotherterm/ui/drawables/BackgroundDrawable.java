package green_green_avk.anotherterm.ui.drawables;

import android.graphics.drawable.Drawable;

import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;

public class BackgroundDrawable extends DrawableWrapperCompat {
    public BackgroundDrawable(final Drawable drawable) {
        super(drawable);
    }

    @Override
    public int getIntrinsicWidth() {
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return -1;
    }

    @Override
    public int getMinimumWidth() {
        return 0;
    }

    @Override
    public int getMinimumHeight() {
        return 0;
    }
}
