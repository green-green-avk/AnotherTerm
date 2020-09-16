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
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.R;

public final class RawPreferenceUiWrapper implements PreferenceUiWrapper {
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, List<?>> listsOpts = new HashMap<>();
    private final Set<String> changedFields = new HashSet<>();
    private boolean isFrozen = false;
    private int delayedInitNum = 0;
    private boolean delayedInitDone = true;

    private void setupViews(@NonNull final View root) {
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
                            callOnChanged(pName);
                        }
                    }

                    @Override
                    public void afterTextChanged(final Editable s) {
                    }
                });
            } else if (root instanceof AdapterView) {
                delayedInitNum++;
                ((AdapterView) root).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(final AdapterView<?> parent, final View view,
                                               final int position, final long id) {
                        if (!isFrozen) {
//                            changedFields.add(pName);
                            callOnChanged(pName);
                            completeDelayedInit();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        if (!isFrozen) {
//                            changedFields.add(pName);
                            callOnChanged(pName);
                            completeDelayedInit();
                        }
                    }
                });
                changedFields.add(pName); // TODO: Not now.
            } else if (root instanceof CompoundButton) {
                ((CompoundButton) root).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView,
                                                 final boolean isChecked) {
                        if (!isFrozen) {
//                            changedFields.add(pName);
                            callOnChanged(pName);
                        }
                    }
                });
                changedFields.add(pName); // TODO: Not now.
            } else {
                changedFields.add(pName);
            }
            if (chs.length > 1)
                listsOpts.put(pName, Arrays.asList(Arrays.copyOfRange(chs, 1, chs.length)));
        }
        if (root instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) root).getChildCount(); ++i) {
                final View v = ((ViewGroup) root).getChildAt(i);
                setupViews(v);
            }
        }
    }

    private void completeDelayedInit() {
        if (!delayedInitDone) {
            delayedInitNum--;
            if (delayedInitNum == 0) {
                delayedInitDone = true;
                callOnInitialized();
            }
        }
    }

    public void clear() {
        views.clear();
        delayedInitNum = 0;
    }

    public void addBranch(@NonNull final View root) {
        delayedInitDone = false;
        setupViews(root);
        if (delayedInitNum == 0) {
            delayedInitDone = true;
            callOnInitialized();
        }
    }

    private void freeze(final boolean v) {
        isFrozen = v;
    }

    public void setListValues(final String key, final List<?> values) {
        listsOpts.put(key, values);
    }

    private static int findInAdapter(@NonNull final Adapter a, final Object v) {
        for (int i = 0; i < a.getCount(); ++i) {
            if (a.getItem(i) == v) return i;
        }
        return -1;
    }

    private static final ResultException noEmptyValue =
            new ResultException();

    private long getLongEmptyValue(final String key) throws ResultException {
        final List<?> opts = listsOpts.get(key);
        if (opts == null || opts.size() < 1) throw noEmptyValue;
        final Object opt = opts.get(0);
        try {
            return Long.parseLong(opt.toString());
        } catch (final NumberFormatException e) {
            throw noEmptyValue;
        }
    }

    private static long getLongValue(final Object value) {
        if (value == null) throw new NumberFormatException("The value is `null'");
        if (value instanceof Integer) return (int) value;
        if (value instanceof Long) return (long) value;
        return Long.parseLong(value.toString());
    }

    @Override
    public Object get(final String key) {
        if (!views.containsKey(key)) return null;
        final View view = views.get(key);
        if (view instanceof EditText) {
            final String t = ((EditText) view).getText().toString();
            final int it = ((EditText) view).getInputType();
            if ((it & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER) {
                if ((it & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
                    try {
                        return Double.parseDouble(t);
                    } catch (final NumberFormatException e) {
                        return new ParseException(view.getContext().getString(R.string.number_expected), view, key, t);
                    }
                } else {
                    try {
                        if (t.isEmpty()) {
                            try {
                                return getLongEmptyValue(key);
                            } catch (final ResultException ignored) {
                            }
                        }
                        return Long.parseLong(t);
                    } catch (final NumberFormatException e) {
                        return new ParseException(view.getContext().getString(R.string.number_expected), view, key, t);
                    }
                }
            } else {
                return t;
            }
        } else if (view instanceof AdapterView) {
            final List<?> values = listsOpts.get(key);
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
            final List<?> values = listsOpts.get(key);
            if (values == null) {
                if (value instanceof Integer && (int) value >= 0)
                    ((AdapterView) view).setSelection((int) value);
                else ((AdapterView) view).setSelection(0);
            } else ((AdapterView) view).setSelection(values.indexOf(value));
            return;
        }
        if (view instanceof CompoundButton) {
            try {
                ((CompoundButton) view).setChecked(BooleanCaster.CAST(value));
            } catch (final ClassCastException e) {
                Log.e("Preference UI", "Type cast", e);
            }
            return;
        }
        if (view instanceof TextView) {
            String v = null;
            if (view instanceof EditText) {
                final int it = ((EditText) view).getInputType();
                if ((it & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER) {
                    if ((it & InputType.TYPE_NUMBER_FLAG_DECIMAL) == 0) {
                        try {
                            if (getLongValue(value) == getLongEmptyValue(key)) v = "";
                        } catch (final NumberFormatException | ResultException ignored) {
                        }
                    }
                }
            }
            if (v == null)
                v = value.toString();
            ((TextView) view).setText(v);
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
    public void setDefaultPreferences(@NonNull final Map<String, ?> pp) {
        freeze(true);
        try {
            setPreferences(pp);
        } finally {
            freeze(false);
        }
    }

    @Override
    @NonNull
    public Set<String> getChangedFields() {
        return changedFields;
    }

    private Callbacks callbacks = null;

    private void callOnInitialized() {
        if (callbacks != null) callbacks.onInitialized();
    }

    private void callOnChanged(final String key) {
        if (callbacks != null) callbacks.onChanged(key);
    }

    @Override
    public void setCallbacks(@Nullable final Callbacks callbacks) {
        this.callbacks = callbacks;
    }
}
