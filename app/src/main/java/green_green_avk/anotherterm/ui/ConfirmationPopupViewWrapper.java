package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;

import green_green_avk.anotherterm.R;

public final class ConfirmationPopupViewWrapper extends WheelPopupViewWrapper {
    private static final int[] ATTRS_NONE = new int[]{};
    private static final int[] ATTRS_HIGHLIGHT = new int[]{android.R.attr.state_pressed};

    @Nullable
    private Drawable pointerDrawable = null;

    @Nullable
    private Runnable onConfirmListener = null;

    private final class PopupView extends View {
        private final PointF pointerPosition = new PointF(0f, 0f);

        public PopupView(final Context context) {
            super(context);
        }

        public PopupView(final Context context, @Nullable final AttributeSet attrs) {
            super(context, attrs);
        }

        public PopupView(final Context context, @Nullable final AttributeSet attrs,
                         final int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public PopupView(final Context context, @Nullable final AttributeSet attrs,
                         final int defStyleAttr, final int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        private float getR() {
            final float x = pointerPosition.x * 2 / getWidth();
            final float y = pointerPosition.y * 2 / getHeight();
            return (float) Math.sqrt(x * x + y * y);
        }

        private void setPointerPosition(@NonNull final MotionEvent event) {
            pointerPosition.set(event.getX() - getPopupRefX(), event.getY() - getPopupRefY());
        }

        private void setPointerPositionOut() {
            pointerPosition.set(-getPopupRefX(), -getPopupRefY());
        }

        @Override
        public boolean onHoverEvent(final MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_HOVER_MOVE:
                    setPointerPosition(event);
                    ViewCompat.postInvalidateOnAnimation(this);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    setPointerPositionOut();
                    ViewCompat.postInvalidateOnAnimation(this);
                    break;
            }
            return super.onHoverEvent(event);
        }

        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    setPointerPosition(event);
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    setPointerPosition(event);
                    ViewCompat.postInvalidateOnAnimation(this);
                    break;
            }
            return true;
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);
            if (pointerDrawable != null) {
                final float r = Math.min(getR(), 1f);
                final int xC = getPopupRefX();
                final int yC = getPopupRefY();
                final int xOff = Math.round(r * xC);
                final int yOff = Math.round(r * yC);
                pointerDrawable.setBounds(xC - xOff, yC - yOff,
                        xC + xOff, yC + yOff);
                pointerDrawable.setState(r >= 1f ? ATTRS_HIGHLIGHT : ATTRS_NONE);
                pointerDrawable.draw(canvas);
            }
        }
    }

    private final PopupView popupView;

    public ConfirmationPopupViewWrapper(@NonNull final View ownerView) {
        super(ownerView);
        popupView = new PopupView(getContext());
        popupView.setLayoutParams(new FrameLayout.LayoutParams(16, 16));
        setContentView(popupView);
    }

    @Override
    protected boolean onGestureEnd(@NonNull final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP &&
                popupView.getR() >= 1f && onConfirmListener != null)
            onConfirmListener.run();
        return true;
    }

    public void setPopupRadius(final int v) {
        popupView.getLayoutParams().width = v * 2;
        popupView.getLayoutParams().height = v * 2;
        popupView.requestLayout();
    }

    public void setPointerDrawable(@Nullable final Drawable v) {
        if (v != null)
            v.mutate();
        pointerDrawable = v;
    }

    public void setPointerBackgroundDrawable(@Nullable final Drawable v) {
        popupView.setBackgroundDrawable(v);
    }

    public void setOnConfirmListener(@Nullable final Runnable v) {
        onConfirmListener = v;
    }

    @Override
    public int getPopupRefX() {
        return popupView.getLayoutParams().width / 2;
    }

    @Override
    public int getPopupRefY() {
        return popupView.getLayoutParams().height / 2;
    }

    @Override
    protected int getAnimationStyle() {
        return R.style.Animation_WheelPopup;
    }
}
