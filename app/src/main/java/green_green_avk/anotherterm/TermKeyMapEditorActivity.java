package green_green_avk.anotherterm;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
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
import green_green_avk.anotherterm.whatsnew.WhatsNewDialog;

public final class TermKeyMapEditorActivity extends AppCompatActivity {
    private static final String KEYMAP_KEY = "E_KEYMAP";
    private static final String KEY_KEY = "E_KEY";
    private static final String NEED_SAVE_KEY = "E_NEED_SAVE";
    private static final int DEF_POS = 0;

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

        private int getPos(final int keyCode) {
            for (int i = 0; i < keys.length; i++) {
                if (keyCode == keys[i]) return i;
            }
            return -1;
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
                        .inflate(parent instanceof Spinner ?
                                        android.R.layout.simple_spinner_item
                                        : R.layout.sidepager_entry,
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

        private void setupView(final int position, @NonNull final View view) {
            final int code = keys[position];
            ((TextView) view).setText(TermKeyMap.keyCodeToString(code));
            final Resources res = view.getContext().getResources();
            view.setBackgroundColor(res.getColor(activity.isKeyCodeRedefined(code)
                    ? R.color.colorAccentTr
                    : android.R.color.transparent));
        }
    }

    private KeysAdapter keysAdapter = null;
    private CharSequence title = null;
    private CharSequence titleNeedSave = null;

    private TermKeyMapRules.Editable keyMap = null;
    private int currentKeyCode = -1;
    private boolean isNeedSave = false;

    private void setNeedSave(final boolean v) {
        if (v == isNeedSave) return;
        isNeedSave = v;
        setTitle(v ? titleNeedSave : title);
    }

    private final HashSet<Integer> redefinedKeyCodes = new HashSet<>();

    private void updateRedefinedKeyCodes(final int code) {
        for (int m = 0; m < TermKeyMap.MODIFIERS_SIZE; m++)
            if (keyMap.get(code, m, TermKeyMap.APP_MODE_NONE) != null
                    || keyMap.get(code, m, TermKeyMap.APP_MODE_DEFAULT) != null) {
                redefinedKeyCodes.add(code);
                return;
            }
        redefinedKeyCodes.remove(code);
    }

    private void updateRedefinedKeyCodes() {
        for (final int code : TermKeyMapRulesDefault.getSupportedKeys())
            updateRedefinedKeyCodes(code);
    }

    private boolean isKeyCodeRedefined(final int code) {
        return redefinedKeyCodes.contains(code);
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

    private boolean byUser = true;

    private void prepareKeyField(@NonNull final View v, final int m, final int am) {
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
                if (byUser && currentKeyCode >= 0) {
                    keyMap.set(currentKeyCode, m, am, nullUnescape(s.toString()));
                    updateRedefinedKeyCodes(currentKeyCode);
                    setNeedSave(true);
                    keysAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void refreshKeysView() {
        byUser = false;
        try {
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
        } finally {
            byUser = true;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEYMAP_KEY, keyMap);
        outState.putInt(KEY_KEY, currentKeyCode);
        outState.putBoolean(NEED_SAVE_KEY, isNeedSave);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = getTitle();
        titleNeedSave = new SpannableStringBuilder(title)
                .append(getText(R.string.title_suffix_has_unsaved_changes));
        setContentView(R.layout.term_key_map_editor_activity);

        UiUtils.enableAnimation(getWindow().getDecorView());

        String name = getIntent().getStringExtra(C.IFK_MSG_NAME);
        if (savedInstanceState != null) {
            keyMap = (TermKeyMapRules.Editable) savedInstanceState.get(KEYMAP_KEY);
            currentKeyCode = savedInstanceState.getInt(KEY_KEY, currentKeyCode);
            setNeedSave(savedInstanceState.getBoolean(NEED_SAVE_KEY, isNeedSave));
        } else if (getIntent().getData() != null) {
            try {
                final Uri uri = getIntent().getData();
                keyMap = TermKeyMapRulesParser.fromUri(uri);
                name = uri.getQueryParameter("name");
                setNeedSave(true);
            } catch (final IllegalArgumentException e) {
                keyMap = (TermKeyMapRules.Editable) TermKeyMapManager.getRules(name);
            }
        } else {
            keyMap = (TermKeyMapRules.Editable) TermKeyMapManager.getRules(name);
        }
        updateRedefinedKeyCodes();
        setName(name);
        final AdapterView<ListAdapter> keyView = findViewById(R.id.f_key);
        keysAdapter = new KeysAdapter(this);
        keyView.setAdapter(keysAdapter);

        final ViewGroup keysView = findViewById(R.id.keys);
        keysView.setSaveFromParentEnabled(false);
        final LayoutInflater inflater = LayoutInflater.from(this);
        for (int m = 0; m < TermKeyMap.MODIFIERS_SIZE; ++m) {
            final View v = inflater
                    .inflate(R.layout.term_key_map_entry, keysView, false);
            v.findViewById(R.id.shift).setVisibility((m & 1) != 0 ? View.VISIBLE : View.INVISIBLE);
            v.findViewById(R.id.alt).setVisibility((m & 2) != 0 ? View.VISIBLE : View.INVISIBLE);
            v.findViewById(R.id.ctrl).setVisibility((m & 4) != 0 ? View.VISIBLE : View.INVISIBLE);
            prepareKeyField(v.findViewById(R.id.normal), m, TermKeyMap.APP_MODE_NONE);
            prepareKeyField(v.findViewById(R.id.app), m, TermKeyMap.APP_MODE_DEFAULT);
            keysView.addView(v);
        }

        final int keyPos;
        if (currentKeyCode < 0) {
            keyPos = DEF_POS;
            currentKeyCode = (int) keysAdapter.getItemId(keyPos);
        } else keyPos = keysAdapter.getPos(currentKeyCode);
        if (keyView instanceof ListView) {
            ((ListView) keyView).setItemChecked(keyPos, true);
            ((ListView) keyView).smoothScrollToPosition(keyPos);
            refreshKeysView();
            keyView.setOnItemClickListener((parent, view, position, id) -> {
                ((ListView) parent).setItemChecked(position, true);
                currentKeyCode = (int) id;
                refreshKeysView();
            });
        } else {
            if (currentKeyCode >= 0)
                keyView.setSelection(keysAdapter.getPos(currentKeyCode));
            keyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                    currentKeyCode = (int) id;
                    refreshKeysView();
                }

                @Override
                public void onNothingSelected(final AdapterView<?> parent) {
                }
            });
        }

        WhatsNewDialog.showUnseen(this);
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

    private void close(@NonNull final Runnable r) {
        if (isNeedSave)
            UiUtils.confirm(this,
                    getString(R.string.msg_unsaved_changes_confirmation), r);
        else r.run();
    }

    @Override
    public void onBackPressed() {
        close(TermKeyMapEditorActivity.super::onBackPressed);
    }

    @Override
    public boolean onSupportNavigateUp() {
        close(TermKeyMapEditorActivity.super::onSupportNavigateUp);
        return true;
    }

    public void save(final View v) {
        final String name = getName();
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_name_must_not_be_empty), Toast.LENGTH_SHORT).show();
        } else {
            if (TermKeyMapManager.contains(name))
                UiUtils.confirm(this, getString(R.string.prompt_overwrite), () -> {
                    save(name);
                    setNeedSave(false);
                });
            else {
                save(name);
                setNeedSave(false);
            }
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
        updateRedefinedKeyCodes();
        keysAdapter.notifyDataSetChanged();
        setName(uri.getQueryParameter("name"));
        refreshKeysView();
        setNeedSave(true);
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
