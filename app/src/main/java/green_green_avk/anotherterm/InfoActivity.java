package green_green_avk.anotherterm;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

import green_green_avk.anotherterm.ui.HtmlTextView;

public final class InfoActivity extends AppCompatActivity {

    private static final class Source {
        private enum Type {XML, HTML, PLAIN}

        private final int id;
        private final Type type;

        private Source(@StringRes final int id, final Type type) {
            this.id = id;
            this.type = type;
        }
    }

    private static final Map<String, Source> res = new HashMap<>();

    static {
        res.put("/no_info", new Source(R.string.msg_no_info_page, Source.Type.XML));
        res.put("/8_bit_C1", new Source(R.string.desc_8_bit_c1_mode_help, Source.Type.XML));
        res.put("/keymap_escapes", new Source(R.string.desc_keymap_escapes, Source.Type.XML));
        res.put("/scratchpad", new Source(R.string.desc_scratchpad_help, Source.Type.XML));
        res.put("/share_input", new Source(R.string.desc_share_input_help, Source.Type.XML));
        res.put("/fav_token", new Source(R.string.desc_fav_token_help, Source.Type.XML));
        res.put("/shell_perm_favmgmt", new Source(R.string.desc_favorites_management, Source.Type.XML));
        res.put("/shell_perm_pluginexec", new Source(R.string.desc_plugins_execution, Source.Type.XML));
        res.put("/shell_perm_clipboard-copy", new Source(R.string.desc_copy_to_clipboard, Source.Type.XML));
        res.put("/shell_env_man", new Source(R.string.desc_shell_env_help, Source.Type.XML));
        res.put("/termsh_man", new Source(R.string.desc_termsh_help, Source.Type.XML));
        res.put("/help", new Source(R.string.desc_main_help, Source.Type.XML));
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        new AsyncLayoutInflater(this).inflate(R.layout.info_activity,
                (ViewGroup) getWindow().getDecorView().getRootView(),
                (view, i, viewGroup) -> {
                    try {
                        final Uri uri = getIntent().getData();
                        if (uri == null) return;
                        final HtmlTextView wText = view.findViewById(R.id.desc);
                        final String content;
                        final Source.Type type;
                        if ("info".equals(uri.getScheme())) {
                            final Source source = res.get(uri.getPath());
                            if (source == null) return;
                            content = getString(source.id);
                            type = source.type;
                        } else if (ContentResolver.SCHEME_ANDROID_RESOURCE.
                                equals(uri.getScheme())) {
                            final String refApp = uri.getAuthority();
                            if (refApp == null) return;
                            final String refRes = uri.getLastPathSegment();
                            if (refRes == null) return;
                            try {
                                content = this.getPackageManager()
                                        .getResourcesForApplication(refApp)
                                        .getString(Integer.parseInt(refRes));
                            } catch (final NumberFormatException e) {
                                wText.setXmlText(getString(R.string.msg_error_getting_info_page_s,
                                                StringEscapeUtils.escapeXml10(e.getLocalizedMessage())),
                                        true);
                                return;
                            } catch (final PackageManager.NameNotFoundException |
                                           Resources.NotFoundException e) {
                                return;
                            }
                            final String strType = uri.getFragment();
                            if ("XML".equals(strType)) {
                                type = Source.Type.XML;
                            } else if ("HTML".equals(strType)) {
                                type = Source.Type.HTML;
                            } else {
                                type = Source.Type.PLAIN;
                            }
                        } else return;
                        switch (type) {
                            case XML:
                                wText.setHtmlText(getString(R.string.desc_rendering___));
                                wText.setXmlText(content, true);
                                break;
                            case HTML:
                                wText.setHtmlText(getString(R.string.desc_rendering___));
                                wText.setHtmlText(content, true);
                                break;
                            default:
                                wText.setTypeface(Typeface.MONOSPACE);
                                wText.setText(content);
                        }
                    } finally {
                        setContentView(view);
                    }
                });

    }
}
