package name.green_green_avk.compatcolorpicker;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An API<16 workaround for {@link android.graphics.drawable.GradientDrawable}.
 */
final class GradientBarDrawable extends Drawable {
    private final Paint fillPaint = new Paint();
    private boolean isGradientDirty = true;

    @ColorInt
    private int startColor;
    @ColorInt
    private int endColor;

    {
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public GradientBarDrawable(final int startColor, final int endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
    }

    private void invalidateGradient() {
        if (!isGradientDirty)
            return;
        isGradientDirty = false;
        final Rect bb = getBounds();
        fillPaint.setShader(new LinearGradient(bb.left, 0, bb.right, 0,
                startColor, endColor, Shader.TileMode.CLAMP));
    }

    public void setStartColor(@ColorInt final int v) {
        startColor = v;
        isGradientDirty = true;
        invalidateSelf();
    }

    public void setEndColor(@ColorInt final int v) {
        endColor = v;
        isGradientDirty = true;
        invalidateSelf();
    }

    private boolean isOpaque(@ColorInt final int color) {
        return (color & 0xFF000000) == 0xFF000000;
    }

    private boolean isTransparent(@ColorInt final int color) {
        return (color & 0xFF000000) == 0;
    }

    @Override
    public int getOpacity() {
        if (isOpaque(startColor) && isOpaque(endColor))
            return PixelFormat.OPAQUE;
        if (isTransparent(startColor) && isTransparent(endColor))
            return PixelFormat.TRANSPARENT;
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(final int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable final ColorFilter colorFilter) {
    }

    @Override
    public void setBounds(final int left, final int top, final int right, final int bottom) {
        final Rect bb = getBounds();
        if (left != bb.left || top != bb.top || right != bb.right || bottom != bb.bottom) {
            isGradientDirty = true;
        }
        super.setBounds(left, top, right, bottom);
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        invalidateGradient();
        canvas.drawRect(getBounds(), fillPaint);
    }
}
