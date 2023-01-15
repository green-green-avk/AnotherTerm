package name.green_green_avk.compatcolorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.InflateException;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;

import java.util.LinkedList;
import java.util.List;

public final class ColorPickerView extends FrameLayout
        implements ColorPickerControl {
    @Nullable
    private ColorPickerView.OnValueChanged onValueChanged = null;
    @ColorInt
    private int color = Color.WHITE;
    @LayoutRes
    private int layout = ResourcesCompat.ID_NULL;
    private boolean hasAlpha = true;

    public interface OnValueChanged {
        void onValueChanged(@ColorInt int v);
    }

    public ColorPickerView(@NonNull final Context context) {
        super(context);
        initAttrs(context, null, 0, 0);
    }

    public ColorPickerView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs, 0, 0);
    }

    public ColorPickerView(@NonNull final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ColorPickerView(@NonNull final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initAttrs(@NonNull final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ColorPickerView,
                defStyleAttr, defStyleRes);
        try {
            layout = a.getResourceId(R.styleable.ColorPickerView_layout, layout);
            if (layout == ResourcesCompat.ID_NULL)
                throw new InflateException("`layout' attribute is missing");
            hasAlpha = a.getBoolean(R.styleable.ColorPickerView_hasAlpha, hasAlpha);
        } finally {
            a.recycle();
        }
        applyLayout();
    }

    private final List<ColorPickerControl> ctls = new LinkedList<>();

    private void adoptCtl(@NonNull final ColorPickerControl view) {
        ctls.add(view);
        view.setOnValueChanged(v -> {
            if (view instanceof ColorPickerExtraState) {
                final Object extraState = ((ColorPickerExtraState) view).getExtraState();
                setValue(v, extraState);
            } else {
                setValue(v);
            }
            notifyValue();
        });
    }

    private void applyLayout() {
        inflate(getContext(), layout, this);
        adoptCtl(findViewById(R.id.color_hs));
        adoptCtl(findViewById(R.id.color_v));
        adoptCtl(findViewById(R.id.color_a));
        adoptCtl(findViewById(R.id.color_r));
        adoptCtl(findViewById(R.id.color_g));
        adoptCtl(findViewById(R.id.color_b));
        adoptCtl(findViewById(R.id.color_result));
        setHasAlpha(hasAlpha);
    }

    private void reapplyLayout() {
        ctls.clear();
        removeAllViews();
        applyLayout();
    }

    @LayoutRes
    public int getLayout() {
        return layout;
    }

    public void setLayout(@LayoutRes final int layout) {
        this.layout = layout;
        reapplyLayout();
    }

    public boolean isHasAlpha() {
        return hasAlpha;
    }

    public void setHasAlpha(final boolean v) {
        hasAlpha = v;
        findViewById(R.id.color_a).setVisibility(hasAlpha ? VISIBLE : GONE);
        this.<ColorPickerTextView>findViewById(R.id.color_result).hasAlpha = hasAlpha;
        setValue(color);
    }

    @ColorInt
    public int getValue() {
        return color;
    }

    public void setOnValueChanged(@Nullable final ColorPickerView.OnValueChanged v) {
        onValueChanged = v;
    }

    public void setValue(@ColorInt int v) {
        if (!hasAlpha)
            v |= 0xFF000000;
        setValue(v, null);
    }

    private void setValue(@ColorInt final int v, @Nullable final Object s) {
        color = v;
        for (final ColorPickerControl ctl : ctls) {
            if (s != null && ctl instanceof ColorPickerExtraState)
                ((ColorPickerExtraState) ctl).setExtraState(s, v);
            else
                ctl.setValue(v);
        }
    }

    private void notifyValue() {
        if (onValueChanged != null)
            onValueChanged.onValueChanged(getValue());
    }
}
