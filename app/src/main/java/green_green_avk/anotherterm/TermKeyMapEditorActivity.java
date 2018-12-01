package green_green_avk.anotherterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.utils.Escape;
import green_green_avk.anotherterm.utils.Unescape;

public final class TermKeyMapEditorActivity extends AppCompatActivity {
    private static final class KeysAdapter extends BaseAdapter {
        private static final int[] keys;

        static {
            keys = new int[TermKeyMapRulesDefault.getSupportedKeys().size()];
            int i = 0;
            for (final int k : TermKeyMapRulesDefault.getSupportedKeys()) keys[i++] = k;
            Arrays.sort(keys);
        }

        @Override
        public int getCount() {
            return TermKeyMapRulesDefault.getSupportedKeys().size();
        }

        @Override
        public Object getItem(int position) {
            return keys[position];
        }

        @Override
        public long getItemId(int position) {
            return keys[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!(convertView instanceof TextView))
                convertView = new TextView(parent.getContext(), null,
                        android.R.style.Widget_TextView_SpinnerItem);
            ((TextView) convertView).setText(TermKeyMap.keyCodeToString(keys[position]));
            return convertView;
        }
    }

    private TermKeyMapRules.Editable keyMap = null;
    private int currentKeyCode = -1;

    @Nullable
    private static String nullUnescape(@NonNull String v) {
        if (v.isEmpty()) return null;
        try {
            return Unescape.c(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void prepareKeyField(final View v, final int m, final int am) {
        ((EditText) v).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (currentKeyCode >= 0)
                    keyMap.set(currentKeyCode, m, am, nullUnescape(s.toString()));
            }
        });
        /*
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && currentKeyCode >= 0)
                    keyMap.set(currentKeyCode, m, am, ((EditText) v).getText().toString());
            }
        });
        */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("E_KEYMAP", keyMap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term_key_map_editor);
        final String name = getIntent().getStringExtra(C.IFK_MSG_NAME);
        ((EditText) findViewById(R.id.f_name)).setText(name);
        if (savedInstanceState != null) {
            keyMap = (TermKeyMapRules.Editable) savedInstanceState.get("E_KEYMAP");
        } else {
            keyMap = (TermKeyMapRules.Editable) TermKeyMapManager.getRules(name);
        }
        final Spinner keyView = findViewById(R.id.f_key);
        keyView.setAdapter(new KeysAdapter());

        final ViewGroup keysView = findViewById(R.id.keys);
        keysView.setSaveFromParentEnabled(false);
        for (int m = 0; m < TermKeyMap.MODIFIERS_SIZE; ++m) {
            final View v = LayoutInflater.from(this)
                    .inflate(R.layout.term_key_map_entry, keysView, false);
            ((CheckBox) v.findViewById(R.id.shift)).setChecked((m & 1) != 0);
            ((CheckBox) v.findViewById(R.id.alt)).setChecked((m & 2) != 0);
            ((CheckBox) v.findViewById(R.id.ctrl)).setChecked((m & 4) != 0);
            prepareKeyField(v.findViewById(R.id.normal), m, TermKeyMap.APP_MODE_NONE);
            prepareKeyField(v.findViewById(R.id.app), m, TermKeyMap.APP_MODE_DEFAULT);
            keysView.addView(v);
        }

        keyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentKeyCode = (int) id;
                for (int m = 0; m < TermKeyMap.MODIFIERS_SIZE; ++m) {
                    final View v = keysView.getChildAt(m);
                    final EditText nView = v.findViewById(R.id.normal);
                    final EditText aView = v.findViewById(R.id.app);
                    nView.setHint(Escape.c(TermKeyMapManager.defaultKeyMap.get(currentKeyCode, m, TermKeyMap.APP_MODE_NONE)));
                    aView.setHint(Escape.c(TermKeyMapManager.defaultKeyMap.get(currentKeyCode, m, TermKeyMap.APP_MODE_DEFAULT)));
                    nView.setText(Escape.c(keyMap.get(currentKeyCode, m, TermKeyMap.APP_MODE_NONE)));
                    aView.setText(Escape.c(keyMap.get(currentKeyCode, m, TermKeyMap.APP_MODE_DEFAULT)));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void save(@NonNull final String name) {
        TermKeyMapManager.set(name, keyMap);
        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
    }

    public void save(View v) {
        final String name = ((EditText) findViewById(R.id.f_name)).getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_name_must_not_be_empty), Toast.LENGTH_SHORT).show();
        } else {
            if (TermKeyMapManager.contains(name))
                UiUtils.confirm(this, getString(R.string.prompt_overwrite), new Runnable() {
                    @Override
                    public void run() {
                        save(name);
                    }
                });
            else save(name);
        }
    }

    public void info(View v) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/keymap_escapes")));
    }
}
