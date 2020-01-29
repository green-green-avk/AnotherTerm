package green_green_avk.anotherterm;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.utils.PreferenceUiWrapper;
import green_green_avk.anotherterm.utils.RawPreferenceUiWrapper;

public final class FavoriteEditorActivity extends AppCompatActivity {
    private final RawPreferenceUiWrapper mPrefs = new RawPreferenceUiWrapper();
    private ViewGroup mContainer;
    private EditText mNameW;
    private EditText mScrColsW;
    private EditText mScrRowsW;
    private Spinner mCharsetW;
    private Spinner mKeyMapW;
    private Spinner mTypeW;
    private String mOldName = null;
    private boolean mMakeNew = false;
    private View mCurrMSL = null;
    private PreferenceStorage mPrefsSt = new PreferenceStorage();
    private boolean mInSetPreferences = false;

    private static void warnByHint(@NonNull final View view, final String msg) {
        if (view instanceof TextView) {
            ((TextView) view).setHintTextColor(view.getResources().getColor(R.color.colorHintWarning));
            ((TextView) view).setHint(msg);
            view.requestFocus();
        }
    }

    public void share(final View view) {
        final PreferenceStorage ps = getPreferencesWithName();
        if (checkAndWarn(ps)) return;
        final Uri uri = BackendsList.toUri(ps.get());
        // https://stackoverflow.com/questions/29907030/sharing-text-plain-string-via-bluetooth-converts-data-into-html
        // So, via ContentProvider...
        ShareCompat.IntentBuilder.from(this).setType("text/html")
                .setStream(LinksProvider.getHtmlWithLink(uri)).startChooser();
    }

    public void copy(final View view) {
        final PreferenceStorage ps = getPreferencesWithName();
        if (checkAndWarn(ps)) return;
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        final Uri uri = BackendsList.toUri(ps.get());
//        clipboard.setPrimaryClip(ClipData.newRawUri("Favorite URI", BackendsList.toUri(ps.get())));
        clipboard.setPrimaryClip(new ClipData(getString(R.string.terminal_link_s, ps.get("name")),
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_URILIST},
                new ClipData.Item(uri.toString(), null, uri)));
        Toast.makeText(this, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public void paste(final View view) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        if (!clipboard.hasPrimaryClip()) return;
        final ClipData.Item cd = clipboard.getPrimaryClip().getItemAt(0);
        Uri uri = cd.getUri();
        if (uri == null) uri = Uri.parse(cd.coerceToText(this).toString());
        try {
            setPreferences(uri);
        } catch (final BackendModule.ParametersUriParseException e) {
            Toast.makeText(this, R.string.msg_clipboard_does_not_contain_any_settings, Toast.LENGTH_SHORT).show();
        }
    }

