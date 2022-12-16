package green_green_avk.anotherterm.ui.dragndrop;

import android.content.Context;
import android.graphics.RectF;
import android.text.Layout;
import android.text.Spannable;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DragMovementMethodDelegate {
    protected static final float SCROLL_THRESHOLD_DP = 2;

    protected boolean isScrolled(@NonNull final Context ctx, @NonNull final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE && event.getHistorySize() > 0) {
            final float scrollThreshold =
                    ctx.getResources().getDisplayMetrics().density * SCROLL_THRESHOLD_DP;
            final float dx = Math.abs(event.getHistoricalX(0) - event.getX());
            final float dy = Math.abs(event.getHistoricalY(0) - event.getY());
            return dx > scrollThreshold || dy > scrollThreshold;
        }
        return false;
    }

    public boolean onTouchEvent(final TextView textView, final Spannable text,
                                final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            final DragSpan span = findSpan(textView, text, DragSpan.class,
                    (int) event.getX(), (int) event.getY());
            if (span != null)
                textView.getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (isScrolled(textView.getContext(), event)) {
            final DragSpan span = findSpan(textView, text, DragSpan.class,
                    (int) event.getHistoricalX(0), (int) event.getHistoricalY(0));
            if (span != null)
                span.startDragAndDrop(textView);
        }
        return true;
    }

    @Nullable
    protected static <T> T findSpan(@NonNull final TextView textView, @NonNull final Spannable text,
                                    @NonNull final Class<T> what, final int x, final int y) {
        final int _x = x - textView.getTotalPaddingLeft() + textView.getScrollX();
        final int _y = y - textView.getTotalPaddingTop() + textView.getScrollY();
        final Layout layout = textView.getLayout();
        final int line = layout.getLineForVertical(_y);
        final int off = layout.getOffsetForHorizontal(line, _x);
        final RectF lineBounds = new RectF();
        lineBounds.left = layout.getLineLeft(line);
        lineBounds.top = layout.getLineTop(line);
        lineBounds.right = layout.getLineWidth(line) + lineBounds.left;
        lineBounds.bottom = layout.getLineBottom(line);
        if (lineBounds.contains(_x, _y)) {
            final T[] spans = text.getSpans(off, off, what);
            if (spans.length > 0)
                return spans[0];
        }
        return null;
    }
}
