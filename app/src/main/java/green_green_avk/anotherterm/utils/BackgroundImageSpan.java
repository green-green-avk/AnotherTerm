package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ReplacementSpan;

public final class BackgroundImageSpan extends ReplacementSpan {
    @Nullable
    private final Drawable background;
    private final Rect padding = new Rect();

    public BackgroundImageSpan(@Nullable final Drawable background) {
        this.background = background;
        if (this.background != null) this.background.getPadding(padding);
    }

    public BackgroundImageSpan(@NonNull final Context ctx, @DrawableRes final int resId) {
        this(ctx.getResources().getDrawable(resId));
    }

    @Override
    public int getSize(@NonNull final Paint paint, final CharSequence text,
                       final int start, final int end, @Nullable final Paint.FontMetricsInt fm) {
        return (int) Math.ceil(paint.measureText(text, start, end)) + padding.left + padding.right;
    }

    @Override
    public void draw(@NonNull final Canvas canvas, final CharSequence text,
                     final int start, final int end,
                     final float x, final int top, final int y, final int bottom,
                     @NonNull final Paint paint) {
        final int width = getSize(paint, text, start, end, paint.getFontMetricsInt());
        if (background != null) {
            background.setBounds((int) x, top,
                    (int) x + width, bottom);
            background.draw(canvas);
        }
        canvas.drawText(text, start, end, x + padding.left, y, paint);
    }
}
