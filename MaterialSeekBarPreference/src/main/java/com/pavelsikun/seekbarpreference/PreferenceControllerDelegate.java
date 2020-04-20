package com.pavelsikun.seekbarpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.AnyRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

/**
 * Created by Pavel Sikun on 28.05.16.
 * <p>
 * Changed by Aleksandr Kiselev.
 */

@SuppressWarnings("WeakerAccess")
final class PreferenceControllerDelegate implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = PreferenceControllerDelegate.class.getSimpleName();
    static final String NS_ANDROID = "http://schemas.android.com/apk/res/android";

    static final int DEFAULT_CURRENT_VALUE = 50;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_INTERVAL = 1;
    private static final boolean DEFAULT_DIALOG_ENABLED = true;
    private static final boolean DEFAULT_IS_ENABLED = true;

    private int maxValue = DEFAULT_MAX_VALUE;
    private int minValue = DEFAULT_MIN_VALUE;
    private int interval = DEFAULT_INTERVAL;
    private int currentValue = DEFAULT_CURRENT_VALUE;
    private final Plurals unit = new Plurals();
    private boolean dialogEnabled = DEFAULT_DIALOG_ENABLED;

    @StyleRes
    private int dialogStyle;

    private TextView valueView;
    private SeekBar seekBarView;
    private ViewGroup valueHolderView;
    private View bottomLineView;

    //view stuff
    private TextView titleView, summaryView;
    private String title;
    private final Plurals summary = new Plurals();
    private boolean isEnabled = DEFAULT_IS_ENABLED;

    private String dialogTitle = null;
    @DrawableRes
    private int dialogIconResId = 0;

    //controller stuff
    private final boolean isView;
    private final Context context;
    private ViewStateListener viewStateListener;
    private PersistValueListener persistValueListener;
    private ChangeValueListener changeValueListener;

    interface ViewStateListener {
        boolean isEnabled();

        void setEnabled(boolean enabled);
    }

    PreferenceControllerDelegate(@NonNull final Context context, final Boolean isView) {
        this.context = context;
        this.isView = isView;
    }

    void setPersistValueListener(@Nullable final PersistValueListener persistValueListener) {
        this.persistValueListener = persistValueListener;
    }

    void setViewStateListener(@Nullable final ViewStateListener viewStateListener) {
        this.viewStateListener = viewStateListener;
    }

    void setChangeValueListener(@Nullable final ChangeValueListener changeValueListener) {
        this.changeValueListener = changeValueListener;
    }

    void loadValuesFromXml(@Nullable final AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
            try {
                minValue = a.getInt(R.styleable.SeekBarPreference_msbp_minValue, DEFAULT_MIN_VALUE);
                maxValue = a.getInt(R.styleable.SeekBarPreference_msbp_maxValue, DEFAULT_MAX_VALUE);
                interval = a.getInt(R.styleable.SeekBarPreference_msbp_interval, DEFAULT_INTERVAL);

                dialogEnabled = a.getBoolean(R.styleable.SeekBarPreference_msbp_dialogEnabled, DEFAULT_DIALOG_ENABLED);

                // plurals support for units
                // TODO: case when the string is not a reference
                unit.set(a.getResourceId(R.styleable.SeekBarPreference_msbp_measurementUnit, 0));

                dialogStyle = a.getResourceId(R.styleable.SeekBarPreference_msbp_dialogStyle, 0);
                dialogTitle = a.getString(R.styleable.SeekBarPreference_msbp_dialogTitle);
                dialogIconResId = a.getResourceId(R.styleable.SeekBarPreference_msbp_dialogIcon, 0);

                if (isView) {
                    title = a.getString(R.styleable.SeekBarPreference_msbp_view_title);

                    currentValue = a.getInt(R.styleable.SeekBarPreference_msbp_view_defaultValue, DEFAULT_CURRENT_VALUE);

                    isEnabled = a.getBoolean(R.styleable.SeekBarPreference_msbp_view_enabled, DEFAULT_IS_ENABLED);

                    // following lines are dealing with plurals resource for summary
                    // plurals resource may be specified in "msbp_view_summary"
                    // or "android:summary" (takes precedence)

                    // try "android:summary" for reference first
                    // TODO: case when the string is not a reference
                    summary.set(attrs.getAttributeResourceValue(NS_ANDROID, "summary",
                            attrs.getAttributeResourceValue(R.styleable.SeekBarPreference_msbp_view_summary, 0)));
                }
            } finally {
                a.recycle();
            }
        }
    }

    void onBind(@NonNull final View view) {
        view.setClickable(false);

        seekBarView = view.findViewById(R.id.seekbar);
        valueView = view.findViewById(R.id.seekbar_value);

        setMaxValue(maxValue);
        seekBarView.setOnSeekBarChangeListener(this);

        setCurrentValue(currentValue);

        if (isView) {
            titleView = view.findViewById(android.R.id.title);
            summaryView = view.findViewById(android.R.id.summary);

            titleView.setText(title);
            summaryView.setText(summary.apply(context, currentValue));
        }

        bottomLineView = view.findViewById(R.id.bottom_line);
        valueHolderView = view.findViewById(R.id.value_holder);

        setDialogEnabled(dialogEnabled);
        setEnabled(isEnabled(), true);
    }

    private void bindCurrentValueToView() {
        if (valueView != null) {
            String s = unit.apply(context, currentValue);
            if (!unit.isFormatted()) s = TextUtils.isEmpty(s)
                    ? Integer.toString(currentValue)
                    : currentValue + " " + s;
            valueView.setText(s);
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress,
                                  final boolean fromUser) {
        if (fromUser) {
            final int newValue = progressToValue(progress);
            setCurrentValue(newValue);
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        setCurrentValue(currentValue);
    }

    @Override
    public void onClick(final View v) {
        // TODO: separate atomic atomic interval
        new CustomValueDialog(context, dialogStyle,
                dialogTitle == null ?
                        context.getString(R.string.title_dialog, title, unit.get(context, 1))
                        : dialogTitle, dialogIconResId,
                minValue, maxValue, 1, currentValue)
                .setPersistValueListener(new PersistValueListener() {
                    @Override
                    public boolean persistInt(final int value) {
                        setCurrentValue(value);
                        return true;
                    }
                })
                .show();
    }

    String getTitle() {
        return title;
    }

    void setTitle(final String title) {
        this.title = title;
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    String getSummary() {
        return summary.get(context, 1);
    }

    void setSummary(final String summary) {
        this.summary.set(summary);
        if (seekBarView != null) {
            summaryView.setText(this.summary.apply(context, currentValue));
        }
    }

    boolean isEnabled() {
        if (!isView && viewStateListener != null) {
            return viewStateListener.isEnabled();
        } else return isEnabled;
    }

    void setEnabled(final boolean enabled, final boolean viewsOnly) {
        if (DEBUG) Log.d(TAG, "setEnabled = " + enabled);
        isEnabled = enabled;

        if (viewStateListener != null && !viewsOnly) {
            viewStateListener.setEnabled(enabled);
        }

        if (seekBarView != null) { //theoretically might not always work
            if (DEBUG) Log.d(TAG, "view is disabled!");
            seekBarView.setEnabled(enabled);
            valueView.setEnabled(enabled);
            valueHolderView.setClickable(enabled);
            valueHolderView.setEnabled(enabled);

            bottomLineView.setEnabled(enabled);

            if (isView) {
                titleView.setEnabled(enabled);
                summaryView.setEnabled(enabled);
            }
        }

    }

    void setEnabled(final boolean enabled) {
        setEnabled(enabled, false);
    }

    int getMaxValue() {
        return maxValue;
    }

    void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;

        if (seekBarView != null) {
            seekBarView.setMax(valueToProgress(maxValue));
            seekBarView.setProgress(valueToProgress(currentValue));
        }
    }

    private int valueToProgress(int value) {
        if (value >= maxValue) value = maxValue;
        else if (value <= minValue) return 0;
        return (value - minValue + interval / 2) / interval; // round to nearest
    }

    private int progressToValue(final int progress) {
        if (progress <= 0) return minValue;
        final int r = progress * interval + minValue;
        return r > maxValue ? maxValue : r;
    }

    int getMinValue() {
        return minValue;
    }

    void setMinValue(final int minValue) {
        this.minValue = minValue;
        setMaxValue(maxValue);
    }

    int getInterval() {
        return interval;
    }

    void setInterval(final int interval) {
        this.interval = interval;
    }

    int getCurrentValue() {
        return currentValue;
    }

    void setCurrentValue(int value) { // TODO: refactor
        if (value < minValue) value = minValue;
        if (value > maxValue) value = maxValue;

        if (changeValueListener != null) {
            if (!changeValueListener.onChange(value)) {
                return;
            }
        }
        currentValue = value;
        if (seekBarView != null)
            seekBarView.setProgress(valueToProgress(currentValue));

        if (persistValueListener != null) {
            persistValueListener.persistInt(value);
        }
        bindCurrentValueToView();
    }

    boolean isUsingPluralsForUnits() {
        return unit.isPlurals(context);
    }

    @Nullable
    String getUnit() {
        return unit.get(context, 1);
    }

    void setUnit(@Nullable final String str) {
        this.unit.set(str);
    }

    void setUnit(@AnyRes final int resId) {
        this.unit.set(resId);
    }

    boolean isDialogEnabled() {
        return dialogEnabled;
    }

    void setDialogEnabled(final boolean dialogEnabled) {
        this.dialogEnabled = dialogEnabled;

        if (valueHolderView != null && bottomLineView != null) {
            valueHolderView.setOnClickListener(dialogEnabled ? this : null);
//            valueHolderView.setClickable(dialogEnabled);
            bottomLineView.setVisibility(dialogEnabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    void setDialogStyle(@StyleRes final int dialogStyle) {
        this.dialogStyle = dialogStyle;
    }
}
