package green_green_avk.anotherterm.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.style.QuoteSpan;

import androidx.annotation.NonNull;

public class CustomQuoteSpan extends QuoteSpan {
    protected static final int mStripeWidth = 4;
    protected static final int mGapWidth = 4;

    public CustomQuoteSpan(int color) {
        super(color);
    }

    public CustomQuoteSpan(Parcel src) {
        super(src);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mStripeWidth + mGapWidth;
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas c, @NonNull Paint p, int x, int dir,
                                  int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end,
                                  boolean first, @NonNull Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(getColor());

        c.drawRect(x, top, x + dir * mStripeWidth, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
