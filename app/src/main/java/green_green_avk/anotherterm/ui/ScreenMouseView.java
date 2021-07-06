package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import androidx.annotation.CheckResult;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import java.util.Arrays;

import green_green_avk.anotherterm.R;

public class ScreenMouseView extends ScrollableView {

    protected Drawable cursor = null;
    protected int cursorSize = 64;
    protected int vScrollStep = 16;
    protected int buttonsLayoutResId = 0;

    protected boolean visibleCursor = true;

    @Nullable
    protected View[] bypassTo = null;

    protected PopupWindow overlay = null;
    protected ViewGroup overlayView = null;
    protected View overlayButtonsView = null;

    // Just to make event splitting behave properly when setBypassTo() is in use.
    protected final View[] overlayMarginViews = new View[4];

    public ScreenMouseView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.screenMouseViewStyle, R.style.AppScreenMouseViewStyle);
    }

    public ScreenMouseView(final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AppScreenMouseViewStyle);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScreenMouseView(final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void init(final Context context, @Nullable final AttributeSet attrs,
                        final int defStyleAttr, final int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ScreenMouseView, defStyleAttr, defStyleRes);
        try {
//            cursor = a.getDrawable(R.styleable.ScreenMouseView_cursor);
            cursor = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ScreenMouseView_cursor,
                            R.drawable.ic_screen_mouse_cursor));
            cursorSize = a.getDimensionPixelSize(R.styleable.ScreenMouseView_cursorSize,
                    cursor == null ? cursorSize :
                            Math.max(cursor.getIntrinsicWidth(), cursor.getIntrinsicHeight()));
            vScrollStep = a.getDimensionPixelSize(R.styleable.ScreenMouseView_vScrollStep,
                    vScrollStep);
            buttonsLayoutResId = a.getResourceId(R.styleable.ScreenMouseView_buttonsLayout,
                    R.layout.screen_mouse_buttons);
        } finally {
            a.recycle();
        }
        setScrollScale(1, 1);
        mGestureDetector.setOnDoubleTapListener(null); // avoid interference with scroll
