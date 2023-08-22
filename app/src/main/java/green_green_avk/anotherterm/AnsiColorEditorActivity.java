package green_green_avk.anotherterm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import green_green_avk.anotherterm.ui.ColorPickerPopupView;
import green_green_avk.anotherterm.ui.ParameterViewBinder;
import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.utils.ValueConsumer;
import green_green_avk.anotherterm.utils.ValueProvider;
import green_green_avk.anotherterm.whatsnew.WhatsNewDialog;

public final class AnsiColorEditorActivity extends AppCompatActivity {
    private static final String DATA_KEY = "E_DATA";
    private static final String ORIG_DATA_KEY = "E_ORIG_DATA";
    private static final String IS_NEW_KEY = "E_IS_NEW";

    public static void start(@Nullable final Context context, @Nullable final String name) {
        if (context == null)
            return;
        context.startActivity(new Intent(context,
                AnsiColorEditorActivity.class)
                .putExtra(C.IFK_MSG_NAME, name));
    }

    private CharSequence title = null;
    private CharSequence titleNeedSave = null;

    private AnsiColorProfile.Editable colorMap = null;
    private AnsiColorProfile.Editable originalColorMap = null;
    private boolean isNeedSave = false;
    private boolean isNew = false;

    private void setNeedSave(final boolean v) {
        if (v == isNeedSave)
            return;
        isNeedSave = v;
        setTitle(v ? titleNeedSave : title);
    }

    private void updateNeedSave() {
        setNeedSave(isNew || !originalColorMap.dataEquals(colorMap));
    }

    private final ParameterViewBinder binder =
            new ParameterViewBinder(this::updateNeedSave);

    private void bind(@NonNull final ColorPickerPopupView view,
                      @NonNull final ValueProvider<Integer> provider,
                      @NonNull final ValueConsumer<? super Integer> consumer) {
        view.setHasAlpha(true);
        binder.bind(view, provider, consumer);
    }

    private void bind(@IdRes final int id,
                      @NonNull final ValueProvider<Integer> provider,
                      @NonNull final ValueConsumer<? super Integer> consumer) {
        final ColorPickerPopupView view = findViewById(id);
        bind(view, provider, consumer);
    }

    private void bind(@NonNull final View root,
                      @IdRes final int id,
                      @NonNull final ValueProvider<Integer> provider,
                      @NonNull final ValueConsumer<? super Integer> consumer) {
        final ColorPickerPopupView view = root.findViewById(id);
        bind(view, provider, consumer);
    }

    private void updateFields() {
        binder.updateAll();
    }

