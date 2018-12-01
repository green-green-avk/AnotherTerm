package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import green_green_avk.anotherterm.R;

public class FancyButton extends android.support.v7.widget.AppCompatImageButton {
    protected float mTouchMaskDpi = 10;
    protected Bitmap mTouchMask = null;
    protected Drawable mTouchMaskSrc = null;

    public FancyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.fancyButtonStyle, R.style.AppFancyButtonStyle);
    }

    public FancyButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AppFancyButtonStyle);
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.FancyButton, defStyleAttr, defStyleRes);
        try {
//            mTouchMaskSrc = a.getDrawable(R.styleable.FancyButton_touchMask);
            mTouchMaskSrc = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.FancyButton_touchMask, 0));
            mTouchMaskDpi = a.getFloat(R.styleable.FancyButton_touchMaskDpi, mTouchMaskDpi);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setTouchMask(mTouchMaskSrc);
    }

    public Drawable getTouchMask() {
        return mTouchMaskSrc;
    }

    public void setTouchMask(final Drawable drawable) {
        mTouchMaskSrc = drawable;
        if (drawable != null) {
            if (getWidth() <= 0 || getHeight() <= 0) return;
            // TODO: Optimize for bitmaps & colors
            final DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            // https://issuetracker.google.com/issues/36940792
            // https://stackoverflow.com/questions/9247369/alpha-8-bitmaps-and-getpixel
            // https://android.googlesource.com/platform/frameworks/base/+/6260b22501996d2e7a0323b493ae6c4badb93c28%5E%21/core/jni/android/graphics/Bitmap.cpp
            // TODO: Or copyPixelsToBuffer() solution is better?
            mTouchMask = Bitmap.createBitmap(
                    (int) (getWidth() * mTouchMaskDpi / dm.xdpi),
                    (int) (getHeight() * mTouchMaskDpi / dm.ydpi),
                    Build.VERSION.SDK_INT < 23 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.ALPHA_8
            );
            final Canvas cv = new Canvas(mTouchMask);
            final Rect bb = drawable.copyBounds();
            drawable.setBounds(0, 0, cv.getWidth(), cv.getHeight());
            drawable.draw(cv);
            drawable.setBounds(bb);
        } else {
            mTouchMask = null;
        }
    }

    protected boolean isHit(int x, int y) {
        if (mTouchMask == null) return true;
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) return false;
        x = x * mTouchMask.getWidth() / getWidth();
        y = y * mTouchMask.getHeight() / getHeight();
        return Color.alpha(mTouchMask.getPixel(x, y)) != 0;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return isHit((int) event.getX(), (int) event.getY()) && super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return isHit((int) event.getX(), (int) event.getY()) && super.dispatchGenericMotionEvent(event);
    }
}
