package name.green_green_avk.compatcolorpicker;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorPickerTextView extends FrameLayout
        implements ColorPickerControl {
    @Nullable
    private AlertDialog dialog = null;
    private final ColorDrawable colorDrawable = new ColorDrawable();

    @Nullable
    private ColorPickerView.OnValueChanged onValueChanged = null;
    boolean hasAlpha = true;

    public ColorPickerTextView(final Context context) {
        super(context);
    }

    public ColorPickerTextView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerTextView(final Context context, @Nullable final AttributeSet attrs,
                               final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ColorPickerTextView(final Context context, @Nullable final AttributeSet attrs,
                               final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @ColorInt
    private final int textColor;
    private final boolean isDarkTheme;

    private final TextView wText;
    private final ImageButton wCopy;
    private final ImageButton wPaste;

    {
        setBackgroundDrawable(colorDrawable);
        inflate(getContext(), R.layout.ccp_color_picker_text_view, this);
        wText = findViewById(R.id.text);
        wCopy = findViewById(R.id.copy);
        wPaste = findViewById(R.id.paste);
        textColor = wText.getCurrentTextColor();
        isDarkTheme = isLightColor(textColor);
        wCopy.setOnClickListener(view ->
                Utils.toClipboard(getContext(), "#" + stringify(getValue())));
        wPaste.setOnClickListener(view -> {
            final String v = Utils.stringFromClipBoard(getContext());
            if (v == null)
                return;
            try {
                setValue(parse(v));
                notifyValue();
            } catch (final IllegalArgumentException ignored) {
            }
        });
        wText.setOnClickListener(view -> {
            final class DialogState {
                private int color;
            }
            final DialogState dialogState = new DialogState();
            final View dialogRoot = inflate(getContext(),
                    R.layout.ccp_color_picker_text_dialog, null);
            final EditText wDialogText = dialogRoot.findViewById(R.id.text);
            dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogRoot)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        setValue(dialogState.color);
                        notifyValue();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    })
                    .setCancelable(true)
                    .show();
            final Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            wDialogText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s,
                                              final int start, final int count, final int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s,
                                          final int start, final int before, final int count) {
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    try {
                        dialogState.color = parse(s.toString());
                    } catch (final IllegalArgumentException e) {
                        ok.setEnabled(false);
                        return;
                    }
                    ok.setEnabled(true);
                }
            });
            wDialogText.setText(stringify(getValue()));
        });
        setValue(Color.WHITE);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    @ColorInt
    public int getValue() {
        return colorDrawable.getColor();
    }

    @Override
    public void setOnValueChanged(@Nullable final ColorPickerView.OnValueChanged v) {
        onValueChanged = v;
    }

    @Override
    public void setValue(@ColorInt final int v) {
        colorDrawable.setColor(v);
        wText.setText("#" + stringify(v));
        wText.setTextColor(
                isLightColor(v, isDarkTheme ? Color.BLACK : Color.WHITE) == isDarkTheme ?
                        invertColor(textColor) : textColor);
        wCopy.setColorFilter(wText.getCurrentTextColor());
        wPaste.setColorFilter(wText.getCurrentTextColor());
    }

    private void notifyValue() {
        if (onValueChanged != null)
            onValueChanged.onValueChanged(getValue());
    }

    @NonNull
    private String stringify(@ColorInt final int v) {
        return String.format(Locale.ROOT,
                hasAlpha ? "%08X" : "%06X",
                hasAlpha ? v : v & 0xFFFFFF);
    }

    private static final Pattern reColorAlpha =
            Pattern.compile("^#?([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");
    private static final Pattern reColor =
            Pattern.compile("^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    @ColorInt
    private int parse(@NonNull final String v) {
        final Matcher m = (hasAlpha ? reColorAlpha : reColor).matcher(v);
        if (!m.matches())
            throw new IllegalArgumentException("Not a color");
        final String s = m.group(1);
        assert s != null;
        int n = Integer.parseUnsignedInt(s, 16);
        switch (s.length()) {
            case 3:
                n = ((n & 0xF) | (n & 0xF) << 4)
                        | ((n & 0xF0) << 4 | (n & 0xF0) << 8)
                        | ((n & 0xF00) << 8 | (n & 0xF00) << 12)
                        | 0xFF000000;
                break;
            case 4:
                n = ((n & 0xF) | (n & 0xF) << 4)
                        | ((n & 0xF0) << 4 | (n & 0xF0) << 8)
                        | ((n & 0xF00) << 8 | (n & 0xF00) << 12)
                        | ((n & 0xF000) << 12 | (n & 0xF000) << 16);
                break;
            case 6:
                n |= 0xFF000000;
                break;
        }
        return n;
    }

    @ColorInt
    private static int invertColor(@ColorInt final int v) {
        return Color.argb(Color.alpha(v),
                255 - Color.red(v),
                255 - Color.green(v),
                255 - Color.blue(v));
    }

    private static boolean isLightColor(@ColorInt final int v) {
        return (Color.red(v) >> 1)
                + Color.green(v)
                + (Color.blue(v) >> 1) > 0xFF;
    }

    private static boolean isLightColor(@ColorInt final int v, @ColorInt final int bg) {
        final int a = Color.alpha(v);
        return ((Color.red(v) * a + Color.red(bg) * (255 - a)) >> 1)
                + (Color.green(v) * a + Color.green(bg) * (255 - a))
                + ((Color.blue(v) * a + Color.blue(bg) * (255 - a)) >> 1) > 0xFFFF;
    }
}
