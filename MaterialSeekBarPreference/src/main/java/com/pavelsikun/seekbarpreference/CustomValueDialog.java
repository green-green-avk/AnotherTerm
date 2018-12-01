package com.pavelsikun.seekbarpreference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Created by Pavel Sikun on 21.05.16.
 */
final class CustomValueDialog {

    private final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_OK_RES_ID = android.R.string.ok;
    private static final int DEFAULT_CANCEL_RES_ID = android.R.string.cancel;

    private Dialog dialog;
    private EditText customValueView;

    private int minValue, maxValue, interval, currentValue;
    private PersistValueListener persistValueListener;

    private Drawable icon;
    private String titleText;
    private String okText;
    private String cancelText;

    private final PopupWindow warnWin;

    CustomValueDialog(@NonNull final Context context,
                      final int style, final int minValue, final int maxValue, final int interval,
                      final int currentValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.interval = interval;
        this.currentValue = currentValue;

        icon = null;
        titleText = null;
        okText = context.getString(DEFAULT_OK_RES_ID);
        cancelText = context.getString(DEFAULT_CANCEL_RES_ID);

        if (style != 0) {
            final TypedArray a = context.obtainStyledAttributes(style, R.styleable.SeekBarPreference);
            try {
                if (a.hasValue(R.styleable.SeekBarPreference_msbp_dialogIcon))
                    icon = a.getDrawable(R.styleable.SeekBarPreference_msbp_dialogIcon);

                if (a.hasValue(R.styleable.SeekBarPreference_msbp_dialogTitle))
                    titleText = a.getString(R.styleable.SeekBarPreference_msbp_dialogTitle);

                if (a.hasValue(R.styleable.SeekBarPreference_msbp_dialogOk))
                    okText = a.getString(R.styleable.SeekBarPreference_msbp_dialogOk);

                if (a.hasValue(R.styleable.SeekBarPreference_msbp_dialogCancel))
                    cancelText = a.getString(R.styleable.SeekBarPreference_msbp_dialogCancel);

            } finally {
                if (a != null) a.recycle();
            }

        }

        // TODO: Add theme attribute
        // I see no problem in merging style attributes into theme though...
        final Context dialogContext = new ContextThemeWrapper(context, style);

        final TextView warnWinView = new TextView(context);
        warnWinView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        warnWinView.setBackgroundResource(R.drawable.msbp_warning_popup);
        warnWin = new PopupWindow(warnWinView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        init(new AlertDialog.Builder(dialogContext));
    }

    private void init(@NonNull final AlertDialog.Builder dialogBuilder) {
        final View dialogView = LayoutInflater.from(dialogBuilder.getContext()).inflate(R.layout.value_selector_dialog, null);

        final TextView minValueView = dialogView.findViewById(R.id.minValue);
        final TextView maxValueView = dialogView.findViewById(R.id.maxValue);
        customValueView = dialogView.findViewById(R.id.customValue);

        minValueView.setText(String.valueOf(minValue));
        maxValueView.setText(String.valueOf(maxValue));
        customValueView.setHint(String.valueOf(currentValue));

        final ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        if (iconView != null) {
            iconView.setImageDrawable(icon);
            if (icon != null) {
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }

        final TextView titleView = dialogView.findViewById(R.id.dialog_title);
        if (titleView != null) {
            titleView.setText(titleText);
            if (!TextUtils.isEmpty(titleText)) {
                titleView.setVisibility(View.VISIBLE);
            } else {
                titleView.setVisibility(View.GONE);
            }
        }

        final View colorView = dialogView.findViewById(R.id.dialog_color_area);
        colorView.setBackgroundColor(fetchAccentColor(dialogBuilder.getContext()));

        final Button btnApply = dialogView.findViewById(R.id.btn_apply);
        final Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnApply.setText(okText);
        btnCancel.setText(cancelText);

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryApply();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                warnWin.dismiss();
            }
        });
/* TODO: on min API 17
        dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                warnWin.dismiss();
            }
        });
*/
        dialog = dialogBuilder.setView(dialogView).create();
    }

    private int fetchAccentColor(@NonNull final Context context) {
        final TypedArray a = context.obtainStyledAttributes(0, new int[]{R.attr.colorAccent});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    CustomValueDialog setPersistValueListener(final PersistValueListener listener) {
        persistValueListener = listener;
        return this;
    }

    void show() {
        dialog.show();
    }

    private void tryApply() {
        warnWin.dismiss();

        final int value;

        try {
            final String valueStr = customValueView.getText().toString();
            if (TextUtils.isEmpty(valueStr)) {
                customValueView.setText(String.valueOf(currentValue));
                return;
            }
            value = Integer.parseInt(valueStr);
            if (value > maxValue) {
                customValueView.setText(String.valueOf(maxValue));
                return;
            } else if (value < minValue) {
                customValueView.setText(String.valueOf(minValue));
                return;
            } else {
                int t = value - minValue;
                if (t % interval != 0) {
                    t = (t + interval / 2) / interval * interval + minValue; // round
                    customValueView.setText(String.valueOf(t));
                    return;
                }
            }
        } catch (final NumberFormatException e) {
            notifyWrongInput(dialog.getContext().getString(R.string.msg_invalid_number));
            return;
        }

        if (persistValueListener != null) {
            persistValueListener.persistInt(value);
            dialog.dismiss();
        }
    }

    private void notifyWrongInput(final String msg) {
        ((TextView) warnWin.getContentView()).setText(msg);
        warnWin.showAsDropDown(customValueView);
    }
}
