package green_green_avk.anotherterm.utils;

import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import green_green_avk.anotherterm.R;

public final class RawPreferenceUiWrapper implements PreferenceUiWrapper {
    public final Map<String, View> views = new HashMap<>();
    public final Map<String, List<?>> listsValues = new HashMap<>();

    private void searchForTags(final View root) {
        final Object tag = root.getTag();
        if (tag instanceof String) {
            final String[] chs = ((String) tag).split("/");
            final String pName = chs[0].intern();
            views.put(pName, root);
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

    public void addBranch(final View root) {
        searchForTags(root);
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

    public Map<String, Object> getPreferences() {
        final Map<String, Object> r = new HashMap<>();
        for (final String k : views.keySet()) {
            r.put(k, get(k));
        }
        return r;
    }

    public void setPreferences(final Map<String, ?> pp) {
        for (final Map.Entry<String, ?> ent : pp.entrySet()) {
            set(ent.getKey(), ent.getValue());
        }
    }
}
