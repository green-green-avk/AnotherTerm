package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import green_green_avk.anotherterm.R;

public class ScreenMouseView extends ScrollableView {

    protected Drawable cursor = null;
    protected int cursorSize = 64;
    protected int vScrollStep = 16;

    protected boolean visibleCursor = true;

    protected Dialog overlay = null;
    protected ViewGroup overlayButtons = null;

    public ScreenMouseView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.screenMouseViewStyle, R.style.AppScreenMouseViewStyle);
    }

    public ScreenMouseView(final Context context, final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AppScreenMouseViewStyle);
    }

    public ScreenMouseView(final Context context, final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void init(final Context context, final AttributeSet attrs,
                        final int defStyleAttr, final int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ScreenMouseView, defStyleAttr, defStyleRes);
        try {
//            cursor = a.getDrawable(R.styleable.ScreenMouseView_cursor);
            cursor = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ScreenMouseView_cursor, 0));
            cursorSize = a.getDimensionPixelSize(R.styleable.ScreenMouseView_cursorSize,
                    cursor == null ? cursorSize :
                            Math.max(cursor.getIntrinsicWidth(), cursor.getIntrinsicHeight()));
            vScrollStep = a.getDimensionPixelSize(R.styleable.ScreenMouseView_vScrollStep, vScrollStep);
        } finally {
            a.recycle();
        }
        setScrollScale(-1, -1);
        mGestureDetector.setOnDoubleTapListener(null); // avoid interference with scroll
