package green_green_avk.anotherterm.ui.dragndrop;

import android.content.ClipData;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.ui.UiUtils;

public class TextDragSpan extends DragSpan {
    @NonNull
    public CharSequence text;
    @NonNull
    public String mime;

    public TextDragSpan(@NonNull final CharSequence text, @NonNull final String mime) {
        this.text = text;
        this.mime = mime;
    }

    @Override
    @Nullable
    public ClipData getData() {
        return new ClipData(text, new String[]{mime},
                new ClipData.Item(text));
    }

    @Override
    @NonNull
    public View.DragShadowBuilder getDragShadow(@NonNull final View view) {
        return new DragShadowBuilder(view);
    }

    protected class DragShadowBuilder extends View.DragShadowBuilder {
        protected final Paint paint;
        protected final Drawable bgDrawable;
        protected final Rect padding = new Rect();

        public DragShadowBuilder(@NonNull final View view) {
            super(view);
            final TextView tv = (TextView) view;
            paint = new Paint();
            paint.setTextSize(tv.getTextSize());
            paint.setColor(tv.getCurrentTextColor());
            bgDrawable = UiUtils.requireDrawable(view.getContext(), R.drawable.bg_frame2);
            bgDrawable.getPadding(padding);
        }

        @Override
        public void onProvideShadowMetrics(final Point outShadowSize,
                                           final Point outShadowTouchPoint) {
            final float w = paint.measureText(text, 0, text.length())
                    + padding.left + padding.right;
            final float h = paint.descent() - paint.ascent()
                    + padding.top + padding.bottom;
            outShadowSize.set((int) w, (int) h);
            outShadowTouchPoint.set((int) (w / 2), (int) h);
        }

        @Override
        public void onDrawShadow(final Canvas canvas) {
            bgDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            bgDrawable.draw(canvas);
            canvas.drawText(text, 0, text.length(),
                    padding.left, padding.top - paint.ascent(), paint);
        }
    }
}
