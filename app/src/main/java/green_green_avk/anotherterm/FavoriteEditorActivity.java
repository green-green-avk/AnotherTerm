package green_green_avk.anotherterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.ui.ExtAppCompatActivity;
import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.ui.ViewValueListener;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.utils.PreferenceUiWrapper;
import green_green_avk.anotherterm.utils.RawPreferenceUiWrapper;
import green_green_avk.anotherterm.whatsnew.WhatsNewDialog;

// TODO: Refactor!!!

public final class FavoriteEditorActivity extends ExtAppCompatActivity {
    private static final int INITIAL_TYPE_ID = 0;

    private static final List<String> TERM_COMPLIANCE_KEYS = Arrays.asList("ansi", "vt52compat");

    private final RawPreferenceUiWrapper mPrefs = new RawPreferenceUiWrapper();
    private ViewGroup mContainer;
    private EditText mNameW;
    private EditText mScrColsW;
    private EditText mScrRowsW;
    private CompoundButton mFontSizeAutoW;
    private CompoundButton mTerminateOD;
    private CompoundButton mTerminateODIfPES0;
    private CompoundButton mWakeLockAOC;
    private CompoundButton mWakeLockROD;
    private Spinner mTermCompliance;
    private Spinner mCharsetW;
    private Spinner mColorMapW;
    private Spinner mKeyMapW;
    private Spinner mTypeW;
    private ViewGroup mTokenG;
    private ViewGroup mTokenFG;
    private TextView mTokenW;
    private String mOldName = null;
    private boolean mMakeNew = false;
    private View mCurrMSL = null;
    private PreferenceStorage mPrefsSt = new PreferenceStorage();
    private boolean mInSetPreferences = false;
    private boolean isNeedSave = false;

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
        UiUtils.shareUri(this, BackendsList.toUri(ps.get()),
                getString(R.string.linktype_connection_settings));
    }

    public void copy(final View view) {
        final PreferenceStorage ps = getPreferencesWithName();
        if (checkAndWarn(ps)) return;
        try {
            UiUtils.uriToClipboard(this, BackendsList.toUri(ps.get()),
                    getString(R.string.title_terminal_s_link_s, ps.get("name"),
                            getString(R.string.linktype_connection_settings)));
        } catch (final IllegalStateException e) {
            return;
        }
        Toast.makeText(this, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public void paste(final View view) {
        final Uri uri;
        try {
            uri = UiUtils.uriFromClipboard(this);
        } catch (final IllegalStateException e) {
            return;
        }
        try {
            setPreferences(uri);
        } catch (final BackendModule.ParametersUriParseException e) {
            Toast.makeText(this, R.string.msg_clipboard_does_not_contain_applicable_settings,
                    Toast.LENGTH_SHORT).show();
        }
        setNeedSave(true);
    }

    public void remove(final View view) {
        final Runnable r = () -> {
            if (mOldName != null)
                FavoritesManager.remove(mOldName);
            finish();
            Toast.makeText(this, R.string.msg_favorite_deleted, Toast.LENGTH_SHORT).show();
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
        final Runnable r = () -> {
            FavoritesManager.set(name, ps);
            if ((!mMakeNew) && (!name.equals(mOldName))) {
                FavoritesManager.remove(mOldName);
            }
            mOldName = name;
            isNeedSave = false;
            asEdit();
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
        };
        if ((mMakeNew || !name.equals(mOldName)) && FavoritesManager.contains(name))
            UiUtils.confirm(this, getString(R.string.msg_favorite_s_is_already_exists_replace, name), r);
        else r.run();
    }

    private void close(@NonNull final Runnable r) {
        if (isNeedSave)
            UiUtils.confirm(this,
                    getString(R.string.msg_unsaved_changes_confirmation), r);
        else r.run();
    }

    @Override
    public void onBackPressed() {
        close(FavoriteEditorActivity.super::onBackPressed);
    }

    @Override
    public boolean onSupportNavigateUp() {
        close(super::onSupportNavigateUp);
        return true;
    }

    public void copyToken(final View view) {
        UiUtils.toClipboard(this, mTokenW.getText().toString());
    }

    public void generateToken(final View view) {
        mTokenW.setText(UUID.randomUUID().toString());
        mTokenFG.setVisibility(View.VISIBLE);
        setNeedSave(true);
    }

    public void deleteToken(final View view) {
        deleteToken();
        setNeedSave(true);
    }

    private void deleteToken() {
        mTokenFG.setVisibility(View.INVISIBLE);
        mTokenW.setText("");
    }

    public void infoToken(final View view) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/fav_token")));
    }

    public void infoShareable(final View view) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/share_input")));
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

    private static boolean dissolveErrors(@NonNull final Map<String, Object> pm) {
        boolean err = false;
        for (final String k : pm.keySet()) {
            final Object v = pm.get(k);
            if (v instanceof PreferenceUiWrapper.ParseException) {
                pm.put(k, ((PreferenceUiWrapper.ParseException) v).value);
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
        final String token = mTokenW.getText().toString();
        if (token.length() >= ControlService.FAV_TOKEN_LENGTH_MIN)
            ps.put(ControlService.FAV_TOKEN_KEY, token);
        ps.put("term_compliance", TERM_COMPLIANCE_KEYS
                .get(mTermCompliance.getSelectedItemPosition()));
        ps.put("charset", C.charsetList.get(mCharsetW.getSelectedItemPosition()));
        ps.put("colormap", ((AnsiColorManager.Meta) mColorMapW.getSelectedItem()).name);
        ps.put("keymap", ((TermKeyMapManager.Meta) mKeyMapW.getSelectedItem()).name);
        ps.put("screen_cols", getSize(mScrColsW));
        ps.put("screen_rows", getSize(mScrRowsW));
        ps.put("font_size_auto", mFontSizeAutoW.isChecked() &&
                mFontSizeAutoW.getVisibility() == View.VISIBLE);
        ps.put("terminate.on_disconnect", mTerminateODIfPES0.isChecked()
                ? C.COND_STR_PROCESS_EXIT_STATUS_0
                : Boolean.valueOf(mTerminateOD.isChecked()));
        ps.put("wakelock.acquire_on_connect", mWakeLockAOC.isChecked());
        ps.put("wakelock.release_on_disconnect", mWakeLockROD.isChecked());
        ps.putAll(mPrefs.getPreferences());
        return ps;
    }

    @NonNull
    private Map<String, Object> getDefaultPreferences() {
        final String type = BackendsList.get(mTypeW.getSelectedItemPosition()).typeStr;
        return BackendsList.getDefaultParameters(type);
    }

    private void addOptionsByTypeId(final int id) {
        mTokenG.setVisibility(BackendsList.get(id).exportable ? View.VISIBLE : View.GONE);
        mTerminateODIfPES0.setVisibility((BackendsList.get(id).meta.getDisconnectionReasonTypes()
                & BackendModule.DisconnectionReason.PROCESS_EXIT)
                == BackendModule.DisconnectionReason.PROCESS_EXIT
                ? View.VISIBLE : View.GONE);
        final int layout = BackendsList.get(id).settingsLayout;
        if (layout == 0)
            return;
        mCurrMSL = getLayoutInflater()
                .inflate(BackendsList.get(id).settingsLayout,
                        mContainer, false);
        mCurrMSL.setSaveFromParentEnabled(false);
        mContainer.addView(mCurrMSL);
        mPrefs.setPreferencesMeta(BackendsList.get(id).meta.getParametersMeta());
        mPrefs.addBranch(mCurrMSL);
        mPrefs.setDefaultPreferences(getDefaultPreferences());
        final Map<String, Object> values = new HashMap<>(mPrefsSt.get());
        values.keySet().retainAll(mPrefs.getChangedFields());
        mPrefs.setPreferences(values);
    }

    private void removeOptions() {
        if (mCurrMSL != null) {
            mPrefs.removeBranches();
            mPrefs.setPreferencesMeta(Collections.emptyMap());
            mContainer.removeView(mCurrMSL);
            mCurrMSL = null;
        }
        mTokenG.setVisibility(View.GONE);
        mTerminateODIfPES0.setVisibility(View.GONE);
    }

    private static void setText(@NonNull final EditText et, final Object v) {
        et.setText(v == null ? "" : v.toString());
    }

    private static void setSizeText(@NonNull final EditText et, Object v) {
        if (v instanceof String) {
            try {
                v = Long.parseLong((String) v);
            } catch (final NumberFormatException e) {
                v = 0;
            }
        }
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
        final Object token = mPrefsSt.get(ControlService.FAV_TOKEN_KEY);
        if (token instanceof String && !((String) token).isEmpty()) {
            mTokenW.setText((String) token);
            mTokenFG.setVisibility(View.VISIBLE);
        } else {
            mTokenFG.setVisibility(View.INVISIBLE);
            mTokenW.setText("");
        }
        setSizeText(mScrColsW, mPrefsSt.get("screen_cols"));
        setSizeText(mScrRowsW, mPrefsSt.get("screen_rows"));
        mFontSizeAutoW.setChecked(Misc.toBoolean(mPrefsSt.get("font_size_auto")));
        final Object terminateOD = mPrefsSt.get("terminate.on_disconnect");
        if (C.COND_STR_PROCESS_EXIT_STATUS_0.equals(terminateOD)) {
            mTerminateOD.setChecked(true);
            mTerminateODIfPES0.setChecked(true);
        } else {
            mTerminateOD.setChecked(Misc.toBoolean(terminateOD));
            mTerminateODIfPES0.setChecked(false);
        }
        mWakeLockAOC.setChecked(Misc.toBoolean(mPrefsSt.get("wakelock.acquire_on_connect")));
        mWakeLockROD.setChecked(Misc.toBoolean(mPrefsSt.get("wakelock.release_on_disconnect")));
        final Object termCompliance = mPrefsSt.get("term_compliance");
        if (termCompliance != null) {
            final int pos = TERM_COMPLIANCE_KEYS.indexOf(termCompliance.toString());
            if (pos >= 0)
                mTermCompliance.setSelection(pos);
        }
        final Object charset = mPrefsSt.get("charset");
        if (charset != null) {
            final int pos = C.charsetList.indexOf(charset.toString());
            if (pos >= 0)
                mCharsetW.setSelection(pos);
        }
        final Object colorMap = mPrefsSt.get("colormap");
        if (colorMap != null) {
            final String colorMapName = colorMap.toString();
            int pos = AnsiColorAdapter.getSelf(mColorMapW.getAdapter())
                    .getPosition(colorMapName);
            if (pos < 0) {
                AnsiColorAdapter.getSelf(mColorMapW.getAdapter())
                        .setZeroEntry(new AnsiColorManager.Meta(colorMapName,
                                getString(R.string.profile_title_s_q_not_defined_here_q,
                                        colorMapName),
                                false));
                pos = 0;
            }
            mColorMapW.setSelection(pos);
        }
        final Object keyMap = mPrefsSt.get("keymap");
        if (keyMap != null) {
            final String keyMapName = keyMap.toString();
            int pos = TermKeyMapAdapter.getSelf(mKeyMapW.getAdapter())
                    .getPosition(keyMapName);
            if (pos < 0) {
                TermKeyMapAdapter.getSelf(mKeyMapW.getAdapter())
                        .setZeroEntry(new TermKeyMapManager.Meta(keyMapName,
                                getString(R.string.profile_title_s_q_not_defined_here_q,
                                        keyMapName),
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

    private void refreshScreenParams() {
        mFontSizeAutoW.setVisibility((mScrColsW.getText().toString().trim().isEmpty()
                && mScrRowsW.getText().toString().trim().isEmpty())
                ? View.GONE : View.VISIBLE);
    }

    private CharSequence editorTitle = null;

    private void setEditorTitle(@NonNull final CharSequence v) {
        editorTitle = v;
        refreshEditorTitle();
    }

    private void refreshEditorTitle() {
        setTitle(isNeedSave ? new SpannableStringBuilder(editorTitle)
                .append(getText(R.string.title_suffix_has_unsaved_changes)) : editorTitle);
    }

    private void setNeedSave(final boolean v) {
        if (v == isNeedSave) return;
        isNeedSave = v;
        refreshEditorTitle();
    }

    // TODO: Get rid of this mess.
    private boolean isInInit = true;
    private boolean isTypeOptInit = false;
    private boolean isMainOptsInit = false;
    private boolean isModuleOptsInit = false;
    private final ViewValueListener lNeedSave = new ViewValueListener() {
        @Override
        protected void onChanged(@NonNull final View view, @Nullable final Object value) {
            if (!isInInit) {
                setNeedSave(true);
                if (view == mTerminateOD && Boolean.FALSE.equals(value))
                    mTerminateODIfPES0.setChecked(false);
                if (view == mTerminateODIfPES0 && Boolean.TRUE.equals(value))
                    mTerminateOD.setChecked(true);
            }
        }
    };
    private final ViewValueListener lNeedSaveDelayed = new ViewValueListener() {
        private int toInit = 0;

        @Override
        protected void onChanged(@NonNull final View view, @Nullable final Object value) {
            if (!isInInit)
                setNeedSave(true);
            if (!isMainOptsInit) {
                toInit--;
                if (toInit == 0) {
                    isMainOptsInit = true;
                    onInitComplete();
                }
            }
        }

        @Override
        public void adoptView(@NonNull final View view) {
            toInit++;
            super.adoptView(view);
        }
    };

    private void onInitComplete() {
        if (isTypeOptInit && isMainOptsInit && isModuleOptsInit) {
            // Preferences loading ends here.
            // (AdepterView usually posts onItemSelected() execution on some weird time.)
            isInInit = false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("E_OLD_NAME", mOldName);
        outState.putBoolean("E_NEW", mMakeNew);
        final Map<String, Object> pm = getPreferences().get();
        dissolveErrors(pm);
        mPrefsSt.putAll(pm);
        outState.putSerializable("E_PARAMS", (Serializable) mPrefsSt.get());
        outState.putStringArray("E_SET_PARAMS", mPrefs.getChangedFields().toArray(new String[0]));
        outState.putBoolean("E_NEED_SAVE", isNeedSave);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs.setCallbacks(new PreferenceUiWrapper.Callbacks() {
            @Override
            public void onInitialized() {
                isModuleOptsInit = true;
                onInitComplete();
            }

            @Override
            public void onChanged(final String key) {
                if (!isInInit)
                    setNeedSave(true);
            }
        });
        editorTitle = getTitle();
        setContentView(R.layout.favorite_editor_activity);

        UiUtils.enableAnimation(getWindow().getDecorView());

        mContainer = findViewById(R.id.container);
        mNameW = findViewById(R.id.fav_name);
        mScrColsW = findViewById(R.id.fav_scr_cols);
        mScrRowsW = findViewById(R.id.fav_scr_rows);
        mFontSizeAutoW = findViewById(R.id.fav_font_size_auto);
        mTerminateOD = findViewById(R.id.fav_terminate_on_disconnect);
        mTerminateODIfPES0 = findViewById(R.id.fav_terminate_on_disconnect_if_pes_0);
        mWakeLockAOC = findViewById(R.id.fav_wakelock_acquire_on_connect);
        mWakeLockROD = findViewById(R.id.fav_wakelock_release_on_disconnect);

        mTermCompliance = findViewById(R.id.fav_term_compliance);
        mCharsetW = findViewById(R.id.fav_charset);
        mColorMapW = findViewById(R.id.fav_colormap);
        mKeyMapW = findViewById(R.id.fav_keymap);
        mTypeW = findViewById(R.id.fav_type);
        mTokenG = findViewById(R.id.g_token);
        mTokenFG = findViewById(R.id.g_f_token);
        mTokenW = findViewById(R.id.fav_token);

        mTermCompliance.setSaveEnabled(false);
        mCharsetW.setSaveEnabled(false);
        mColorMapW.setSaveEnabled(false);
        mKeyMapW.setSaveEnabled(false);
        mTypeW.setSaveEnabled(false);

        lNeedSave.adoptView(mNameW);

        final TextWatcher colsRowsWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start,
                                          final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                if (!isInInit)
                    setNeedSave(true);
                refreshScreenParams();
            }

            @Override
            public void afterTextChanged(final Editable s) {
            }
        };
        mScrColsW.addTextChangedListener(colsRowsWatcher);
        mScrRowsW.addTextChangedListener(colsRowsWatcher);
        refreshScreenParams();
        lNeedSave.adoptView(mFontSizeAutoW);

        lNeedSave.adoptView(mTerminateOD);
        lNeedSave.adoptView(mTerminateODIfPES0);
        lNeedSave.adoptView(mWakeLockAOC);
        lNeedSave.adoptView(mWakeLockROD);

        lNeedSaveDelayed.adoptView(mTermCompliance);
        lNeedSaveDelayed.adoptView(mCharsetW);
        lNeedSaveDelayed.adoptView(mColorMapW);
        lNeedSaveDelayed.adoptView(mKeyMapW);

        final ArrayAdapter<String> aType = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                BackendsList.getTitles(this));
        aType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeW.setAdapter(aType);
        mTypeW.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view,
                                       final int position, final long id) {
                if (mInSetPreferences)
                    mInSetPreferences = false;
                else {
                    final Map<String, Object> pm = mPrefs.getPreferences();
                    dissolveErrors(pm);
                    mPrefsSt.putAll(pm);
                }
                removeOptions();
                addOptionsByTypeId(position);
                if (!isInInit)
                    setNeedSave(true);
                if (!isTypeOptInit) {
                    isTypeOptInit = true;
                    onInitComplete();
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                removeOptions();
            }
        });
        mTypeW.setSelection(INITIAL_TYPE_ID);

        final ArrayAdapter<String> aCharset = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, C.charsetList);
        aCharset.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCharsetW.setAdapter(aCharset);
        mCharsetW.setSelection(C.charsetList.indexOf(Charset.defaultCharset().name()));

        mColorMapW.setAdapter(new AnsiColorAdapter(this)
                .setIncludeBuiltIns(true)
                .setItemLayoutRes(android.R.layout.simple_spinner_item)
                .setDropDownItemLayoutRes(android.R.layout.simple_spinner_dropdown_item)
                .getAdapter());
        mColorMapW.setSelection(AnsiColorAdapter.getSelf(mColorMapW.getAdapter())
                .getPosition(null));

        mKeyMapW.setAdapter(new TermKeyMapAdapter(this)
                .setIncludeBuiltIns(true)
                .setItemLayoutRes(android.R.layout.simple_spinner_item)
                .setDropDownItemLayoutRes(android.R.layout.simple_spinner_dropdown_item)
                .getAdapter());
        mKeyMapW.setSelection(TermKeyMapAdapter.getSelf(mKeyMapW.getAdapter())
                .getPosition(null));

        if (savedInstanceState != null) {
            Collections.addAll(mPrefs.getChangedFields(),
                    savedInstanceState.getStringArray("E_SET_PARAMS"));
            setPreferencesOnlyChanged(new PreferenceStorage(
                    (Map<String, Object>) savedInstanceState.get("E_PARAMS")));
            mOldName = savedInstanceState.getString("E_OLD_NAME");
            mMakeNew = savedInstanceState.getBoolean("E_NEW");
            isNeedSave = savedInstanceState.getBoolean("E_NEED_SAVE", isNeedSave);
            if (mMakeNew) {
                if (mOldName != null) asNewFrom();
                else asNew();
            } else asEdit();
            return;
        }

        final Intent intent = getIntent();
        final Uri uri = intent.getData();
        if (uri != null) {
            isNeedSave = true;
            mOldName = null;
            asNew();
            try {
                setPreferences(uri);
            } catch (final BackendModule.ParametersUriParseException e) {
                Toast.makeText(this, R.string.msg_cannot_parse_uri, Toast.LENGTH_SHORT).show();
            }
            WhatsNewDialog.showUnseen(this);
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
        deleteToken();
        setEditorTitle(getString(R.string.new_favorite));
        findViewById(R.id.b_remove).setVisibility(View.GONE);
        findViewById(R.id.b_clone).setVisibility(View.GONE);
    }

    private void asNewFrom() {
        mMakeNew = true;
        deleteToken();
        setEditorTitle(getString(R.string.new_favorite_from_s, mOldName));
        findViewById(R.id.b_remove).setVisibility(View.GONE);
        findViewById(R.id.b_clone).setVisibility(View.GONE);
    }

    private void asEdit() {
        mMakeNew = false;
        setEditorTitle(getString(R.string.edit_favorite_s, mOldName));
        findViewById(R.id.b_remove).setVisibility(View.VISIBLE);
        findViewById(R.id.b_clone).setVisibility(View.VISIBLE);
    }
}
