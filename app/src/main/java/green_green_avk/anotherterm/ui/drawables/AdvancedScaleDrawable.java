package green_green_avk.anotherterm.ui.drawables;

import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;

import green_green_avk.anotherterm.ui.UiDimens;

public class AdvancedScaleDrawable extends DrawableWrapperCompat {
    public final UiDimens.Length left = new UiDimens.Length(0f,
            UiDimens.Length.Units.PARENT_WIDTH, this::invalidateSelf);
    public final UiDimens.Length top = new UiDimens.Length(0f,
            UiDimens.Length.Units.PARENT_HEIGHT, this::invalidateSelf);
    public final UiDimens.Length right = new UiDimens.Length(1f,
            UiDimens.Length.Units.PARENT_WIDTH, this::invalidateSelf);
    public final UiDimens.Length bottom = new UiDimens.Length(1f,
            UiDimens.Length.Units.PARENT_HEIGHT, this::invalidateSelf);

    public AdvancedScaleDrawable(@Nullable final Drawable drawable) {
        super(drawable);
    }

    @Override
    public void setDrawable(@Nullable final Drawable drawable) {
        super.setDrawable(drawable);
        invalidateSelf();
    }

    private static int measure(final float length, @NonNull final UiDimens.Length.Units units,
                               @NonNull final Rect bounds) {
        switch (units) {
            case PARENT_WIDTH:
                return (int) (bounds.width() * length);
            case PARENT_HEIGHT:
                return (int) (bounds.height() * length);
            case PARENT_MIN_DIM:
                return (int) (Math.min(bounds.width(), bounds.height()) * length);
            case PARENT_MAX_DIM:
                return (int) (Math.max(bounds.width(), bounds.height()) * length);
            default:
                return 0;
        }
    }

    private static int measure(@NonNull final UiDimens.Length length, @NonNull final Rect bounds) {
        return measure(length.getValue(), length.getUnits(), bounds);
    }

    @Override
    protected void onBoundsChange(@NonNull final Rect bounds) {
        transparentRegion.set(bounds);
        if (getDrawable() != null) {
            getDrawable().setBounds(
                    measure(left, bounds),
                    measure(top, bounds),
                    measure(right, bounds),
                    measure(bottom, bounds)
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