//        mGestureDetector.setContextClickListener(null);
        mGestureDetector.setIsLongpressEnabled(false);

        overlay = new Dialog(getContext(), R.style.AppScreenMouseOverlayTheme);
        overlay.setContentView(R.layout.screen_mouse_overlay);
        overlay.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        overlay.setOwnerActivity((Activity) getContext());
        final View odv = overlay.getWindow().getDecorView();
        odv.setScaleX(1);
        odv.setScaleY(1);
        overlayButtons = odv.findViewById(R.id.buttons);
        for (final View v : UiUtils.getIterable(overlayButtons)) {
            if (v.getTag() instanceof String) {
                v.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (v == overlayButtons) return true;
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_MOVE:
                                return onOverlayButton(v, event);
                        }
                        return false;
                    }
                });
            }
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scrollPosition.x = w / 2;
        scrollPosition.y = h / 2;
    }

    protected void showCursor() {
        if (!visibleCursor) {
            visibleCursor = true;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    protected void hideCursor() {
        if (visibleCursor) {
            visibleCursor = false;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected float getLeftScrollLimit() { // negative scale
        return getWidth();
    }

    @Override
    protected float getTopScrollLimit() { // negative scale
        return getHeight();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (visibleCursor) {
            canvas.save();
            final int sx = cursorSize;
            final int sy = cursorSize;
            final int mx = sx / 2;
            final int my = sy / 2;
            canvas.translate(scrollPosition.x - mx, scrollPosition.y - my);
            canvas.clipRect(0, 0, sx, sy);
            cursor.setBounds(0, 0, sx, sy);
            cursor.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        final boolean r = super.onScroll(e1, e2, distanceX, distanceY);
        dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                mOverlayButtons != 0 ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_HOVER_MOVE,
                mOverlayButtons, 0, 0);
        ViewCompat.postInvalidateOnAnimation(this);
        return r;
    }

    /*
        protected int getButtonForIndex(MotionEvent event, int i) {
            final float d = event.getX(i) - event.getX();
            if (d < 0) {
                return MotionEvent.BUTTON_PRIMARY;
            } else {
                return MotionEvent.BUTTON_SECONDARY;
            }
        }
    */
    protected void setOverlayCoords(final MotionEvent event) {
        int width = overlayButtons.getLayoutParams().width;
        int height = overlayButtons.getLayoutParams().height;
        if (width <= 0) width = overlayButtons.getWidth();
        if (height <= 0) height = overlayButtons.getHeight();
        overlayButtons.setX(event.getRawX() - width / 2);
        overlayButtons.setY(event.getRawY() - height / 2);
    }

    public static boolean isMouseEvent(final MotionEvent event) {
        return event != null &&
                event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (isOwnEvent(event)) return false;
        if (isMouseEvent(event)) {
            hideCursor();
            return false;
        }
        showCursor();
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                setOverlayCoords(event);
                overlay.show();
                break;
            }
            case MotionEvent.ACTION_UP: {
                overlay.cancel();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                setOverlayCoords(event);
                break;
            }
        }
        /*
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP: {
                final int id = event.getActionIndex();
                int buttons = 0;
                for (int i = 1; i < event.getPointerCount(); ++i) {
                    buttons |= getButtonForIndex(event, i);
                }
                int button = getButtonForIndex(event, id);
                if (action == MotionEvent.ACTION_POINTER_UP) {
                    buttons &= ~button;
                    dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                            ACTION_BUTTON_RELEASE, buttons, button, 0);
                } else {
                    dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                            ACTION_BUTTON_PRESS, buttons, button, 0);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int buttons = 0;
                for (int i = 1; i < event.getPointerCount(); ++i) {
                    buttons |= getButtonForIndex(event, i);
                }
                dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                        MotionEvent.ACTION_HOVER_MOVE, buttons, 0, 0);
                break;
            }
        }
        */
        switch (action) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP: {
                final long ts = SystemClock.uptimeMillis();
                final MotionEvent me = MotionEvent.obtain(ts, ts, action,
                        event.getX(), event.getY(), 0);
                final boolean r = super.onTouchEvent(me);
                me.recycle();
                return r;
            }

        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (isOwnEvent(event)) return false;
        if (isMouseEvent(event)) hideCursor();
        return false;
    }

    protected int mOverlayButtons = 0;
    protected int mVScroll = 0;

    protected boolean onOverlayButton(final View v, final MotionEvent event) {
        final String id = v.getTag().toString();
        if ("v_scroll".equals(id)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    final int diffY = ((int) event.getY() - mVScroll) / vScrollStep;
                    dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                            MotionEvent.ACTION_SCROLL, mOverlayButtons, 0, -diffY);
                    mVScroll += diffY * vScrollStep;
                    break;
                case MotionEvent.ACTION_DOWN:
                    mVScroll = (int) event.getY();
                    break;
            }
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                break;
            default:
                return true;
        }
        int button = 0;
        switch (id) {
            case "left":
                button = MotionEvent.BUTTON_PRIMARY;
                break;
            case "right":
                button = MotionEvent.BUTTON_SECONDARY;
                break;
            case "middle":
                button = MotionEvent.BUTTON_TERTIARY;
                break;
            default:
                return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOverlayButtons |= button;
                break;
            case MotionEvent.ACTION_UP:
                mOverlayButtons &= ~button;
                break;
        }
        dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                event.getAction(), mOverlayButtons, button, 0);
        return true;
    }

    protected MotionEvent mOwnEvent = null;

    protected boolean isOwnEvent(final MotionEvent event) {
        return event == mOwnEvent;
    }

    protected void dispatchEventToSibling(int x, int y, final int action,
                                          final int buttons, final int button, final int vScroll) {
        x += getLeft();
        y += getTop();
        final View v = getTargetView(x, y);
        if (v == null) return;
        mOwnEvent = obtainEvent(x - v.getLeft(), y - v.getTop(), action,
                buttons, button, vScroll);
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_MOVE:
                    v.dispatchTouchEvent(mOwnEvent);
                    break;
                case MotionEvent.ACTION_SCROLL:
                case MotionEvent.ACTION_HOVER_MOVE:
//            case MotionEvent.ACTION_BUTTON_PRESS:
//            case MotionEvent.ACTION_BUTTON_RELEASE:
                    v.dispatchGenericMotionEvent(mOwnEvent);
                    break;
            }
        } finally {
            try {
                mOwnEvent.recycle();
            } finally {
                mOwnEvent = null;
            }
        }
    }

    protected static MotionEvent obtainEvent(final float x, final float y, final int action,
                                             final int buttons, final int actionButton,
                                             final int vScroll) {
        // ActionButton can be set only by means of hack using reflection; I prefer to avoid it.
        // TODO: or not to do?.. Hack...
        final long ts = SystemClock.uptimeMillis();
        final MotionEvent.PointerProperties[] pp = {new MotionEvent.PointerProperties()};
        pp[0].id = 0;
        pp[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
        final MotionEvent.PointerCoords[] pc = {new MotionEvent.PointerCoords()};
        pc[0].x = x;
        pc[0].y = y;
        pc[0].pressure = 1.0f;
        pc[0].size = 1.0f;
        pc[0].setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);
        return MotionEvent.obtain(ts, ts, action, 1, pp, pc, 0, buttons,
                1.0f, 1.0f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
    }

    @Nullable
    protected View getTargetView(final int x, final int y) {
        final ViewGroup parent = (ViewGroup) getParent();
        final Rect b = new Rect();
        for (int i = 0; i < parent.getChildCount(); ++i) {
            final View v = parent.getChildAt(i);
            if (v == this) continue;
            v.getHitRect(b);
            if (b.contains(x, y)) return v;
        }
        return null;
    }
}
