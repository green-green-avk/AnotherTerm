package green_green_avk.anotherterm.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.style.QuoteSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class CustomQuoteSpan extends QuoteSpan {
    protected static final int mStripeWidth = 4;
    protected static final int mGapWidth = 4;

    public CustomQuoteSpan(@ColorInt final int color) {
        super(color);
    }

    public CustomQuoteSpan(@NonNull final Parcel src) {
        super(src);
    }

    @Override
    public int getLeadingMargin(final boolean first) {
        return mStripeWidth + mGapWidth;
    }

    @Override
    public void drawLeadingMargin(@NonNull final Canvas c, @NonNull final Paint p,
                                  final int x, final int dir,
                                  final int top, final int baseline, final int bottom,
                                  @NonNull final CharSequence text, final int start, final int end,
                                  final boolean first, @NonNull final Layout layout) {
        final Paint.Style style = p.getStyle();
        final int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(getColor());

        c.drawRect(x, top, x + dir * mStripeWidth, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
