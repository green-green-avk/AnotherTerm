package green_green_avk.anotherterm.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class ViewValueListener {
    protected abstract void onChanged(@NonNull View view, @Nullable Object value);

    public void adoptView(@NonNull final View view) {
        if (view instanceof EditText) {
            ((EditText) view).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s, final int start,
                                              final int count, final int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, final int start,
                                          final int before, final int count) {
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    onChanged(view, s);
                }
            });
        } else if (view instanceof CompoundButton) {
            ((CompoundButton) view).setOnCheckedChangeListener((buttonView, isChecked) ->
                    onChanged(view, isChecked));
        } else if (view instanceof AdapterView) {
            ((AdapterView<?>) view).setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(final AdapterView<?> parent, final View view,
                                                   final int position, final long id) {
                            onChanged(view, position);
                        }

                        @Override
                        public void onNothingSelected(final AdapterView<?> parent) {
                            onChanged(view, null);
                        }
                    });
        }
    }
}