//        mGestureDetector.setContextClickListener(null);
        mGestureDetector.setIsLongpressEnabled(false);

        overlayView = new FrameLayout(context);
        overlayView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayView.setMotionEventSplittingEnabled(true);
        overlayView.setOnTouchListener(overlayOnTouch);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            overlayView.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        overlay = new PopupWindow(overlayView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.setSplitTouchEnabled(true);
        overlay.setAnimationStyle(android.R.style.Animation_Dialog);
    }

    @SuppressLint("ClickableViewAccessibility")
    protected final OnTouchListener overlayOnTouch = (v, event) -> true;

    @SuppressLint("ClickableViewAccessibility")
    protected final OnTouchListener overlayButtonsOnTouch = this::onOverlayButton;

    @SuppressLint("ClickableViewAccessibility")
    protected final OnTouchListener overlayMarginOnTouch = this::onOverlay;

    protected void applyButtons() {
        if (overlayButtonsView != null) return;

        overlayView.removeAllViewsInLayout();

        if (bypassTo != null && bypassTo.length > 0)
            for (int i = 0; i < 4; i++) {
                final View v = new View(getContext());
                v.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                overlayView.addView(v);
                v.setOnTouchListener(overlayMarginOnTouch);
                overlayMarginViews[i] = v;
            }
        else
            Arrays.fill(overlayMarginViews, null);

        overlayButtonsView = LayoutInflater.from(getContext()).inflate(buttonsLayoutResId,
                overlayView, false);
        overlayView.addView(overlayButtonsView);
        for (final View v : UiUtils.getIterable(overlayButtonsView)) {
            if (v.getTag() instanceof String) {
                v.setOnTouchListener(overlayButtonsOnTouch);
            }
        }
    }

    @CheckResult
    @LayoutRes
    public int getButtonsLayoutResId() {
        return buttonsLayoutResId;
    }

    public void setButtons(@LayoutRes final int resId) {
        if (resId == 0 || resId == buttonsLayoutResId) return;
        buttonsLayoutResId = resId;
        overlayButtonsView = null;
    }

    @Nullable
    public View[] getBypassTo() {
        return bypassTo;
    }

    /**
     * @param bypassTo - A list of views to bypass events under the mouse buttons overlay.
     */
    public void setBypassTo(@Nullable final View[] bypassTo) {
        this.bypassTo = bypassTo;
        overlayButtonsView = null;
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scrollPosition.x = (float) w / 2;
        scrollPosition.y = (float) h / 2;
        execOnScroll();
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
    public float getRightScrollLimit() {
        return getWidth();
    }

    @Override
    public float getBottomScrollLimit() {
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

    protected void onPointerMove() {
        dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                mOverlayButtons != 0 ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_HOVER_MOVE,
                mOverlayButtons, 0, 0);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        onPointerMove();
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        final boolean r = super.onScroll(e1, e2, -distanceX, -distanceY);
        onPointerMove();
        ViewCompat.postInvalidateOnAnimation(this);
        return r;
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        return super.onFling(e1, e2, -velocityX, -velocityY);
    }

    private final int[] _overlayLoc = new int[2];

    protected void setOverlayCoords(@NonNull final MotionEvent event) {
        overlayView.getLocationOnScreen(_overlayLoc);
        int width = overlayButtonsView.getLayoutParams().width;
        int height = overlayButtonsView.getLayoutParams().height;
        if (width <= 0) width = overlayButtonsView.getWidth();
        if (height <= 0) height = overlayButtonsView.getHeight();
        final float x = event.getRawX() - _overlayLoc[0] - (float) width / 2;
        final float y = event.getRawY() - _overlayLoc[1] - (float) height / 2;

        overlayButtonsView.setX(x);
        overlayButtonsView.setY(y);

        if (overlayMarginViews[0] != null)
            overlayMarginViews[0].setX(x - overlayMarginViews[0].getWidth());
        if (overlayMarginViews[1] != null)
            overlayMarginViews[1].setY(y - overlayMarginViews[1].getHeight());
        if (overlayMarginViews[2] != null)
            overlayMarginViews[2].setX(x + width);
        if (overlayMarginViews[3] != null)
            overlayMarginViews[3].setY(y + height);
    }

    @CheckResult
    public static boolean isMouseEvent(@Nullable final MotionEvent event) {
        return event != null &&
                event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER;
    }

    @SuppressLint("ClickableViewAccessibility")
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
                applyButtons();
                setOverlayCoords(event);
                overlay.showAtLocation(this, Gravity.NO_GRAVITY, 0, 0);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                overlay.dismiss();
                if (_bypassTarget != null) {
                    dispatchBypassEventCancel(_bypassTarget);
                    _bypassTarget = null;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                applyButtons();
                setOverlayCoords(event);
                break;
            }
        }
        switch (action) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP: {
                final long ts = SystemClock.uptimeMillis();
                final MotionEvent me = MotionEvent.obtain(ts, ts, action,
                        event.getX(), event.getY(), 0);
                final boolean r;
                try {
                    r = super.onTouchEvent(me);
                } finally {
                    me.recycle();
                }
                return r;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (isOwnEvent(event)) return false;
        if (isMouseEvent(event)) hideCursor();
        return false;
    }

    private final Rect _bypassVisible = new Rect();
    private final Point _bypassOffset = new Point();
    private final int[] _bypassRootLoc = new int[2];
    private View _bypassTarget;

    private static void dispatchBypassEventCancel(@NonNull final View view) {
        final long ts = SystemClock.uptimeMillis();
        final MotionEvent tEvent = MotionEvent.obtain(ts, ts,
                MotionEvent.ACTION_CANCEL, 0, 0, 0);
        try {
            view.dispatchTouchEvent(tEvent);
        } finally {
            tEvent.recycle();
        }
    }

    private boolean dispatchBypassEvent(@NonNull final View view,
                                        @NonNull final MotionEvent event,
                                        final boolean force) {
        view.getRootView().getLocationOnScreen(_bypassRootLoc);
        final float x = event.getRawX() - _bypassRootLoc[0];
        final float y = event.getRawY() - _bypassRootLoc[1];
        if (force || view.getGlobalVisibleRect(_bypassVisible, _bypassOffset)) {
            if (force || _bypassVisible.contains((int) x, (int) y)) {
                final MotionEvent tEvent = MotionEvent.obtain(event);
                try {
                    tEvent.setLocation(x - _bypassOffset.x, y - _bypassOffset.y);
                    if (view.dispatchTouchEvent(tEvent))
                        return true;
                } finally {
                    tEvent.recycle();
                }
            }
        }
        return false;
    }

    protected boolean onOverlay(@NonNull final View v, @NonNull final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (bypassTo != null)
                for (final View view : bypassTo) {
                    if (dispatchBypassEvent(view, event, false)) {
                        _bypassTarget = view;
                        return true;
                    }
                }
            _bypassTarget = null;
        } else if (_bypassTarget != null) {
            dispatchBypassEvent(_bypassTarget, event, true);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                _bypassTarget = null;
            }
        }
        return true;
    }

    protected int mOverlayButtons = 0;
    protected int mVScroll = 0;

    protected boolean onOverlayButton(@NonNull final View v, @NonNull final MotionEvent event) {
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
        final int button;
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
                return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOverlayButtons |= button;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mOverlayButtons &= ~button;
                break;
            default:
                return true;
        }
        dispatchEventToSibling((int) scrollPosition.x, (int) scrollPosition.y,
                event.getAction(), mOverlayButtons, button, 0);
        return true;
    }

    @Nullable
    protected MotionEvent mOwnEvent = null;

    @CheckResult
    protected boolean isOwnEvent(@Nullable final MotionEvent event) {
        return event != null && event == mOwnEvent;
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

    @CheckResult
    @NonNull
    protected static MotionEvent obtainEvent(final float x, final float y, final int action,
                                             final int buttons, final int actionButton,
                                             final int vScroll) {
        // ActionButton can be set only by means of hack using reflections; I prefer to avoid it.
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

    @CheckResult
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
