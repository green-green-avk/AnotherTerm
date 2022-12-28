package name.green_green_avk.compatcolorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ColorPickerChannelView extends androidx.appcompat.widget.AppCompatSeekBar
        implements ColorPickerControl, ColorPickerExtraState {
    @Nullable
    private ColorPickerView.OnValueChanged onValueChanged = null;
    private final int[] bgra = new int[4];
    @IntRange(from = 0, to = 3)
    private int channel = 0;

    public ColorPickerChannelView(final Context context) {
        super(context);
        initAttrs(context, null, 0);
    }

    public ColorPickerChannelView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs, 0);
    }

    public ColorPickerChannelView(final Context context, final AttributeSet attrs,
                                  final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr);
    }

    private final GradientBarDrawable bg = new GradientBarDrawable(Color.BLACK, Color.WHITE);

    {
        setMax(255);
        setBackgroundDrawable(bg);
    }

    private void initAttrs(final Context context, final AttributeSet attrs,
                           final int defStyleAttr) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ColorPickerChannelView,
                defStyleAttr, 0);
        try {
            setChannel(a.getInteger(R.styleable.ColorPickerChannelView_channel,
                    channel));
        } finally {
            a.recycle();
        }
    }

    @IntRange(from = 0, to = 3)
    public int getChannel() {
        return channel;
    }

    public void setChannel(@IntRange(from = 0, to = 3) final int i) {
        channel = i;
        if (i == 3) {
            bg.setStartColor(Color.TRANSPARENT);
            bg.setEndColor(Color.WHITE);
        } else {
            bg.setStartColor(Color.BLACK);
            bg.setEndColor(Color.BLACK | (0xFF << (i * 8)));
        }
    }

    @Override
    @ColorInt
    public int getValue() {
        return Color.argb(bgra[3], bgra[2], bgra[1], bgra[0]);
    }

    @Override
    public void setOnValueChanged(@Nullable final ColorPickerView.OnValueChanged v) {
        onValueChanged = v;
    }

    @Override
    public void setValue(@ColorInt final int v) {
        bgra[0] = Color.blue(v);
        bgra[1] = Color.green(v);
        bgra[2] = Color.red(v);
        bgra[3] = Color.alpha(v);
        setProgress(bgra[channel]);
        if (channel == 3)
            bg.setEndColor(v | Color.BLACK);
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
                    bgra[channel] = progress;
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

    private final ChannelColorBox boxedColor = new ChannelColorBox(bgra);

    @Override
    @NonNull
    public Object getExtraState() {
        boxedColor.channel = channel;
        return boxedColor;
    }

    @Override
    public void setExtraState(@Nullable final Object s, @ColorInt final int v) {
        setValue(v);
    }
}
