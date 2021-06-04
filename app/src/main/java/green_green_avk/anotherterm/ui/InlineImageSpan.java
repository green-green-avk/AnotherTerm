package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InlineImageSpan extends DynamicDrawableSpan {
    @NonNull
    protected final Drawable mDrawable;

    public InlineImageSpan(@NonNull final Drawable drawable) {
        super();
        mDrawable = drawable.mutate();
    }

    public InlineImageSpan(@NonNull final Drawable drawable, final int verticalAlignment) {
        super(verticalAlignment);
        mDrawable = drawable.mutate();
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
}
