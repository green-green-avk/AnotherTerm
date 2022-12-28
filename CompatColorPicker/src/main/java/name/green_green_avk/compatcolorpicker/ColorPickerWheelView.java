package name.green_green_avk.compatcolorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ColorPickerWheelView extends androidx.appcompat.widget.AppCompatImageView
        implements ColorPickerControl, ColorPickerExtraState {
    @Nullable
    private ColorPickerView.OnValueChanged onValueChanged = null;
    private final float[] hsv = new float[3];
    private int alpha;
    @NonNull
    private Drawable marker;

    public ColorPickerWheelView(final Context context) {
        super(context);
    }

    public ColorPickerWheelView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerWheelView(final Context context, @Nullable final AttributeSet attrs,
                                final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        _setMarker(Utils.requireDrawable(getContext(), R.drawable.ccp_ic_marker2d));
        if (getDrawable() == null) {
            setScaleType(ScaleType.FIT_XY);
            setImageDrawable(new ColorPickerWheelDrawable(
                    Short.MAX_VALUE,
                    Short.MAX_VALUE
            ));
        }
    }

    private void _setMarker(@NonNull final Drawable marker) {
        this.marker = marker.mutate();
        this.marker.setBounds(0, 0,
                this.marker.getIntrinsicWidth(), this.marker.getIntrinsicHeight());
    }

    private void updateColor(final float x, final float y) {
        final double r = Math.sqrt(x * x + y * y);
        hsv[0] = (float) (Math.atan2(y, -x) / Math.PI * 180f) + 180f;
        hsv[1] = Math.max(0f, Math.min(1f, (float) r));
    }

    private float getWheelXF() {
        return (float) Math.cos(hsv[0] / 180f * Math.PI) * hsv[1];
    }

    private float getWheelYF() {
        return -(float) Math.sin(hsv[0] / 180f * Math.PI) * hsv[1];
    }

    private int getInnerWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getInnerHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private void updateColor(@NonNull final MotionEvent event) {
        final float x = (event.getX() - getPaddingLeft()) * 2 / getInnerWidth() - 1f;
        final float y = (event.getY() - getPaddingTop()) * 2 / getInnerHeight() - 1f;
        updateColor(x, y);
    }

    @NonNull
    public Drawable getMarker() {
        return marker;
    }

    public void setMarker(@NonNull final Drawable marker) {
        _setMarker(marker);
        invalidate();
    }

    @Override
    @ColorInt
    public int getValue() {
        return Color.HSVToColor(alpha, hsv);
    }

    @Override
    public void setOnValueChanged(@Nullable final ColorPickerView.OnValueChanged v) {
        onValueChanged = v;
    }

    @Override
    public void setValue(@ColorInt final int v) {
        alpha = Color.alpha(v);
        Color.colorToHSV(v, hsv);
        invalidate();
    }

    private void notifyValue() {
        if (onValueChanged != null)
            onValueChanged.onValueChanged(getValue());
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateColor(event);
                notifyValue();
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        final int w = getInnerWidth();
        final int h = getInnerHeight();
        final int sl = canvas.save();
        try {
            canvas.translate(
                    getPaddingLeft() +
                            (getWheelXF() + 1f) / 2 * w - (marker.getIntrinsicWidth() >> 1),
                    getPaddingTop() +
                            (getWheelYF() + 1f) / 2 * h - (marker.getIntrinsicHeight() >> 1)
            );
            marker.draw(canvas);
        } finally {
            canvas.restoreToCount(sl);
        }
    }

    // Extra state

    private final HSVColorBox boxedHSV = new HSVColorBox(hsv);

    @Override
    @Nullable
    public Object getExtraState() {
        return boxedHSV;
    }

    @Override
    public void setExtraState(@Nullable final Object s, @ColorInt final int v) {
        if (s instanceof HSVColorBox) {
            boxedHSV.from((HSVColorBox) s);
            invalidate();
        } else if (s instanceof ChannelColorBox && ((ChannelColorBox) s).channel == 3) {
            alpha = Color.alpha(v);
        } else {
            setValue(v);
        }
    }
}
