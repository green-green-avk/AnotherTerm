package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;

import green_green_avk.anotherterm.R;
import name.green_green_avk.compatcolorpicker.ColorPickerView;

public final class ColorPickerPopupView extends AppCompatImageView
        implements ParameterView<Integer> {
    private final ColorDrawable sample = new ColorDrawable();
    @Nullable
    private AlertDialog dialog = null;

    private boolean hasAlpha = true;

    public ColorPickerPopupView(@NonNull final Context context) {
        super(context, null, R.attr.colorPickerPopupViewStyle);
        initAttrs(context, null, R.attr.colorPickerPopupViewStyle);
    }

    public ColorPickerPopupView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs, R.attr.colorPickerPopupViewStyle);
        initAttrs(context, attrs, R.attr.colorPickerPopupViewStyle);
    }

    public ColorPickerPopupView(@NonNull final Context context, @Nullable final AttributeSet attrs,
                                final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr);
    }

    private void initAttrs(@NonNull final Context context, @Nullable final AttributeSet attrs,
                           final int defStyleAttr) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ColorPickerPopupView,
                defStyleAttr, 0);
        try {
            hasAlpha = a.getBoolean(R.styleable.ColorPickerPopupView_hasAlpha,
                    hasAlpha);
        } finally {
            a.recycle();
        }
    }

    {
        setImageDrawable(sample);
        setOnClickListener(view -> {
            final View root = LayoutInflater.from(getContext())
                    .inflate(R.layout.color_picker_popup, null);
            final ColorPickerView colorPicker = root.findViewById(R.id.color_picker);
            colorPicker.setHasAlpha(hasAlpha);
            colorPicker.setValue(getValue());
            dialog = new AlertDialog.Builder(getContext())
                    .setOnDismissListener(d -> dialog = null)
                    .setView(root)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> {
                                setValue(colorPicker.getValue());
                                notifyValueChanged();
                            })
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> {
                            })
                    .setCancelable(true)
                    .show();
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        super.onDetachedFromWindow();
    }

    @CheckResult
    public boolean isHasAlpha() {
        return hasAlpha;
    }

    public void setHasAlpha(final boolean v) {
        hasAlpha = v;
    }

    @ColorInt
    @NonNull
    public Integer getValue() {
        return sample.getColor();
    }

    public void setValue(@ColorInt @Nullable final Integer v) {
        if (v == null)
            return;
        sample.setColor(v);
    }

    @Nullable
    private OnValueChanged<? super Integer> onValueChanged = null;

    private void notifyValueChanged() {
        if (onValueChanged != null)
            onValueChanged.onValueChanged(getValue());
    }

    @Override
    public void setOnValueChanged(@Nullable final OnValueChanged<? super Integer> v) {
        onValueChanged = v;
    }
}
