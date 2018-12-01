package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class ScrollerEx extends Scroller {
    private int mPrevX = 0;
    private int mPrevY = 0;

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

    public void fling(final int velocityX, final int velocityY) {
        super.fling(0, 0, velocityX, velocityY,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        super.computeScrollOffset();
    }
}
