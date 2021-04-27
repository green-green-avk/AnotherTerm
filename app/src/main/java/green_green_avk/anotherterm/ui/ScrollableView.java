package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.CallSuper;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;

public abstract class ScrollableView extends GestureView {

    public interface OnScroll {
        void onScroll(@NonNull ScrollableView scrollableView);
    }

    public final PointF scrollPosition = new PointF(0, 0);
    protected final PointF scrollScale = new PointF(16, 16);
    protected final ScrollerEx mScroller;

    public boolean scrollDisabled = false;

    public OnScroll onScroll = null;

    public ScrollableView(final Context context) {
        super(context);
        mScroller = new ScrollerEx(getContext());
    }

    public ScrollableView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        mScroller = new ScrollerEx(getContext());
    }

    public ScrollableView(final Context context, @Nullable final AttributeSet attrs,
                          final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new ScrollerEx(getContext());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScrollableView(final Context context, @Nullable final AttributeSet attrs,
                          final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mScroller = new ScrollerEx(getContext());
    }

    public void setScrollScale(final float x, final float y) {
        mScroller.forceFinished(true);
        scrollScale.x = x;
        scrollScale.y = y;
    }

    private float toFloatX(final int v) {
        return (float) v / scrollScale.x;
    }

    private float toFloatY(final int v) {
        return (float) v / scrollScale.y;
    }

    private int toIntX(final float v) {
        return (int) (v * scrollScale.x);
    }

    private int toIntY(final float v) {
        return (int) (v * scrollScale.y);
    }

    @CheckResult
    public float getLeftScrollLimit() {
        return 0;
    }

    @CheckResult
    public float getTopScrollLimit() {
        return 0;
    }

    @CheckResult
    public float getRightScrollLimit() {
        return 0;
    }

    @CheckResult
    public float getBottomScrollLimit() {
        return 0;
    }

    @CallSuper
    protected void execOnScroll() {
        if (onScroll != null)
            onScroll.onScroll(this);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollPosition.x += toFloatX(mScroller.getDistanceX());
            scrollPosition.y += toFloatY(mScroller.getDistanceY());
            invalidateScroll();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void invalidateScroll() {
        scrollPosition.x = MathUtils.clamp(scrollPosition.x,
                getLeftScrollLimit(), getRightScrollLimit());
        scrollPosition.y = MathUtils.clamp(scrollPosition.y,
                getTopScrollLimit(), getBottomScrollLimit());
        execOnScroll();
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        mScroller.forceFinished(true);
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (scrollDisabled) return true;
        scrollPosition.x = distanceX / scrollScale.x + scrollPosition.x;
        scrollPosition.y = distanceY / scrollScale.y + scrollPosition.y;
        invalidateScroll();
        ViewCompat.postInvalidateOnAnimation(this);
        return true;
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        mScroller.forceFinished(true);
        if (scrollDisabled) return true;
        mScroller.fling(0, 0,
                -(int) velocityX, -(int) velocityY,
                toIntX(getLeftScrollLimit() - scrollPosition.x),
                toIntX(getRightScrollLimit() - scrollPosition.x),
                toIntY(getTopScrollLimit() - scrollPosition.y),
                toIntY(getBottomScrollLimit() - scrollPosition.y)
        );
        ViewCompat.postInvalidateOnAnimation(this);
        return true;
    }

    // In own units
    protected void _doScrollBy(final float x, final float y) {
        mScroller.forceFinished(true);
        mScroller.startScroll(0, 0, toIntX(x), toIntY(y));
        ViewCompat.postInvalidateOnAnimation(this);
    }

    // In own units
    public void doScrollTo(float x, float y) {
        x = MathUtils.clamp(x, getLeftScrollLimit(), getRightScrollLimit());
        y = MathUtils.clamp(y, getTopScrollLimit(), getBottomScrollLimit());
        _doScrollBy(x - scrollPosition.x, y - scrollPosition.y);
    }
}
