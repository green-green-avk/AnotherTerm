package green_green_avk.anotherterm.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import green_green_avk.anotherterm.utils.WeakHandler;

// Adjust resize in fullscreen mode helper

public final class BottomImePlaceholderView extends View {

    public BottomImePlaceholderView(final Context context) {
        super(context);
    }

    public BottomImePlaceholderView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomImePlaceholderView(final Context context, final AttributeSet attrs,
                                    final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BottomImePlaceholderView(final Context context, final AttributeSet attrs,
                                    final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private boolean needResize() {
        final Context ctx = getContext();
        return ctx instanceof Activity &&
                (((Activity) ctx).getWindow().getAttributes().softInputMode
                        & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                        != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
    }

    private final WeakHandler handler = new WeakHandler();

    private final Runnable rLayout = new Runnable() {
        @Override
        public void run() {
            requestLayout();
        }
    };

    private int oldH = -1;

    private final Rect r = new Rect();

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int h;
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);
        if (hMode == MeasureSpec.EXACTLY) {
            h = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            if (needResize()) {
                final View v = (View) getParent();
                v.getWindowVisibleDisplayFrame(r);
                h = v.getBottom() - r.bottom;
                if (h < 0) h = 0;
            } else h = 0;
            if (hMode == MeasureSpec.AT_MOST) {
                final int hMax = MeasureSpec.getSize(heightMeasureSpec);
                if (h > hMax) h = hMax;
            }
        }
        // Lost re-rendering workaround when IME is shown after hidden navigation bar
        if (h != oldH) {
            handler.removeCallbacks(rLayout);
            handler.postDelayed(rLayout, 500);
            oldH = h;
        }
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), h);
    }
}
