package green_green_avk.anotherterm.ui;

/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/* Changed by Aleksandr Kiselev */

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.util.Pools;

import java.util.HashSet;
import java.util.Set;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.WeakHandler;

/**
 * A view that renders a screen {@link ExtKeyboard}.
 * It handles rendering of keys and detecting key presses and touch movements.
 */
public abstract class ExtKeyboardView extends View /*implements View.OnClickListener*/ {
    public static final int SHIFT = ExtKeyboard.SHIFT;
    public static final int ALT = ExtKeyboard.ALT;
    public static final int CTRL = ExtKeyboard.CTRL;

    public interface OnKeyboardActionListener {

        /**
         * Called when the user presses a key. This is sent before the {@link #onKey} is called.
         * For keys that repeat, this is only called once.
         *
         * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
         *                    key, the value will be zero.
         */
        void onPress(int primaryCode);

        /**
         * Called when the user releases a key. This is sent after the {@link #onKey} is called.
         * For keys that repeat, this is only called once.
         *
         * @param primaryCode the code of the key that was released
         */
        void onRelease(int primaryCode);

        /**
         * Send a key press to the listener.
         *
         * @param primaryCode   this is the key that was pressed
         * @param modifiers     modifiers values to enforce
         * @param modifiersMask modifiers to enforce
         */
        void onKey(int primaryCode, int modifiers, int modifiersMask);

        /**
         * Sends a sequence of characters to the listener.
         *
         * @param text the sequence of characters to be displayed.
         */
        void onText(CharSequence text);
    }

    /* To be overridden */
    public boolean getAutoRepeat() {
        return true;
    }

    /* We cannot use the View visibility property here
    because invisible View cannot receive focus and thus hardware keyboard events */
    protected boolean mHidden = false;
    protected ExtKeyboard mKeyboard = null;

    protected FontProvider fontProvider = new DefaultConsoleFontProvider();

    protected final Paint mPaint = new Paint();
    protected final Rect mKeyPadding = new Rect(0, 0, 0, 0);
    protected final Rect mLedPadding = new Rect(0, 0, 0, 0);

    protected boolean mAutoRepeatAllowed = true; // widget own option
    protected int mAutoRepeatDelay;
    protected int mAutoRepeatInterval;
    protected int mPopupDelay;
    protected TypedValue mPopupKeySizeTyped = new TypedValue();
    protected ColorStateList mPopupKeyTextColor;
    protected int mPopupShadowColor;
    protected float mPopupShadowRadius;

    protected int mLabelTextSize;
    protected int mKeyTextSize;
    protected int mKeyTextColor;
    protected float mShadowRadius;
    protected int mShadowColor;
    protected Drawable mKeyBackground;
    protected Drawable mLedBackground;
    protected Drawable mPopupBackground;
    protected Drawable mPopupKeyBackground;

    protected int mVerticalCorrection;

    /**
     * Listener for {@link OnKeyboardActionListener}.
     */
    protected OnKeyboardActionListener mKeyboardActionListener = null;

    /**
     * The dirty region in the keyboard bitmap
     */
    protected Rect mDirtyRect = new Rect();
    /**
     * The keyboard bitmap for faster updates
     */
    protected Bitmap mBuffer = null;
    /**
     * The canvas for the above mutable keyboard bitmap
     */
    protected Canvas mCanvas;

    /**
     * Selected modifiers state
     */
    protected int mModifiers = 0;

    /**
     * LEDs control
     */
//    protected final SparseBooleanArray leds = new SparseBooleanArray();
    protected final Set<Integer> leds = new HashSet<>();

    protected final KeyTouchMap mTouchedKeys = new KeyTouchMap();

    protected static final int MSG_REPEAT = 1;

    protected WeakHandler mHandler = new WeakHandler();

