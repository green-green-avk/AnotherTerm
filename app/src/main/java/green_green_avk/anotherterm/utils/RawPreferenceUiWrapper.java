package green_green_avk.anotherterm.utils;

import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.R;

public final class RawPreferenceUiWrapper implements PreferenceUiWrapper {
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, List<?>> listsValues = new HashMap<>();
    private final Set<String> changedFields = new HashSet<>();
    private boolean isFrozen = false;

    private void searchForTags(@NonNull final View root) {
        final Object tag = root.getTag();
        if (tag instanceof String) {
            final String[] chs = ((String) tag).split("/");
            final String pName = chs[0].intern();
            views.put(pName, root);
            if (root instanceof EditText) {
                final ColorStateList color = ((EditText) root).getTextColors();
                ((EditText) root).setTextColor(color.withAlpha(0xA0)); // TODO: UI mess
                ((EditText) root).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(final CharSequence s, final int start,
                                                  final int count, final int after) {

                    }

                    @Override
                    public void onTextChanged(final CharSequence s, final int start,
                                              final int before, final int count) {
                        if (!isFrozen) {
                            changedFields.add(pName);
                            ((EditText) root).setTextColor(color);
                        }
                    }

                    @Override
                    public void afterTextChanged(final Editable s) {

                    }
                });
// TODO: Not now.
/*
            } else if (root instanceof AdapterView) {
                final Drawable bg = root.getBackground();
                ((AdapterView) root).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        changedFields.add(pName);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        changedFields.add(pName);
                        setBg(root, bg);
                    }
                });
            } else if (root instanceof CompoundButton) {
                final Drawable bg = root.getBackground();
                ((CompoundButton) root).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        changedFields.add(pName);
                    }
                });
*/
            } else {
                changedFields.add(pName);
            }
            if (chs.length > 1)
                listsValues.put(pName, Arrays.asList(Arrays.copyOfRange(chs, 1, chs.length)));
        }
        if (root instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) root).getChildCount(); ++i) {
                final View v = ((ViewGroup) root).getChildAt(i);
                searchForTags(v);
            }
        }
    }

    public void clear() {
        views.clear();
    }

    public void addBranch(@NonNull final View root) {
        searchForTags(root);
    }

    public void freeze(final boolean v) {
        isFrozen = v;
    }

    public void setListValues(final String key, final List<?> values) {
        listsValues.put(key, values);
    }

    private static int findInAdapter(final Adapter a, final Object v) {
        for (int i = 0; i < a.getCount(); ++i) {
            if (a.getItem(i) == v) return i;
        }
        return -1;
    }

    @Override
    public Object get(final String key) {
        if (!views.containsKey(key)) return null;
        final View view = views.get(key);
        if (view instanceof EditText) {
            final String t = ((EditText) view).getText().toString();
            final int it = ((EditText) view).getInputType();
            if ((it & InputType.TYPE_CLASS_NUMBER) != 0 && (it & InputType.TYPE_CLASS_TEXT) == 0) {
                if ((it & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
                    try {
                        return Double.parseDouble(t);
                    } catch (final NumberFormatException e) {
                        return new ParseException(view.getContext().getString(R.string.number_expected), view, key, t);
                    }
                } else {
                    try {
                        return Long.parseLong(t);
                    } catch (final NumberFormatException e) {
                        return new ParseException(view.getContext().getString(R.string.number_expected), view, key, t);
                    }
                }
            } else {
                return t;
            }
        } else if (view instanceof AdapterView) {
            final List<?> values = listsValues.get(key);
            if (values == null) return ((AdapterView) view).getSelectedItemPosition();
            return values.get(((AdapterView) view).getSelectedItemPosition());
        } else if (view instanceof CompoundButton) {
            return ((CompoundButton) view).isChecked();
        }
        return null;
    }

    @Override
    public void set(final String key, final Object value) {
        if (!views.containsKey(key)) return;
        final View view = views.get(key);
        if (view instanceof AdapterView) {
            final List<?> values = listsValues.get(key);
            if (values == null) {
                if (value instanceof Integer && (int) value >= 0)
                    ((AdapterView) view).setSelection((int) value);
                else ((AdapterView) view).setSelection(0);
            } else ((AdapterView) view).setSelection(values.indexOf(value));
            return;
        }
        if (view instanceof CompoundButton) {
            try {
                ((CompoundButton) view).setChecked((boolean) BooleanCaster.CAST(value));
            } catch (final ClassCastException e) {
                Log.e("Preference UI", "Type cast", e);
            }
            return;
        }
        if (view instanceof TextView) {
            ((TextView) view).setText(value.toString());
            return;
        }
    }

    @Override
    @NonNull
    public Map<String, Object> getPreferences() {
        final Map<String, Object> r = new HashMap<>();
        for (final String k : views.keySet()) {
            r.put(k, get(k));
        }
        return r;
    }

    @Override
    public void setPreferences(@NonNull final Map<String, ?> pp) {
        for (final Map.Entry<String, ?> ent : pp.entrySet()) {
            set(ent.getKey(), ent.getValue());
        }
    }

    @Override
    @NonNull
    public Set<String> getChangedFields() {
        return changedFields;
    }
}
