package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public abstract class GestureView extends View
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener/*,
        GestureDetector.OnContextClickListener*/ {

    protected final GestureDetector mGestureDetector;

    public GestureView(final Context context) {
        super(context);
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    public GestureView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    public GestureView(final Context context, @Nullable final AttributeSet attrs,
                       final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public GestureView(final Context context, @Nullable final AttributeSet attrs,
                       final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    /*
        @Override
        public boolean onContextClick(final MotionEvent e) {
            return false;
        }
    */
    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        return false;
    }

    @Override
    public void onLongPress(final MotionEvent e) {

    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(final MotionEvent e) {

    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        return false;
    }
}