    public ExtKeyboardView(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, R.attr.extKeyboardViewStyle);
    }

    public ExtKeyboardView(final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs,
                defStyleAttr, R.style.AppExtKeyboardViewStyle);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ExtKeyboardView(final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(final Context context, @Nullable final AttributeSet attrs,
                      final int defStyleAttr, final int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ExtKeyboardView, defStyleAttr, defStyleRes);
        try {
//            mKeyBackground = a.getDrawable(R.styleable.ExtKeyboardView_keyBackground);
            mKeyBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_keyBackground, 0));
            mLedBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_ledBackground, 0));
            mVerticalCorrection = a.getDimensionPixelOffset(R.styleable.ExtKeyboardView_verticalCorrection, 0);
            mKeyTextSize = a.getDimensionPixelSize(R.styleable.ExtKeyboardView_keyTextSize, 18);
            mKeyTextColor = a.getColor(R.styleable.ExtKeyboardView_keyTextColor, 0xFF000000);
            mLabelTextSize = a.getDimensionPixelSize(R.styleable.ExtKeyboardView_labelTextSize, 14);
            mShadowColor = a.getColor(R.styleable.ExtKeyboardView_shadowColor, 0);
            mShadowRadius = a.getFloat(R.styleable.ExtKeyboardView_shadowRadius, 0f);
            mAutoRepeatDelay = a.getInteger(R.styleable.ExtKeyboardView_autoRepeatDelay, 1000);
            mAutoRepeatInterval = a.getInteger(R.styleable.ExtKeyboardView_autoRepeatInterval, 100);
