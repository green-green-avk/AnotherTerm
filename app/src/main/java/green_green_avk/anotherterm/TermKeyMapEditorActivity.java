package green_green_avk.anotherterm;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.HashSet;

import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.utils.Escape;
import green_green_avk.anotherterm.utils.Unescape;

public final class TermKeyMapEditorActivity extends AppCompatActivity {
    public static void start(@Nullable final Context context, @Nullable final String name) {
        if (context == null) return;
        context.startActivity(new Intent(context, TermKeyMapEditorActivity.class)
                .putExtra(C.IFK_MSG_NAME, name));
    }

    private static final class KeysAdapter extends BaseAdapter {
        private static final int[] keys;

        static {
            keys = new int[TermKeyMapRulesDefault.getSupportedKeys().size()];
            int i = 0;
            for (final int k : TermKeyMapRulesDefault.getSupportedKeys()) keys[i++] = k;
            Arrays.sort(keys);
        }

        private final TermKeyMapEditorActivity activity;

        private KeysAdapter(@NonNull final TermKeyMapEditorActivity a) {
            activity = a;
        }

        @Override
        public int getCount() {
            return TermKeyMapRulesDefault.getSupportedKeys().size();
        }

        @Override
        public Object getItem(final int position) {
            return keys[position];
        }

        @Override
        public long getItemId(final int position) {
            return keys[position];
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (!(convertView instanceof TextView))
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_spinner_item,
                                parent, false);
            setupView(position, convertView);
            return convertView;
        }

