package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.R;

public class FilePickerView extends LinearLayoutCompat implements ParameterView<Uri> {
    public String title = "Pick a file";
    public String mimeType = "*/*";
    private Uri uri = null;

    public FilePickerView(@NonNull final Context context) {
        super(context);
    }

    public FilePickerView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public FilePickerView(@NonNull final Context context, @Nullable final AttributeSet attrs,
                          final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private final AppCompatTextView wText;
    private final AppCompatImageButton wSet;
    private final AppCompatImageButton wUnset;

    private final RequesterCompatDelegate.ActivityResultCallback onFilePicked =
            (resultCode, data) -> {
                if (data == null)
                    return;
                final Uri uri = data.getData();
                if (uri == null)
                    return;
                this.uri = uri;
                refresh();
                if (android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.KITKAT) {
                    try {
                        getContext().getContentResolver()
                                .takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (final Exception e) {
                        if (BuildConfig.DEBUG)
                            Log.w(this.getClass().getSimpleName(), e);
                    }
                }
                doOnValueChanged();
            };

    {
        setOrientation(HORIZONTAL);
        wText = (AppCompatTextView) LayoutInflater.from(getContext())
                .inflate(R.layout.parameter_text, this, false);
        ((LayoutParams) wText.getLayoutParams()).weight = 1f;
        wSet = (AppCompatImageButton) LayoutInflater.from(getContext())
                .inflate(R.layout.parameter_button, this, false);
        wSet.setImageResource(R.drawable.ic_edit);
        wSet.setContentDescription(getResources().getString(R.string.action_set));
        final ExtAppCompatActivity activity = ExtAppCompatActivity.getByContext(getContext());
        activity.activityRequester.checkOnResume(getTag(), onFilePicked);
        wSet.setOnClickListener(view -> {
            final Intent i = new Intent(android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.KITKAT ?
                    Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType(mimeType);
            activity.activityRequester.launch(getTag(),
                    Intent.createChooser(i, title), onFilePicked);
        });
        wUnset = (AppCompatImageButton) LayoutInflater.from(getContext())
                .inflate(R.layout.parameter_button, this, false);
        wUnset.setImageResource(R.drawable.ic_close);
        wUnset.setContentDescription(getResources().getString(R.string.action_unset));
        wUnset.setOnClickListener(view -> {
            final Uri uri = this.uri;
            if (uri == null)
                return;
            this.uri = null;
            refresh();
            doOnValueChanged();
        });
        addView(wUnset);
        addView(wText);
        addView(wSet);
        ViewCompat.setBackgroundTintMode(wText, PorterDuff.Mode.OVERLAY);
        refresh();
    }

    private static final ColorStateList unsetBgTint = ColorStateList.valueOf(0x10FFFF00);
    private static final ColorStateList okBgTint = ColorStateList.valueOf(0x1000FF00);
    private static final ColorStateList errorBgTint = ColorStateList.valueOf(0x20FF0000);

    private final Drawable lockIcon = getWithTint(R.drawable.ic_lock, Color.RED);
    private final Drawable notFoundIcon = getWithTint(R.drawable.ic_folder_off, Color.RED);
    private final Drawable unknownIcon = getWithTint(R.drawable.ic_menu_help, Color.RED);

    @Nullable
    private Drawable getWithTint(@DrawableRes final int res, @ColorInt final int tint) {
        final Drawable r = AppCompatResources.getDrawable(getContext(), res);
        if (r == null)
            return null;
        DrawableCompat.setTint(r.mutate(), tint);
        return r;
    }

    @NonNull
    private CharSequence prefixWith(@Nullable final Drawable d, @NonNull final CharSequence v) {
        if (d == null)
            return v;
        final Editable builder = new SpannableStringBuilder();
        builder.append('X').setSpan(new InlineImageSpan(d),
                builder.length() - 1,
                builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(v);
        return builder;
    }

    private void refresh() {
        if (uri != null) {
            wUnset.setVisibility(VISIBLE);
            final Drawable icon;
            switch (check(uri)) {
                case OK:
                    ViewCompat.setBackgroundTintList(wText, okBgTint);
                    icon = null;
                    break;
                case ACCESS_DENIED:
                    ViewCompat.setBackgroundTintList(wText, errorBgTint);
                    icon = lockIcon;
                    break;
                case NOT_FOUND:
                    ViewCompat.setBackgroundTintList(wText, errorBgTint);
                    icon = notFoundIcon;
                    break;
                default:
                    ViewCompat.setBackgroundTintList(wText, errorBgTint);
                    icon = unknownIcon;
            }
            wText.setText(prefixWith(icon, uri.toString()));
        } else {
            ViewCompat.setBackgroundTintList(wText, unsetBgTint);
            wUnset.setVisibility(GONE);
            wText.setText(R.string.value_not_set);
        }
    }

    private enum Result {OK, ACCESS_DENIED, NOT_FOUND, UNKNOWN}

    @NonNull
    private Result check(@NonNull final Uri uri) {
        try {
            final int n;
            final Cursor c = getContext().getContentResolver().query(uri,
                    new String[]{OpenableColumns.SIZE},
                    null, null, null);
            if (c != null) {
                n = c.getCount();
                c.close();
            } else
                n = 0;
            return n > 0 ? Result.OK : Result.NOT_FOUND;
        } catch (final SecurityException e) {
            return Result.ACCESS_DENIED;
        } catch (final Exception e) {
            return Result.UNKNOWN;
        }
    }

    @Override
    @Nullable
    public Uri getValue() {
        return uri;
    }

    @Override
    public void setValue(@Nullable final Uri v) {
        uri = v;
        refresh();
    }

    @Override
    public void setValueFrom(@Nullable final Object v) {
        if (v instanceof Uri)
            setValue((Uri) v);
        else if (v instanceof String) {
            setValue(Uri.parse((String) v));
        } else
            throw new IllegalArgumentException();
    }

    private OnValueChanged<Uri> onValueChanged = null;

    private void doOnValueChanged() {
        final OnValueChanged<Uri> h = onValueChanged;
        if (h != null)
            h.onValueChanged(uri);
    }

    @Override
    public void setOnValueChanged(@Nullable final OnValueChanged<Uri> v) {
        onValueChanged = v;
    }
}
