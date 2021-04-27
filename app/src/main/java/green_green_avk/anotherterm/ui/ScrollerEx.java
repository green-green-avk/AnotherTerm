package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class ScrollerEx extends Scroller {
    protected int mPrevX = 0;
    protected int mPrevY = 0;

    public ScrollerEx(final Context context) {
        super(context);
    }

    public ScrollerEx(final Context context, final Interpolator interpolator) {
        super(context, interpolator);
    }

    public ScrollerEx(final Context context, final Interpolator interpolator,
                      final boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    @Override
    public boolean computeScrollOffset() {
        mPrevX = getCurrX();
        mPrevY = getCurrY();
        return super.computeScrollOffset();
    }

    public int getDistanceX() {
        return getCurrX() - mPrevX;
    }

    public int getDistanceY() {
        return getCurrY() - mPrevY;
    }

    @Override
    public void startScroll(final int startX, final int startY, final int dx, final int dy) {
        super.startScroll(startX, startY, dx, dy);
        super.computeScrollOffset();
    }

    @Override
    public void startScroll(final int startX, final int startY, final int dx, final int dy,
                            final int duration) {
        super.startScroll(startX, startY, dx, dy, duration);
        super.computeScrollOffset();
    }

    public void fling(final int velocityX, final int velocityY) {
        fling(0, 0, velocityX, velocityY,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void fling(final int startX, final int startY, final int velocityX, final int velocityY,
                      final int minX, final int maxX, final int minY, final int maxY) {
        super.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
        super.computeScrollOffset();
    }
}
