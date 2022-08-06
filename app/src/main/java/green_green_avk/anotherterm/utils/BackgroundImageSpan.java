package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public final class BackgroundImageSpan extends ReplacementSpan {
    @Nullable
    private final Drawable background;
    private final Rect padding = new Rect();
    private CharSequence content = null;
    private Layout layout = null;
    private int width = Short.MAX_VALUE;

    public BackgroundImageSpan(@Nullable final Drawable background) {
        this.background = background;
        if (this.background != null) this.background.getPadding(padding);
    }

    public BackgroundImageSpan(@NonNull final Context ctx, @DrawableRes final int resId) {
        this(ResourcesCompat.getDrawable(ctx.getResources(), resId, null));
    }

    /**
     * Implements nested layouts effectively enabling nesting of
     * {@link ReplacementSpan} and thus image spans.
     * <p>
     * May be inefficient.
     *
     * @param content to show in place of an actual underlying text
     * @return self
     */
    @NonNull
    public BackgroundImageSpan setContent(@Nullable final CharSequence content) {
        this.content = content;
        return this;
    }

    @Override
    public int getSize(@NonNull final Paint paint, final CharSequence text,
                       final int start, final int end, @Nullable final Paint.FontMetricsInt fm) {
        final int width;
        if (content != null) {
            final TextPaint textPaint = new TextPaint(paint);
            final BoringLayout.Metrics metrics = BoringLayout.isBoring(content, textPaint);
            if (metrics != null) {
                width = metrics.width;
                layout = BoringLayout.make(content, textPaint, width,
                        Layout.Alignment.ALIGN_NORMAL, 1, 0,
                        metrics, true);
                if (fm != null) {
                    fm.top = metrics.top - padding.top;
                    fm.ascent = metrics.ascent - padding.top;
                    fm.bottom = metrics.bottom + padding.bottom;
                    fm.descent = metrics.descent + padding.bottom;
                }
            } else { // Static layout fallback: Old API or exotic content.
                width = (int) Math.ceil(StaticLayout.getDesiredWidth(content, textPaint));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    layout = StaticLayout.Builder
                            .obtain(content, 0, content.length(), textPaint, width)
                            .setMaxLines(1).build();
                } else {
                    layout = new StaticLayout(content, textPaint, width,
                            Layout.Alignment.ALIGN_NORMAL, 1, 0,
                            true);
                }
                if (fm != null) {
                    fm.top = fm.ascent = layout.getLineAscent(0) - padding.top;
                    fm.bottom = fm.descent = layout.getLineDescent(0) + padding.bottom;
                }
            }
        } else {
            width = (int) Math.ceil(paint.measureText(text, start, end));
            layout = null;
            if (fm != null) {
                fm.top -= padding.top;
                fm.ascent -= padding.top;
                fm.bottom += padding.bottom;
                fm.descent += padding.bottom;
            }
        }
        this.width = width + padding.left + padding.right;
        return this.width;
    }

    @Override
    public void draw(@NonNull final Canvas canvas, final CharSequence text,
                     final int start, final int end,
                     final float x, final int top, final int y, final int bottom,
                     @NonNull final Paint paint) {
        if (background != null) {
            background.setBounds((int) x, top,
                    (int) x + width, bottom);
            background.draw(canvas);
        }
        if (layout != null) {
            // Yes, the paint can differ from the getSize() phase a bit:
            // must be set here to AVOID drawing artifacts.
            layout.getPaint().set(paint);
            canvas.save();
            canvas.translate(x + padding.left, y - layout.getLineBaseline(0));
            layout.draw(canvas);
            canvas.restore();
        } else
            canvas.drawText(text, start, end, x + padding.left, y, paint);
    }
}
