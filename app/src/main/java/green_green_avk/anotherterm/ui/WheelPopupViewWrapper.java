package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.CheckResult;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;

public abstract class WheelPopupViewWrapper {
    public abstract int getPopupRefX();

    public abstract int getPopupRefY();

    protected boolean onGestureEnd(@NonNull final MotionEvent event) {
        return false;
    }

    @Nullable
    private Runnable onShow = null;
    @Nullable
    private Runnable onHide = null;

    @NonNull
    protected final View ownerView;
    protected boolean showOnHover = false;

    private final int[] viewPos = new int[2];
    private final ExtPopupWindow window;
    private final Point windowPos = new Point(0, 0);
    private final ViewGroup contentViewWrapper;
    protected final PointF downPos = new PointF(0F, 0F);

    private void calcPos() {
        ownerView.getLocationInWindow(viewPos);
        windowPos.set(viewPos[0] + ownerView.getWidth() / 2 - getPopupRefX(),
                viewPos[1] + ownerView.getHeight() / 2 - getPopupRefY());
    }

    private boolean dispatchClick(@NonNull final MotionEvent event) {
        final float x = event.getX() + viewPos[0] - windowPos.x;
        final float y = event.getY() + viewPos[1] - windowPos.y;
        final long dt = SystemClock.uptimeMillis();
        boolean processed = false;
        final MotionEvent ed = MotionEvent.obtain(
                dt, dt, MotionEvent.ACTION_DOWN,
                x, y, 0);
        ed.setSource(InputDevice.SOURCE_CLASS_POINTER);
        if (window.getContentView().dispatchTouchEvent(ed)) {
            final MotionEvent eu = MotionEvent.obtain(
                    dt, dt, MotionEvent.ACTION_UP,
                    x, y, 0);
            eu.setSource(InputDevice.SOURCE_CLASS_POINTER);
            processed = window.getContentView().dispatchTouchEvent(eu);
            eu.recycle();
        }
        ed.recycle();
        return processed;
    }

    private void dispatchHover(@NonNull final MotionEvent event) {
        final float x = event.getX() + viewPos[0] - windowPos.x;
        final float y = event.getY() + viewPos[1] - windowPos.y;
        final long dt = SystemClock.uptimeMillis();
        final MotionEvent eh = MotionEvent.obtain(
                dt, dt, MotionEvent.ACTION_HOVER_MOVE,
                x, y, 0);
        eh.setSource(InputDevice.SOURCE_CLASS_POINTER);
        window.getContentView().dispatchGenericMotionEvent(eh);
        eh.recycle();
    }

    private final class ContentWrapperView extends FrameLayout {
        private ContentWrapperView(@NonNull final Context context) {
            super(context);
        }

        @Override
        protected void onLayout(final boolean changed, final int left, final int top,
                                final int right, final int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (changed) {
                calcPos();
                window.update(windowPos.x, windowPos.y, -1, -1);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public WheelPopupViewWrapper(@NonNull final View ownerView) {
        this.ownerView = ownerView;
        contentViewWrapper = new ContentWrapperView(getContext());
        contentViewWrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        window = new ExtPopupWindow(contentViewWrapper,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        window.setOnDismissListener(() -> {
            window.setFocusable(false);
            if (onHide != null)
                onHide.run();
        });
        // ===
        // Mandatory to make it dismissible by outside touch in APIs 21 -- 22
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // ===
        window.setClippingEnabled(false);
        window.setSplitTouchEnabled(true);
        window.setAnimationStyle(android.R.style.Animation_Dialog);
        contentViewWrapper.setOnGenericMotionListener((v, event) -> {
            if (showOnHover) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_HOVER_EXIT:
                        hide();
                        break;
                }
            }
            return false;
        });
        this.ownerView.setOnTouchListener((v, event) -> {
            boolean processed = false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    dispatchHover(event);
                    break;
                case MotionEvent.ACTION_UP:
                    processed |= dispatchClick(event);
                case MotionEvent.ACTION_CANCEL:
                    processed |= onGestureEnd(event);
                    if (processed)
                        hide();
                    else {
                        window.setFocusable(true);
                        window.update();
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    if (MotionEventCompat.isFromSource(event, InputDevice.SOURCE_MOUSE)
                            && (event.getButtonState() & MotionEvent.BUTTON_PRIMARY) == 0)
                        return false;
                    setDownPos(event);
                    show();
                    break;
            }
            return true;
        });
        this.ownerView.setOnGenericMotionListener((v, event) -> {
            if (showOnHover) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        setDownPos(event);
                        show();
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        dispatchHover(event);
                        break;
                }
            }
            return false;
        });
    }

    protected final void setDownPos(@NonNull final MotionEvent event) {
        downPos.set(event.getX() - ownerView.getWidth() / 2,
                event.getY() - ownerView.getHeight() / 2);
    }

    private void show() {
        if (!window.isShowing()) {
            calcPos();
            window.showAtLocation(ownerView,
                    Gravity.NO_GRAVITY, windowPos.x, windowPos.y);
            if (onShow != null)
                onShow.run();
        }
    }

    private void hide() {
        if (window.isShowing())
            window.dismiss();
    }

    @CheckResult
    @NonNull
    public final Context getContext() {
        return ownerView.getContext();
    }

    @CheckResult
    public final <T extends View> T getContentView() {
        return (T) contentViewWrapper.getChildAt(0);
    }

    public final void setContentView(@Nullable final View view) {
        contentViewWrapper.removeAllViewsInLayout();
        contentViewWrapper.addView(view);
    }

    public final void setContentView(@LayoutRes final int res) {
        contentViewWrapper.removeAllViewsInLayout();
        View.inflate(getContext(), res, contentViewWrapper);
    }

    public final boolean isShowOnHover() {
        return showOnHover;
    }

    public final void setShowOnHover(final boolean showOnHover) {
        this.showOnHover = showOnHover;
    }

    public final void setOnShow(@Nullable final Runnable v) {
        onShow = v;
    }

    public final void setOnHide(@Nullable final Runnable v) {
        onHide = v;
    }

    public final void dismiss() {
        hide();
    }

    public final void open() {
        window.setFocusable(true);
        show();
    }
}
