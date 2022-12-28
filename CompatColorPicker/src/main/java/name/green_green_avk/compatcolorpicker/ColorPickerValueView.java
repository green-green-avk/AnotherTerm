package name.green_green_avk.compatcolorpicker;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ColorPickerValueView extends androidx.appcompat.widget.AppCompatSeekBar
        implements ColorPickerControl, ColorPickerExtraState {
    @Nullable
    private ColorPickerView.OnValueChanged onValueChanged = null;
    private final float[] hsv = new float[3];
    private int alpha;

    public ColorPickerValueView(final Context context) {
        super(context);
    }

    public ColorPickerValueView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerValueView(final Context context, final AttributeSet attrs,
                                final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private final GradientBarDrawable bg = new GradientBarDrawable(Color.BLACK, Color.WHITE);

    {
        setMax(255);
        setBackgroundDrawable(bg);
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
        refresh();
    }

    private void refresh() {
        setProgress((int) (hsv[2] * 255f + .5f));
        final float[] hs = new float[]{hsv[0], hsv[1], 1f};
        bg.setEndColor(Color.HSVToColor(hs));
    }

    private void notifyValue() {
        if (onValueChanged != null)
            onValueChanged.onValueChanged(getValue());
    }

    {
        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress,
                                          final boolean fromUser) {
                if (fromUser) {
                    hsv[2] = (float) progress / 255;
                    notifyValue();
                }
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        });
    }

    // Extra state

    private final HSVColorBox boxedHSV = new HSVColorBox(hsv);

    @Override
    @NonNull
    public Object getExtraState() {
        return boxedHSV;
    }

    @Override
    public void setExtraState(@Nullable final Object s, @ColorInt final int v) {
        if (s instanceof HSVColorBox) {
            boxedHSV.from((HSVColorBox) s);
            refresh();
        } else if (s instanceof ChannelColorBox && ((ChannelColorBox) s).channel == 3) {
            alpha = Color.alpha(v);
        } else {
            setValue(v);
        }
    }
}
