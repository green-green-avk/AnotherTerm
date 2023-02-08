package green_green_avk.anotherterm.ui.drawables;

import android.graphics.drawable.Drawable;

import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;

public final class SquarePreviewDrawable extends DrawableWrapperCompat {
    public SquarePreviewDrawable(final Drawable drawable) {
        super(drawable);
    }

    @Override
    public int getIntrinsicWidth() {
        return Short.MAX_VALUE;
    }

    @Override
    public int getIntrinsicHeight() {
        return Short.MAX_VALUE;
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