        @Override
        public View getDropDownView(final int position, View convertView, final ViewGroup parent) {
            if (!(convertView instanceof TextView))
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_spinner_dropdown_item,
                                parent, false);
            setupView(position, convertView);
            return convertView;
        }

        private void setupView(final int position, @NonNull final View convertView) {
            final int code = keys[position];
            ((TextView) convertView).setText(TermKeyMap.keyCodeToString(code));
            final Resources res = convertView.getContext().getResources();
            convertView.setBackgroundColor(res.getColor(activity.isKeyCodeChanged(code)
                    ? R.color.colorAccentTr
                    : android.R.color.transparent));
        }
    }

    private KeysAdapter keysAdapter = null;

    private TermKeyMapRules.Editable keyMap = null;
    private int currentKeyCode = -1;

    private final HashSet<Integer> changedKeyCodes = new HashSet<>();

    private void updateChangedKeyCodes(final int code) {
        for (int m = 0; m < TermKeyMap.MODIFIERS_SIZE; m++)
            if (keyMap.get(code, m, TermKeyMap.APP_MODE_NONE) != null
                    || keyMap.get(code, m, TermKeyMap.APP_MODE_DEFAULT) != null) {
                changedKeyCodes.add(code);
                return;
            }
        changedKeyCodes.remove(code);
    }

    private void updateChangedKeyCodes() {
        for (final int code : TermKeyMapRulesDefault.getSupportedKeys())
            updateChangedKeyCodes(code);
    }

    private boolean isKeyCodeChanged(final int code) {
        return changedKeyCodes.contains(code);
    }

    @Nullable
    private static String nullUnescape(@NonNull final String v) {
        if (v.isEmpty()) return null;
        try {
            return Unescape.c(v);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private void prepareKeyField(final View v, final int m, final int am) {
        ((EditText) v).addTextChangedListener(new TextWatcher() {
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
                if (currentKeyCode >= 0) {
                    keyMap.set(currentKeyCode, m, am, nullUnescape(s.toString()));
                    updateChangedKeyCodes(currentKeyCode);
                    keysAdapter.notifyDataSetChanged();
                }
            }
        });
        /*
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                if (!hasFocus && currentKeyCode >= 0)
                    keyMap.set(currentKeyCode, m, am, ((EditText) v).getText().toString());
            }
        });
        */
    }

    private void refreshKeysView() {
        final ViewGroup keysView = findViewById(R.id.keys);
        for (int m = 0; m < TermKeyMap.MODIFIERS_SIZE; ++m) {
            final View v = keysView.getChildAt(m);
            final EditText nView = v.findViewById(R.id.normal);
            final EditText aView = v.findViewById(R.id.app);
            nView.setHint(Escape.c(TermKeyMapManager.defaultKeyMap.get(currentKeyCode, m,
                    TermKeyMap.APP_MODE_NONE)));
            aView.setHint(Escape.c(TermKeyMapManager.defaultKeyMap.get(currentKeyCode, m,
                    TermKeyMap.APP_MODE_DEFAULT)));
            nView.setText(Escape.c(keyMap.get(currentKeyCode, m,
                    TermKeyMap.APP_MODE_NONE)));
            aView.setText(Escape.c(keyMap.get(currentKeyCode, m,
                    TermKeyMap.APP_MODE_DEFAULT)));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("E_KEYMAP", keyMap);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term_key_map_editor);

        UiUtils.enableAnimation(getWindow().getDecorView());

        String name = getIntent().getStringExtra(C.IFK_MSG_NAME);
        if (savedInstanceState != null) {
            keyMap = (TermKeyMapRules.Editable) savedInstanceState.get("E_KEYMAP");
        } else if (getIntent().getData() != null) {
            try {
                final Uri uri = getIntent().getData();
                keyMap = TermKeyMapRulesParser.fromUri(uri);
                name = uri.getQueryParameter("name");
            } catch (final IllegalArgumentException e) {
                keyMap = (TermKeyMapRules.Editable) TermKeyMapManager.getRules(name);
            }
        } else {
            keyMap = (TermKeyMapRules.Editable) TermKeyMapManager.getRules(name);
        }
        updateChangedKeyCodes();
        setName(name);
        final Spinner keyView = findViewById(R.id.f_key);
        keysAdapter = new KeysAdapter(this);
        keyView.setAdapter(keysAdapter);

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
            public void onItemSelected(final AdapterView<?> parent, final View view,
                                       final int position, final long id) {
                currentKeyCode = (int) id;
                refreshKeysView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @NonNull
    private String getName() {
        return ((EditText) findViewById(R.id.f_name)).getText().toString().trim();
    }

    private void setName(@Nullable final String name) {
        ((EditText) findViewById(R.id.f_name)).setText(name);
    }

    private void save(@NonNull final String name) {
        TermKeyMapManager.set(name, keyMap);
        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
    }

    public void save(final View v) {
        final String name = getName();
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

    public void paste(final View view) {
        if (!(keyMap instanceof TermKeyMapRules.UriImportable))
            return;
        final Uri uri;
        try {
            uri = UiUtils.uriFromClipboard(this);
        } catch (final IllegalStateException e) {
            return;
        }
        try {
            ((TermKeyMapRules.UriImportable) keyMap).fromUri(uri);
        } catch (final IllegalArgumentException e) {
            Toast.makeText(this, R.string.msg_clipboard_does_not_contain_applicable_settings,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        updateChangedKeyCodes();
        keysAdapter.notifyDataSetChanged();
        setName(uri.getQueryParameter("name"));
        refreshKeysView();
    }

    public void copy(final View view) {
        if (!(keyMap instanceof TermKeyMapRules.UriExportable))
            return;
        final String name = getName();
        final Uri uri = ((TermKeyMapRules.UriExportable) keyMap).toUri().buildUpon()
                .appendQueryParameter("name", name).build();
        try {
            UiUtils.uriToClipboard(this, uri, getString(R.string.title_terminal_s_link_s,
                    name,
                    getString(R.string.linktype_key_map_settings)));
        } catch (final IllegalStateException e) {
            return;
        }
        Toast.makeText(this, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public void share(final View view) {
        if (!(keyMap instanceof TermKeyMapRules.UriExportable))
            return;
        final Uri uri = ((TermKeyMapRules.UriExportable) keyMap).toUri().buildUpon()
                .appendQueryParameter("name", getName()).build();
        UiUtils.shareUri(this, uri, getString(R.string.linktype_key_map_settings));
    }

    public void info(final View v) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/keymap_escapes")));
    }
}
