package green_green_avk.anotherterm.utils;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetaPreferenceUiWrapper implements PreferenceUiWrapper {

    public static final class Meta {
        public static final class Field {
            public int id;
            public Caster type;
            public List<ListItem> listValues;

            public Field(final int id, final Caster type, final List<ListItem> listValues) {
                this.id = id;
                this.type = type;
                this.listValues = listValues;
            }
        }

        public Map<String, Meta.Field> fields = new HashMap<>();
    }

    private final Meta meta;
    private View root;
    private final BooleanCaster booleanCaster = new BooleanCaster();

    public MetaPreferenceUiWrapper(final Meta meta) {
        this.meta = meta;
        for (final Meta.Field field : this.meta.fields.values()) {
            final View view = root.findViewById(field.id);
            if (view instanceof AdapterView && field.listValues != null)
                ((AdapterView) view).setAdapter(new ArrayAdapter<>(
                        root.getContext(),
                        android.R.layout.simple_list_item_1,
                        field.listValues
                ));
        }
    }

    public void setRoot(final View root) {
        this.root = root;
    }

    private View getView(final String key) {
        if (!meta.fields.containsKey(key)) return null;
        return root.findViewById(meta.fields.get(key).id);
    }

    public Object get(final String key) {
        final View view = getView(key);
        if (view == null) return null;
        final Meta.Field field = meta.fields.get(key);
        if (view instanceof EditText) {
            final String t = ((EditText) view).getText().toString();
            return field.type.cast(t);
        }
        if (view instanceof AdapterView) {
            return field.listValues.get(((AdapterView) view).getSelectedItemPosition()).key;
        }
        if (view instanceof CompoundButton) {
            return field.type.cast(((CompoundButton) view).isChecked());
        }
        return null;
    }

    public void set(final String key, final Object value) {
        final View view = getView(key);
        if (view == null) return;
        final Meta.Field field = meta.fields.get(key);
        if (view instanceof AdapterView) {
            ((AdapterView) view).setSelection(field.listValues.indexOf(value));
            return;
        }
        if (view instanceof CompoundButton) {
            try {
                ((CompoundButton) view).setChecked((boolean) booleanCaster.cast(value));
            } catch (ClassCastException e) {
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
        for (final String k : meta.fields.keySet()) {
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
