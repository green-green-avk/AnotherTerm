package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.graphics.PointF;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Mouse right & middle buttons tribute...
// Google and phone manufacturers are wise... It would be too simple otherwise.
// TODO: Add the middle button support... where possible...

// To be used with an Activity ancestor
public final class MouseButtonsWorkAround {
    @NonNull
    private final Activity ctx;
    private int mButtons = 0;
    private int mToolType = MotionEvent.TOOL_TYPE_UNKNOWN;
    private int mInjectedButtons = 0;
    private final PointF mXY = new PointF();
    @Nullable
    private MotionEvent fixedEvent = null;
    public boolean result = false;

    public MouseButtonsWorkAround(@NonNull final Activity ctx) {
        this.ctx = ctx;
    }

    private static boolean isInteresting(@NonNull final InputEvent event) {
        return (event.getSource() & InputDevice.SOURCE_ANY & (
                InputDevice.SOURCE_MOUSE
                        | InputDevice.SOURCE_STYLUS
                        | InputDevice.SOURCE_TRACKBALL
        )) != 0;
    }

    // We can't assign actionButton: Google is against it yet...
    @NonNull
    private static MotionEvent obtainEvent(final float x, final float y, final int action,
                                           final int buttons, final int actionButton,
                                           final int vScroll,
                                           final int source, final int toolType) {
        final long ts = SystemClock.uptimeMillis();
        final MotionEvent.PointerProperties[] pp = {new MotionEvent.PointerProperties()};
        pp[0].id = 0;
        pp[0].toolType = toolType;
        final MotionEvent.PointerCoords[] pc = {new MotionEvent.PointerCoords()};
        pc[0].x = x;
        pc[0].y = y;
        pc[0].pressure = 1.0f;
        pc[0].size = 1.0f;
        pc[0].setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);
        return MotionEvent.obtain(ts, ts, action, 1, pp, pc, 0, buttons,
                1.0f, 1.0f, 0, 0, source, 0);
    }

    private boolean fixEvent(@NonNull final MotionEvent event, final int action) {
        mXY.x = event.getX();
        mXY.y = event.getY();
        mButtons = event.getButtonState();
        mToolType = event.getToolType(0);
        if ((mButtons | mInjectedButtons) == event.getButtonState()) return false;
        return fixEvent((InputEvent) event, action);
    }

    private boolean fixEvent(@NonNull final InputEvent event, final int action) {
        fixedEvent = obtainEvent(mXY.x, mXY.y, action,
                mButtons | mInjectedButtons, 0, 0,
                event.getSource(), mToolType);
        try {
            result = ctx.dispatchTouchEvent(fixedEvent);
        } finally {
            try {
                fixedEvent.recycle();
            } finally {
                fixedEvent = null;
            }
        }
        return true;
    }

    public boolean onDispatchTouchEvent(@NonNull final MotionEvent event) {
        if (event == fixedEvent) return false;
        if (!isInteresting(event)) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP: {
                return fixEvent(event, event.getAction());
            }
        }
        return false;
    }

    public boolean onDispatchGenericMotionEvent(@NonNull final MotionEvent event) {
        if (!isInteresting(event)) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_MOVE: {
                return fixEvent(event, MotionEvent.ACTION_MOVE);
            }
        }
        return false;
    }

    public boolean onDispatchKeyEvent(@NonNull final KeyEvent event) {
        if (!isInteresting(event)) return false;
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN: {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_BACK:
                        mInjectedButtons |= MotionEvent.BUTTON_SECONDARY;
                        return fixEvent(event, MotionEvent.ACTION_DOWN);
                }
            }
            case KeyEvent.ACTION_UP: {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_BACK:
                        mInjectedButtons &= ~MotionEvent.BUTTON_SECONDARY;
                        return fixEvent(event, MotionEvent.ACTION_UP);
                }
            }
        }
        return false;
    }
}