    private AnsiColorManager manager = null;

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_NEW_KEY, isNew);
        outState.putSerializable(ORIG_DATA_KEY, originalColorMap);
        outState.putSerializable(DATA_KEY, colorMap);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = AnsiColorManagerUi.instance.getManager(this);
        title = getTitle();
        titleNeedSave = new SpannableStringBuilder(title)
                .append(getText(R.string.title_suffix_has_unsaved_changes));

        setContentView(R.layout.color_map_editor_activity);

        String name = getIntent().getStringExtra(C.IFK_MSG_NAME);
        if (savedInstanceState != null) {
            isNew = savedInstanceState.getBoolean(IS_NEW_KEY, isNew);
            originalColorMap = (AnsiColorProfile.Editable) savedInstanceState.get(ORIG_DATA_KEY);
            colorMap = (AnsiColorProfile.Editable) savedInstanceState.get(DATA_KEY);
        } else {
            if (getIntent().getData() != null) {
                isNew = true;
                try {
                    final Uri uri = getIntent().getData();
                    originalColorMap = AnsiColorManager.fromUri(uri);
                    name = uri.getQueryParameter("name");
                } catch (final IllegalArgumentException e) {
                    originalColorMap = manager.getForEdit(name);
                    Toast.makeText(this,
                            R.string.msg_cannot_parse_uri,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                originalColorMap = manager.getForEdit(name);
            }
            colorMap = originalColorMap.clone();
        }
        updateNeedSave();

        setName(name);

        bind(R.id.f_color_def_bg,
                colorMap::getDefaultBg, colorMap::setDefaultBg);
        bind(R.id.f_color_def_fg_normal,
                colorMap::getDefaultFgNormal, colorMap::setDefaultFgNormal);
        bind(R.id.f_color_def_fg_bold,
                colorMap::getDefaultFgBold, colorMap::setDefaultFgBold);
        bind(R.id.f_color_def_fg_faint,
                colorMap::getDefaultFgFaint, colorMap::setDefaultFgFaint);

        final ViewGroup list = findViewById(R.id.list);
        for (int i = 0; i < 8; i++) {
            final View row = getLayoutInflater()
                    .inflate(R.layout.color_map_editor_entry, list, false);
            row.<TextView>findViewById(R.id.f_label)
                    .setText(String.format(Locale.getDefault(), "%d", i));
            final int idx = i;
            bind(row, R.id.f_color_normal,
                    () -> colorMap.getBasicNormal(idx),
                    v -> colorMap.setBasicNormal(idx, v));
            bind(row, R.id.f_color_bold,
                    () -> colorMap.getBasicBold(idx),
                    v -> colorMap.setBasicBold(idx, v));
            bind(row, R.id.f_color_faint,
                    () -> colorMap.getBasicFaint(idx),
                    v -> colorMap.setBasicFaint(idx, v));
            list.addView(row);
        }

        updateFields();

        WhatsNewDialog.showUnseen(this);
        if (savedInstanceState == null)
            UiUtils.checkStartPermissions(this);
    }

    @NonNull
    private String getName() {
        return ((EditText) findViewById(R.id.f_name)).getText().toString().trim();
    }

    private void setName(@Nullable final String name) {
        ((EditText) findViewById(R.id.f_name)).setText(name);
    }

    private void save(@NonNull final String name) {
        manager.set(name, colorMap);
        originalColorMap.set(colorMap);
        isNew = false;
        updateNeedSave();
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
        close(super::onBackPressed);
    }

    @Override
    public boolean onSupportNavigateUp() {
        close(super::onSupportNavigateUp);
        return true;
    }

    public void save(final View v) {
        final String name = getName();
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_name_must_not_be_empty),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (manager.containsCustom(name)) {
                UiUtils.confirm(this, getString(R.string.prompt_overwrite), () -> save(name));
            } else {
                save(name);
            }
        }
    }

    public void paste(final View view) {
        final Uri uri;
        try {
            uri = UiUtils.uriFromClipboard(this);
        } catch (final IllegalStateException e) {
            return;
        }
        try {
            AnsiColorManager.fromUri(colorMap, uri);
        } catch (final IllegalArgumentException e) {
            Toast.makeText(this,
                    R.string.msg_clipboard_does_not_contain_applicable_settings,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        setName(uri.getQueryParameter("name"));
        updateNeedSave();
        updateFields();
    }

    public void copy(final View view) {
        final String name = getName();
        final Uri uri = AnsiColorManager.toUri(colorMap).buildUpon()
                .appendQueryParameter("name", name).build();
        try {
            UiUtils.uriToClipboard(this, uri, getString(
                    R.string.title_terminal_s_link_s,
                    name,
                    getString(R.string.linktype_color_map_settings)));
        } catch (final IllegalStateException e) {
            return;
        }
        Toast.makeText(this, R.string.msg_copied_to_clipboard,
                Toast.LENGTH_SHORT).show();
    }

    public void share(final View view) {
        final Uri uri = AnsiColorManager.toUri(colorMap).buildUpon()
                .appendQueryParameter("name", getName()).build();
        UiUtils.shareUri(this, uri,
                getString(R.string.linktype_color_map_settings));
    }
}
