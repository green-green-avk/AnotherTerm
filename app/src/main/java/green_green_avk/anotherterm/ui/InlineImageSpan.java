package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

public class InlineImageSpan extends DynamicDrawableSpan {
    @NonNull
    protected final Drawable mDrawable;
    protected boolean mUseTextColor = false;

    public InlineImageSpan(@NonNull final Drawable drawable) {
        super();
        mDrawable = DrawableCompat.wrap(drawable.mutate());
    }

    public InlineImageSpan(@NonNull final Drawable drawable, final int verticalAlignment) {
        super(verticalAlignment);
        mDrawable = DrawableCompat.wrap(drawable.mutate());
    }

    public InlineImageSpan(@NonNull final Context ctx, @DrawableRes final int res) {
        this(UiUtils.requireDrawable(ctx, res));
    }

    public InlineImageSpan(@NonNull final Context ctx, @DrawableRes final int res,
                           final int verticalAlignment) {
        this(UiUtils.requireDrawable(ctx, res), verticalAlignment);
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @NonNull
    public InlineImageSpan useTextColor() {
        mUseTextColor = true;
        return this;
    }

    @Override
    public int getSize(@NonNull final Paint paint,
                       final CharSequence text, final int start, final int end,
                       @Nullable final Paint.FontMetricsInt fm) {
        final int w = mDrawable.getIntrinsicWidth();
        final int h = mDrawable.getIntrinsicHeight();
        if (w > 0 && h > 0) {
            final int nh = (int) paint.getTextSize();
            final int nw = w * nh / h;
            mDrawable.setBounds(0, 0, nw, nh);
        }
        return super.getSize(paint, text, start, end, fm);
    }

    @Override
    public void draw(@NonNull final Canvas canvas, final CharSequence text,
                     final int start, final int end,
                     final float x, final int top, final int y, final int bottom,
                     @NonNull final Paint paint) {
        if (mUseTextColor)
            DrawableCompat.setTint(mDrawable, paint.getColor());
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);
    }
}
