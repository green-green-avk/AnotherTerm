package green_green_avk.anotherterm.ui.drawables;

import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;

import green_green_avk.anotherterm.ui.UiDimens;

/**
 * It works as it sounds.
 * <p>
 * These compound units looks redundant in some way but for good.
 */
public class AdvancedScaleDrawable extends DrawableWrapperCompat {
    public final UiDimens.Length left = new UiDimens.Length(0f,
            UiDimens.Length.Units.PARENT_WIDTH, this::invalidateSelf);
    public final UiDimens.Length top = new UiDimens.Length(0f,
            UiDimens.Length.Units.PARENT_HEIGHT, this::invalidateSelf);
    public final UiDimens.Length right = new UiDimens.Length(1f,
            UiDimens.Length.Units.PARENT_WIDTH, this::invalidateSelf);
    public final UiDimens.Length bottom = new UiDimens.Length(1f,
            UiDimens.Length.Units.PARENT_HEIGHT, this::invalidateSelf);

    private float density = 1f;
    private float scaledDensity = density;

    public AdvancedScaleDrawable(@Nullable final Drawable drawable) {
        super(drawable);
    }

    public void setMetrics(@NonNull final DisplayMetrics displayMetrics) {
        density = displayMetrics.density;
        scaledDensity = displayMetrics.scaledDensity;
    }

    @Override
    public void setDrawable(@Nullable final Drawable drawable) {
        super.setDrawable(drawable);
        invalidateSelf();
    }

    private int measureX(@NonNull final UiDimens.Length length, @NonNull final Rect bounds) {
        return (int) length.measure(bounds.width(), bounds.height(),
                density, scaledDensity) + bounds.left;
    }

    private int measureY(@NonNull final UiDimens.Length length, @NonNull final Rect bounds) {
        return (int) length.measure(bounds.width(), bounds.height(),
                density, scaledDensity) + bounds.top;
    }

    @Override
    protected void onBoundsChange(@NonNull final Rect bounds) {
        transparentRegion.set(bounds);
        if (getDrawable() != null) {
            getDrawable().setBounds(
                    measureX(left, bounds),
                    measureY(top, bounds),
                    measureX(right, bounds),
                    measureY(bottom, bounds)
            );
            transparentRegion.op(getDrawable().getBounds(), Region.Op.DIFFERENCE);
            final Region wtr = getDrawable().getTransparentRegion();
            if (wtr != null) {
                transparentRegion.op(wtr, Region.Op.UNION);
            }
        }
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        if (getDrawable() != null)
            super.draw(canvas);
    }

    @Override
    public int getOpacity() {
        return getDrawable() != null && getDrawable().getOpacity() == PixelFormat.TRANSLUCENT ?
                PixelFormat.TRANSLUCENT : PixelFormat.TRANSPARENT;
    }

    private final Region transparentRegion = new Region();

    @Override
    @Nullable
    public Region getTransparentRegion() {
        return transparentRegion;
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

    @Override
    public boolean getPadding(@NonNull final Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }
}
