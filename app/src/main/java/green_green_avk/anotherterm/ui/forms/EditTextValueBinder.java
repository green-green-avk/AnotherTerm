package green_green_avk.anotherterm.ui.forms;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import green_green_avk.anotherterm.R;

public abstract class EditTextValueBinder<RESULT>
        extends ViewValueBinder<RESULT, Editable> {
    @NonNull
    protected final EditText view;
    protected PopupWindow popup = null;
    protected String warn = null;

    protected void check() {
        check(view.getText());
    }

    private void check(@Nullable final Editable s) {
        warn = onCheck(s);
        updateUi();
    }

    @SuppressLint("InflateParams")
    private void updateUi() {
        if (ViewCompat.isAttachedToWindow(view) && warn != null) {
            if (popup == null) {
                final TextView wHint = (TextView) LayoutInflater.from(view.getContext())
                        .inflate(R.layout.value_hint_popup, null);
                wHint.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                popup = new PopupWindow(wHint,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setAnimationStyle(android.R.style.Animation_Dialog);
            }
            ((TextView) popup.getContentView()).setText(warn);
            popup.showAsDropDown(view, 0, 0);
        } else {
            if (popup != null)
                popup.dismiss();
        }
        onUpdateUi();
    }

    protected void onUpdateUi() {
    }

    public EditTextValueBinder(@NonNull final EditText v) {
        view = v;
        view.addTextChangedListener(new TextWatcher() {
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
                check(s);
            }
        });
        final ViewTreeObserver vto = view.getViewTreeObserver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            vto.addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                @Override
                public void onWindowAttached() {
                    new Handler(Looper.getMainLooper()).post(EditTextValueBinder.this::updateUi);
                }

                @Override
                public void onWindowDetached() {
                    updateUi();
                }
            });
        else
            vto.addOnGlobalLayoutListener(this::updateUi); // Workaround
    }

    @Override
    public int getId() {
        return view.getId();
    }

    @Override
    protected void finalize() throws Throwable {
        popup.dismiss();
        super.finalize();
    }
}