//            mPopupBackground = a.getDrawable(R.styleable.ExtKeyboardView_popupBackground);
            mPopupBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_popupBackground, 0));
            mPopupDelay = a.getInteger(R.styleable.ExtKeyboardView_popupDelay, 100);
            mPopupKeyBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_popupKeyBackground, 0));
            a.getValue(R.styleable.ExtKeyboardView_popupKeySize, mPopupKeySizeTyped);
            mPopupKeyTextColor = a.getColorStateList(R.styleable.ExtKeyboardView_popupKeyTextColor);
            mPopupShadowColor = a.getColor(R.styleable.ExtKeyboardView_popupShadowColor, 0);
            mPopupShadowRadius = a.getFloat(R.styleable.ExtKeyboardView_popupShadowRadius, 0f);
        } finally {
            a.recycle();
        }

        mPaint.setAntiAlias(true);
        mPaint.setColor(mKeyTextColor);
        mPaint.setTextSize(mKeyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);

        mKeyBackground.getPadding(mKeyPadding);
        mLedBackground.getPadding(mLedPadding);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler = new WeakHandler() {
            @Override
            public void handleMessage(@NonNull final Message msg) {
                switch (msg.what) {
                    case MSG_REPEAT: {
                        final KeyTouchState keyState = mTouchedKeys.getRepeatable();
                        if (keyState != null && keyState.isPressed && mAutoRepeatAllowed && getAutoRepeat()) {
                            sendEmptyMessageDelayed(MSG_REPEAT, mAutoRepeatInterval);
                            sendKeyUp(keyState.key, mModifiers);
                        }
                        break;
                    }
                }
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler = new WeakHandler();
        super.onDetachedFromWindow();
    }

    public void setFont(@NonNull final FontProvider fp) {
        fontProvider = fp;
        invalidateAllKeys();
    }

    public boolean isAutoRepeatAllowed() {
        return mAutoRepeatAllowed;
    }

    public int getAutoRepeatDelay() {
        return mAutoRepeatDelay;
    }

    public int getAutoRepeatInterval() {
        return mAutoRepeatInterval;
    }

    public void setAutoRepeatAllowed(final boolean autoRepeatAllowed) {
        this.mAutoRepeatAllowed = autoRepeatAllowed;
    }

    public void setAutoRepeatDelay(final int repeatDelay) {
        this.mAutoRepeatDelay = repeatDelay;
    }

    public void setAutoRepeatInterval(final int repeatInterval) {
        this.mAutoRepeatInterval = repeatInterval;
    }

    protected boolean isHidden() {
        return mHidden;
    }

    protected void setHidden(final boolean hidden) {
        mHidden = hidden;
        requestLayout();
        invalidateAllKeys();
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     *
     * @param keyboard the keyboard to display in this view
     * @see ExtKeyboard
     * @see #getKeyboard()
     */
    public void setKeyboard(final ExtKeyboard keyboard) {
        mHandler.removeMessages(MSG_REPEAT);
        cancelKeys();
        mKeyboard = keyboard;
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        requestLayout();
    }

    /**
     * Returns the current keyboard being displayed by this view.
     *
     * @return the currently attached keyboard
     * @see #setKeyboard(ExtKeyboard)
     */
    public ExtKeyboard getKeyboard() {
        return mKeyboard;
    }

    public void setOnKeyboardActionListener(final OnKeyboardActionListener listener) {
        mKeyboardActionListener = listener;
    }

    /**
     * Returns the {@link OnKeyboardActionListener} object.
     *
     * @return the listener attached to this keyboard
     */
    public OnKeyboardActionListener getOnKeyboardActionListener() {
        return mKeyboardActionListener;
    }

    public void setModifiers(final int v) {
        this.mModifiers = v;
        mTouchedKeys.invalidate();
    }

    public int getModifiers() {
        return mModifiers;
    }

    @NonNull
    protected ExtKeyboard.KeyFcn getModifiersAltKeyFcn(@NonNull final ExtKeyboard.Key key) {
        final ExtKeyboard.KeyFcn fcn = key.getModifierFunction(mModifiers);
        if (fcn == null)
            return key.getBaseFcn();
        return fcn;
    }

    public void setLedsByCode(final int code, final boolean on) {
        if (on) leds.add(code);
        else leds.remove(code);
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     *
     * @see #invalidateKey(ExtKeyboard.Key)
     */
    public void invalidateAllKeys() {
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        invalidate();
    }

    public void invalidateModifierKeys(final int code) {
        if (mKeyboard != null)
            for (final ExtKeyboard.Key key : mKeyboard.getKeysByCode(code))
                invalidateKey(key);
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     *
     * @param key the key in the attached {@link ExtKeyboard}.
     * @see #invalidateAllKeys()
     */
    public void invalidateKey(@NonNull final ExtKeyboard.Key key) {
        invalidateKey(key, mTouchedKeys.isPressed(key));
    }

    protected void invalidateKey(@NonNull final ExtKeyboard.Key key, final boolean pressed) {
        onBufferDrawKey(key, pressed);
        invalidate(key.x + getPaddingLeft(), key.y + getPaddingTop(),
                key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
    }

    @Nullable
    private static ExtKeyboard.KeyFcn getKeyFcn(@NonNull final ExtKeyboard.Key key,
                                                final int modifiers) {
        final ExtKeyboard.KeyFcn fcn = key.getModifierFunction(modifiers);
        if (fcn == null) return key.getModifierFunction(0);
        return fcn;
    }

    protected void sendKeyDown(@NonNull final ExtKeyboard.Key key, final int modifiers) {
        if (mKeyboardActionListener == null) return;
        final ExtKeyboard.KeyFcn fcn = getKeyFcn(key, modifiers);
        if (fcn != null && fcn.code != ExtKeyboard.KEYCODE_NONE)
            mKeyboardActionListener.onPress(fcn.code);
    }

    protected void sendKeyUp(@NonNull final ExtKeyboard.Key key, final int modifiers) {
        if (mKeyboardActionListener == null) return;
        final ExtKeyboard.KeyFcn fcn = getKeyFcn(key, modifiers);
        if (fcn != null) {
            if (fcn.code != ExtKeyboard.KEYCODE_NONE) {
                mKeyboardActionListener.onKey(fcn.code,
                        fcn.modifiers, fcn.modifiersMask);
                mKeyboardActionListener.onRelease(fcn.code);
            }
            if (fcn.text != null)
                mKeyboardActionListener.onText(fcn.text);
        }
    }

    @Nullable
    protected ExtKeyboard.Key getKey(final float x, final float y) {
        return mKeyboard.getKey(
                (int) x + getPaddingLeft(),
                (int) y + getPaddingTop() + mVerticalCorrection
        );
    }

    protected final class KeyTouchMap {
        private final SparseArray<KeyTouchState> map = new SparseArray<>();
        private final Pools.Pool<KeyTouchState> pool = new Pools.SimplePool<>(16);

        @Nullable
        public KeyTouchState get(final int id) {
            return map.get(id);
        }

        @NonNull
        public KeyTouchState put(final int id, @NonNull final ExtKeyboard.Key key,
                                 final float x, final float y) {
            KeyTouchState s = pool.acquire();
            if (s == null) {
                s = new KeyTouchState();
                s.popup = new Popup();
            }
            s.key = key;
            s.coords.x = x;
            s.coords.y = y;
            s.isPressed = true;
            s.popup.setKeyState(s);
            map.put(id, s);
            s.popup.show();
            return s;
        }

        protected void removeAt(final int pos) {
            final KeyTouchState s = map.valueAt(pos);
            if (s != null) {
                s.popup.hide();
                map.removeAt(pos);
                pool.release(s);
            }
        }

        public void remove(final int id) {
            removeAt(map.indexOfKey(id));
        }

        public void filter(@NonNull final KeyTouchStateFilter filter) {
            for (int i = 0; i < map.size(); ++i) {
                if (filter.onElement(map.indexOfKey(i), map.valueAt(i))) {
                    removeAt(i);
                }
            }
        }

        public boolean isPressed(final ExtKeyboard.Key key) {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                if (s.key == key && s.isPressed) return true;
            }
            return false;
        }

        @Nullable
        public KeyTouchState getRepeatable() {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                if (s.key == null) continue;
                if (s.key.repeatable) return s;
            }
            return null;
        }

        public void invalidate() {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                s.popup.invalidate();
            }
        }
    }

    protected static final class KeyTouchState {
        public ExtKeyboard.Key key = null;
        public final PointF coords = new PointF();
        public boolean isPressed = true;
        public Popup popup = null;
    }

    protected interface KeyTouchStateFilter {
        boolean onElement(int id, @NonNull KeyTouchState keyState);
    }

    protected final KeyTouchStateFilter cancelKeysFilter = (id, keyState) -> {
        releaseKey(keyState);
        return true;
    };

    protected void cancelKeys() {
        mTouchedKeys.filter(cancelKeysFilter);
    }

    protected void releaseKey(@NonNull final KeyTouchState keyState) {
        final ExtKeyboard.KeyFcn fcn = keyState.popup.getAltKeyFcn();
        if (fcn != null) {
            sendKeyUp(keyState.key, fcn.modifiers);
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (mKeyboard == null) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {
                final int index = event.getActionIndex();
                final ExtKeyboard.Key key = getKey(event.getX(index), event.getY(index));
                if (key == null || key.type != ExtKeyboard.Key.KEY)
                    return super.onTouchEvent(event);
                final boolean first = !mTouchedKeys.isPressed(key);
                mTouchedKeys.put(event.getPointerId(index),
                        key, event.getX(index), event.getY(index));
                if (mTouchedKeys.getRepeatable() != null) {
                    mHandler.removeMessages(MSG_REPEAT);
                    mHandler.sendEmptyMessageDelayed(MSG_REPEAT, mAutoRepeatDelay);
                }
                if (first)
                    sendKeyDown(key, mModifiers);
                invalidateKey(key, true);
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                final int index = event.getActionIndex();
                final KeyTouchState keyState = mTouchedKeys.get(event.getPointerId(index));
                if (keyState == null) return super.onTouchEvent(event);
                mTouchedKeys.remove(event.getPointerId(index));
                if (keyState.isPressed && keyState.key != null && !mTouchedKeys.isPressed(keyState.key)) {
                    invalidateKey(keyState.key, false);
                }
                releaseKey(keyState);
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); ++i) {
                    final KeyTouchState keyState = mTouchedKeys.get(event.getPointerId(i));
                    if (keyState == null) return super.onTouchEvent(event);
                    keyState.popup.addPointer(event.getX(i), event.getY(i));
                    final boolean oip = keyState.isPressed;
                    keyState.isPressed =
                            getModifiersAltKeyFcn(keyState.key) == keyState.popup.getAltKeyFcn();
                    if (keyState.key != null && keyState.isPressed != oip)
                        invalidateKey(keyState.key, keyState.isPressed);
                }
                return true;
            case MotionEvent.ACTION_CANCEL: {
                cancelKeys();
                invalidateAllKeys();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public int getDesiredWidth() {
        return getPaddingLeft() + getPaddingRight() + ((mKeyboard == null) ? 0 : mKeyboard.getMinWidth());
    }

    public int getDesiredHeight() {
        return getPaddingTop() + getPaddingBottom() + ((mKeyboard == null) ? 0 : mKeyboard.getHeight());
    }

    protected static int getDefaultSize(int desiredSize, final int measureSpec, final int layoutSize) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        if (layoutSize >= 0) desiredSize = layoutSize;
        switch (specMode) {
            case MeasureSpec.EXACTLY:
                return specSize;
            case MeasureSpec.AT_MOST:
                if (layoutSize == ViewGroup.LayoutParams.MATCH_PARENT) {
                    return specSize;
                }
                return Math.min(desiredSize, specSize);
            case MeasureSpec.UNSPECIFIED:
            default:
                return desiredSize;
        }
    }

    @Override
    public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mHidden) {
            if (Build.VERSION.SDK_INT >= 28) setMeasuredDimension(
                    getDefaultSize(getDesiredWidth(), widthMeasureSpec, getLayoutParams().width),
                    1 // for API >= 28 must have nonzero size to be focusable
            );
            else
                setMeasuredDimension(0, 0);
            return;
        }
        setMeasuredDimension(
                getDefaultSize(getDesiredWidth(), widthMeasureSpec, getLayoutParams().width),
                getDefaultSize(getDesiredHeight(), heightMeasureSpec, getLayoutParams().height)
        );
    }

    @Override
    public void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mKeyboard != null && !mHidden) {
            mKeyboard.resize(getContext(),
                    w - getPaddingLeft() - getPaddingRight(),
                    h - getPaddingTop() - getPaddingBottom());
        }
        cancelKeys();
        if (mBuffer != null) {
            mBuffer.recycle();
            mBuffer = null;
        }
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (mHidden) {
            if (Build.VERSION.SDK_INT >= 28) getBackground().draw(canvas); // nonzero size
            return;
        }
        if (!mDirtyRect.isEmpty() || mBuffer == null) {
            onBufferDraw();
        }
        canvas.drawBitmap(mBuffer, 0, 0, null);
    }

    protected void onBufferDraw() {
        if (mBuffer == null ||
                (mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight())) {
            // Make sure our bitmap is at least 1x1
            final int width = Math.max(1, getWidth());
            final int height = Math.max(1, getHeight());
            if (mBuffer != null)
                mBuffer.recycle();
            mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBuffer);
            mDirtyRect.union(0, 0, getWidth(), getHeight());
        }
        final Canvas canvas = mCanvas;
        canvas.save();
        canvas.clipRect(mDirtyRect);
        getBackground().draw(canvas);

        if (mKeyboard == null) return;

        for (final ExtKeyboard.Key key : mKeyboard.getKeys()) {
            final int left = getPaddingLeft() + key.x;
            final int top = getPaddingTop() + key.y;
            if (canvas.quickReject(left, top, left + key.width, top + key.height,
                    Canvas.EdgeType.AA))
                continue;
            onBufferDrawKey(key, mTouchedKeys.isPressed(key));
        }

        canvas.restore();
        mDirtyRect.setEmpty();
    }

    protected void onBufferDrawKey(final ExtKeyboard.Key key, final boolean pressed) {
        final Canvas canvas = mCanvas;
        if (canvas == null) return;
        final Paint paint = mPaint;
        final Drawable background = key.type == ExtKeyboard.Key.LED ?
                mLedBackground : mKeyBackground;
        final Rect padding = key.type == ExtKeyboard.Key.LED ?
                mLedPadding : mKeyPadding;
        final int left = getPaddingLeft() + key.x;
        final int top = getPaddingTop() + key.y;

        final ExtKeyboard.KeyFcn keyFcn = getKeyFcn(key, mModifiers);
        final int[] drawableState = ExtKeyboard.Key.getKeyState(pressed,
                keyFcn != null && leds.contains(keyFcn.code));
        background.setState(drawableState);

        final Rect bounds = background.getBounds();
        if (key.width != bounds.right ||
                key.height != bounds.bottom) {
            background.setBounds(0, 0, key.width, key.height);
        }

        canvas.save();

        canvas.translate(left, top);
        canvas.clipRect(0, 0, key.width, key.height);
        getBackground().draw(canvas);
        background.draw(canvas);

        if (keyFcn == null) {
            canvas.restore();
            return;
        }

        if (keyFcn.label != null) {
            if (keyFcn.label.length() < 2) {
                paint.setTextSize(mKeyTextSize);
                fontProvider.setPaint(paint, Typeface.BOLD);
            } else {
                paint.setTextSize(mLabelTextSize);
                fontProvider.setPaint(paint, Typeface.NORMAL);
            }

            final float labelX = (float) (key.width - padding.left - padding.right) / 2
                    + padding.left;
            final float labelY = (float) (key.height - padding.top - padding.bottom) / 2
                    + (paint.getTextSize() - paint.descent()) / 2 + padding.top;

            paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);
            if (key.showBothLabels) {
                final float loX = labelX / 2;
                final float loY = labelY / 2;
                canvas.drawText(keyFcn.label.toString(), labelX, labelY, paint);
                paint.setTextSize(paint.getTextSize() / 2);
                canvas.drawText(
                        key.functions.get(SHIFT - (mModifiers & SHIFT)).label.toString(),
                        labelX + loX,
                        labelY + (((mModifiers & SHIFT) == 0) ? -loY : (loY / 2)),
                        paint
                );
            } else canvas.drawText(keyFcn.label.toString(), labelX, labelY, paint);
            paint.clearShadowLayer();
        } else if (keyFcn.icon != null) {
            final int drawableX = (key.width - padding.left - padding.right
                    - keyFcn.icon.getIntrinsicWidth()) / 2 + padding.left;
            final int drawableY = (key.height - padding.top - padding.bottom
                    - keyFcn.icon.getIntrinsicHeight()) / 2 + padding.top;
            canvas.translate(drawableX, drawableY);
            keyFcn.icon.setBounds(0, 0,
                    keyFcn.icon.getIntrinsicWidth(), keyFcn.icon.getIntrinsicHeight());
            keyFcn.icon.draw(canvas);
            canvas.translate(-drawableX, -drawableY);
        }

        canvas.restore();
    }

    protected class Popup {
        protected final int mPopupKeySize = (int) UiUtils.getDimensionOrFraction(
                mPopupKeySizeTyped,
                getResources().getDisplayMetrics(),
                mKeyboard.getKeyWidth(), getWidth(),
                mKeyboard.getKeyWidth());
        protected final View view = new View(getContext());
        protected final PopupWindow window = new PopupWindow(view,
                mPopupKeySize * 4, mPopupKeySize * 4);
        protected final WeakHandler mHandler = new WeakHandler();
        protected final int[] mWindowCoords = new int[2];
        protected KeyTouchState keyState = null;
        protected float ptrA = Float.NaN;
        protected float ptrD = 0f;
        protected float ptrStep = 0f;
        protected ExtKeyboard.KeyFcn keyFcn = null;

        {
            window.setClippingEnabled(false);
            window.setSplitTouchEnabled(false);
            window.setAnimationStyle(android.R.style.Animation_Dialog);
        }

        public void setKeyState(@NonNull final KeyTouchState keyState) {
            this.keyState = keyState;
            this.ptrA = Float.NaN;
            this.ptrD = 0f;
            this.keyFcn = getModifiersAltKeyFcn(this.keyState.key);
            if (this.keyState.key != null)
                this.ptrStep = (float) Math.PI * 2 /
                        (this.keyState.key.functionsCircularPos.length);
        }

        protected class View extends android.view.View {
            protected final Paint mPaint = new Paint();
            protected final float mBaseTextSize = mPopupKeySize * 0.8f;
            protected final float mBaseTextSize2 = mBaseTextSize * 0.8f;
            protected float mFontHeight;

            public View(final Context context) {
                super(context);
                setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                mPaint.setAntiAlias(true);
                mPaint.setColor(mKeyTextColor);
                mPaint.setTextAlign(Align.CENTER);
                mPaint.setAlpha(255);
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setTextSize(mBaseTextSize);
                fontProvider.setPaint(mPaint, Typeface.BOLD);
                mFontHeight = mPaint.getFontSpacing();
            }

            @Override
            protected void onDraw(final Canvas canvas) {
                super.onDraw(canvas);
                if (keyState.key == null || keyState.key.functions.size() < 2) return;
                canvas.save();
                if (mPopupBackground != null) {
                    mPopupBackground.setBounds(0, 0, getWidth(), getHeight());
                    mPopupBackground.draw(canvas);
                }
                canvas.translate(getWidth() / 2f, getHeight() / 2f);
                mPaint.setShadowLayer(mPopupShadowRadius, 0, 0,
                        mPopupShadowColor);
                for (final ExtKeyboard.KeyFcn keyFcn : keyState.key.functions) {
                    final PointF coords = _getAltKeyFcnCoords(keyFcn);
                    final int[] state = ExtKeyboard.Key.getKeyState(
                            keyFcn == getAltKeyFcn(), false);
                    canvas.save();
                    canvas.translate(coords.x, coords.y);
                    canvas.save();
                    canvas.translate(-mFontHeight / 2, -mFontHeight / 2);
                    mPopupKeyBackground.mutate().setState(state);
                    mPopupKeyBackground.setBounds(0, 0,
                            (int) mFontHeight, (int) mFontHeight);
                    canvas.clipRect(0, 0, mFontHeight, mFontHeight);
                    mPopupKeyBackground.draw(canvas);
                    canvas.restore();
                    if (keyFcn.label != null) {
                        if (mPopupKeyTextColor != null)
                            mPaint.setColor(mPopupKeyTextColor.getColorForState(state,
                                    mPopupKeyTextColor.getDefaultColor()));
                        mPaint.setTextSize(mBaseTextSize2 / keyFcn.label.length());
                        canvas.drawText(keyFcn.label.toString(),
                                0, (mPaint.getTextSize() - mPaint.descent()) / 2,
                                mPaint);
                    }
                    canvas.restore();
                }
                mPaint.clearShadowLayer();
                canvas.restore();
            }
        }

        protected final Runnable rShow = () -> {
            if (keyState == null) return;
            ExtKeyboardView.this.getLocationInWindow(mWindowCoords);
            window.showAtLocation(ExtKeyboardView.this, Gravity.NO_GRAVITY,
                    (int) (mWindowCoords[0] + keyState.coords.x - window.getWidth() / 2),
                    (int) (mWindowCoords[1] + keyState.coords.y - window.getHeight() / 2));
        };

        public void show() {
            mHandler.postDelayed(rShow, mPopupDelay);
        }

        public void hide() {
            mHandler.removeCallbacks(rShow);
            window.dismiss();
        }

        public void addPointer(final float x, final float y) {
            final float dx = x - keyState.coords.x;
            final float dy = y - keyState.coords.y;
            ptrD = dx * dx + dy * dy;
            ptrA = (float) (Math.PI + Math.atan2(dy, dx));
            final ExtKeyboard.KeyFcn oFcn = keyFcn;
            keyFcn = _getAltKeyFcn();
            if (keyFcn != oFcn) view.invalidate();
        }

        @Nullable
        public ExtKeyboard.KeyFcn getAltKeyFcn() {
            return keyFcn;
        }

        public void invalidate() {
            keyFcn = _getAltKeyFcn();
            view.invalidate();
        }

        @Nullable
        protected ExtKeyboard.KeyFcn _getAltKeyFcn() {
            if (ptrD < mPopupKeySize * mPopupKeySize)
                return getModifiersAltKeyFcn(keyState.key);
            final ExtKeyboard.KeyFcn fcn =
                    keyState.key.getCircularKeyFcn((int) (ptrA / ptrStep));
            if (fcn == null)
                return null;
            if (fcn == getModifiersAltKeyFcn(keyState.key))
                return keyState.key.getBaseFcn();
            return fcn;
        }

        protected final PointF _altKeyFcnCoords = new PointF();

        protected PointF _getAltKeyFcnCoords(@NonNull ExtKeyboard.KeyFcn fcn) {
            if (fcn == keyState.key.getBaseFcn()) fcn = getModifiersAltKeyFcn(keyState.key);
            else if (fcn == getModifiersAltKeyFcn(keyState.key)) fcn = keyState.key.getBaseFcn();
            float pos = fcn.iconCircularPos;
            if (Float.isNaN(pos)) {
                _altKeyFcnCoords.x = 0;
                _altKeyFcnCoords.y = 0;
            } else {
                _altKeyFcnCoords.x = (float) -Math.cos(pos) * mPopupKeySize * 1.5f;
                _altKeyFcnCoords.y = (float) -Math.sin(pos) * mPopupKeySize * 1.5f;
            }
            return _altKeyFcnCoords;
        }
    }
}