    public void remove(final View view) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mOldName != null)
                    FavoritesManager.remove(mOldName);
                finish();
                Toast.makeText(FavoriteEditorActivity.this, R.string.msg_favorite_deleted, Toast.LENGTH_SHORT).show();
            }
        };
        UiUtils.confirm(this, getString(R.string.do_you_want_to_delete_this_favorite), r);
    }

    public void clone(final View view) {
        if (mOldName == null) return;
        asNewFrom();
        Toast.makeText(this, R.string.msg_cloned, Toast.LENGTH_SHORT).show();
    }

    public void save(final View view) {
        final String name = getName();
        if (name == null) return;
        final PreferenceStorage ps = getPreferences();
        if (checkAndWarn(ps)) return;
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                FavoritesManager.set(name, ps);
                if ((!mMakeNew) && (!name.equals(mOldName))) {
                    FavoritesManager.remove(mOldName);
                }
                mOldName = name;
                asEdit();
                Toast.makeText(FavoriteEditorActivity.this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            }
        };
        if ((mMakeNew || !name.equals(mOldName)) && FavoritesManager.contains(name))
            UiUtils.confirm(this, getString(R.string.favorite_s_is_already_exists_replace, name), r);
        else r.run();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Nullable
    private String getName() {
        final String name = mNameW.getText().toString().trim();
        if (name.isEmpty()) {
            mNameW.setText("");
            warnByHint(mNameW, getString(R.string.field_must_not_be_empty));
            return null;
        }
        return name;
    }

    private static boolean checkAndWarn(@NonNull final PreferenceStorage ps) {
        return checkAndWarn(ps.get());
    }

    private static boolean checkAndWarn(@NonNull final Map<String, ?> pm) {
        boolean err = false;
        for (final String k : pm.keySet()) {
            final Object e = pm.get(k);
            if (e instanceof PreferenceUiWrapper.ParseException) {
                warnByHint(((PreferenceUiWrapper.ParseException) e).view,
                        ((PreferenceUiWrapper.ParseException) e).getLocalizedMessage());
                err = true;
            }
        }
        return err;
    }

    private static boolean dissolveErrors(@NonNull final Map<String, ?> pm) {
        boolean err = false;
        for (final String k : pm.keySet()) {
            final Object v = pm.get(k);
            if (v instanceof PreferenceUiWrapper.ParseException) {
                ((Map<String, Object>) pm).put(k, ((PreferenceUiWrapper.ParseException) v).value);
                err = true;
            }
        }
        return err;
    }

    @NonNull
    private PreferenceStorage getPreferencesWithName() {
        final PreferenceStorage ps = getPreferences();
        final String n = mNameW.getText().toString().trim();
        if (!n.isEmpty())
            ps.put("name", n);
        return ps;
    }

    private static int getSize(@NonNull final EditText et) {
        try {
            return Integer.parseInt(et.getText().toString());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    @NonNull
    private PreferenceStorage getPreferences() {
        final PreferenceStorage ps = new PreferenceStorage();
        ps.put("type", BackendsList.get(mTypeW.getSelectedItemPosition()).typeStr);
        ps.put("charset", C.charsetList.get(mCharsetW.getSelectedItemPosition()));
        ps.put("keymap", ((TermKeyMapManager.Meta) mKeyMapW.getSelectedItem()).name);
        ps.put("screen_cols", getSize(mScrColsW));
        ps.put("screen_rows", getSize(mScrRowsW));
        ps.putAll(mPrefs.getPreferences());
        return ps;
    }

    @NonNull
    private Map<String, ?> getDefaultPreferences() {
        final String type = BackendsList.get(mTypeW.getSelectedItemPosition()).typeStr;
        return BackendsList.getDefaultParameters(type);
    }

    private void addOptionsByTypeId(final int id) {
        final int layout = BackendsList.get(id).settingsLayout;
        if (layout == 0) return;
        mCurrMSL = getLayoutInflater().inflate(BackendsList.get(id).settingsLayout, mContainer, false);
        mCurrMSL.setSaveFromParentEnabled(false);
        mContainer.addView(mCurrMSL);
        mPrefs.addBranch(mCurrMSL);
        mPrefs.freeze(true);
        mPrefs.setPreferences(getDefaultPreferences());
        mPrefs.freeze(false);
        final Map<String, ?> values = new HashMap<>(mPrefsSt.get());
        values.keySet().retainAll(mPrefs.getChangedFields());
        mPrefs.setPreferences(values);
    }

    private void removeOptions() {
        if (mCurrMSL != null) {
            mPrefs.clear();
            mContainer.removeView(mCurrMSL);
            mCurrMSL = null;
        }
    }

    private static void setText(@NonNull final EditText et, final Object v) {
        et.setText(v == null ? "" : v.toString());
    }

    private static void setSizeText(@NonNull final EditText et, final Object v) {
        if (v instanceof Integer && ((int) v) <= 0 ||
                v instanceof Long && ((long) v) <= 0L) setText(et, "");
        else setText(et, v);
    }

    private void setPreferences(@NonNull final PreferenceStorage ps) {
        mPrefs.getChangedFields().addAll(ps.get().keySet());
        setPreferencesOnlyChanged(ps);
    }

    private void setPreferencesOnlyChanged(@NonNull final PreferenceStorage ps) {
        mPrefsSt = ps;
        final Object type = mPrefsSt.get("type");
        if (type != null) {
            final int pos = BackendsList.getId(type.toString());
            if (pos >= 0)
                if (pos == mTypeW.getSelectedItemPosition())
                    mPrefs.setPreferences(mPrefsSt.get());
                else {
                    mInSetPreferences = true;
                    mTypeW.setSelection(pos);
                }
        } else mPrefs.setPreferences(mPrefsSt.get());
        setSizeText(mScrColsW, mPrefsSt.get("screen_cols"));
        setSizeText(mScrRowsW, mPrefsSt.get("screen_rows"));
        final Object charset = mPrefsSt.get("charset");
        if (charset != null) {
            final int pos = C.charsetList.indexOf(charset.toString());
            if (pos >= 0)
                mCharsetW.setSelection(pos);
        }
        final Object keyMap = mPrefsSt.get("keymap");
        if (keyMap != null) {
            final String keyMapName = keyMap.toString();
            int pos = ((TermKeyMapAdapter) mKeyMapW.getAdapter()).getPosition(keyMapName);
            if (pos < 0) {
                ((TermKeyMapAdapter) mKeyMapW.getAdapter()).setZeroEntry(new TermKeyMapManager.Meta(
                        keyMapName,
                        getString(R.string.keymap_title_s_q_not_defined_here_q, keyMapName),
                        false));
                pos = 0;
            }
            mKeyMapW.setSelection(pos);
        }
    }

    private void setPreferences(@NonNull final Uri uri) {
        final PreferenceStorage ps = new PreferenceStorage();
        ps.putAll(BackendsList.fromUri(uri));
        setPreferences(ps);
        final Object n = ps.get("name");
        if (n != null) mNameW.setText(n.toString().trim());
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("E_OLD_NAME", mOldName);
        outState.putBoolean("E_NEW", mMakeNew);
        final Map<String, ?> pm = getPreferences().get();
        dissolveErrors(pm);
        mPrefsSt.putAll(pm);
        outState.putSerializable("E_PARAMS", (Serializable) mPrefsSt.get());
        outState.putStringArray("E_SET_PARAMS", mPrefs.getChangedFields().toArray(new String[0]));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_editor);

        UiUtils.enableAnimation(getWindow().getDecorView());

        mContainer = findViewById(R.id.container);
        mNameW = findViewById(R.id.fav_name);
        mScrColsW = findViewById(R.id.fav_scr_cols);
        mScrRowsW = findViewById(R.id.fav_scr_rows);
        mCharsetW = findViewById(R.id.fav_charset);
        mKeyMapW = findViewById(R.id.fav_keymap);
        mTypeW = findViewById(R.id.fav_type);

        mCharsetW.setSaveEnabled(false);
        mKeyMapW.setSaveEnabled(false);
        mTypeW.setSaveEnabled(false);

        mTypeW.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, BackendsList.getTitles(this)));
        mTypeW.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mInSetPreferences) mInSetPreferences = false;
                else {
                    final Map<String, Object> pm = mPrefs.getPreferences();
                    dissolveErrors(pm);
                    mPrefsSt.putAll(pm);
                }
                removeOptions();
                addOptionsByTypeId(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                removeOptions();
            }
        });
        mTypeW.setSelection(0);

        mCharsetW.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, C.charsetList));
        mCharsetW.setSelection(C.charsetList.indexOf(Charset.defaultCharset().name()));

        mKeyMapW.setAdapter(new TermKeyMapAdapter(getApplicationContext())
                .setIncludeBuiltIns(true)
                .setItemLayoutRes(android.R.layout.simple_list_item_1)
        );
        mKeyMapW.setSelection(((TermKeyMapAdapter) mKeyMapW.getAdapter()).getPosition(null));

        if (savedInstanceState != null) {
            Collections.addAll(mPrefs.getChangedFields(), savedInstanceState.getStringArray("E_SET_PARAMS"));
            setPreferencesOnlyChanged(new PreferenceStorage((Map<String, ?>) savedInstanceState.get("E_PARAMS")));
            mOldName = savedInstanceState.getString("E_OLD_NAME");
            mMakeNew = savedInstanceState.getBoolean("E_NEW");
            if (mMakeNew) {
                if (mOldName != null) asNewFrom();
                else asNew();
            } else asEdit();
            return;
        }

        final Intent intent = getIntent();
        final Uri uri = intent.getData();
        if (uri != null) {
            mOldName = null;
            asNew();
            try {
                setPreferences(uri);
            } catch (final BackendModule.ParametersUriParseException e) {
                Toast.makeText(this, R.string.msg_cannot_parse_uri, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        final String name = intent.getStringExtra(C.IFK_MSG_NAME);
        mOldName = name;
        if (name != null) {
            mMakeNew = intent.getBooleanExtra(C.IFK_MSG_NEW, false);
            if (mMakeNew) asNewFrom();
            else asEdit();
            mNameW.setText(name);
            setPreferences(FavoritesManager.get(name));
        } else {
            asNew();
        }
    }

    private void asNew() {
        mMakeNew = true;
        setTitle(R.string.new_favorite);
        findViewById(R.id.b_remove).setVisibility(View.GONE);
        findViewById(R.id.b_clone).setVisibility(View.GONE);
    }

    private void asNewFrom() {
        mMakeNew = true;
        setTitle(getString(R.string.new_favorite_from_s, mOldName));
        findViewById(R.id.b_remove).setVisibility(View.GONE);
        findViewById(R.id.b_clone).setVisibility(View.GONE);
    }

    private void asEdit() {
        mMakeNew = false;
        setTitle(getString(R.string.edit_favorite_s, mOldName));
        findViewById(R.id.b_remove).setVisibility(View.VISIBLE);
        findViewById(R.id.b_clone).setVisibility(View.VISIBLE);
    }
}
