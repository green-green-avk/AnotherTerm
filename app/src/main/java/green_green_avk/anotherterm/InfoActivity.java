package green_green_avk.anotherterm;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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
        res.put("/keymap_escapes", new Source(R.string.desc_keymap_escapes, Source.Type.XML));
        res.put("/scratchpad", new Source(R.string.desc_scratchpad_help, Source.Type.XML));
        res.put("/share_input", new Source(R.string.desc_share_input_help, Source.Type.XML));
        res.put("/fav_token", new Source(R.string.desc_fav_token_help, Source.Type.XML));
        res.put("/shell_env_man", new Source(R.string.desc_shell_env_help, Source.Type.XML));
        res.put("/termsh_man", new Source(R.string.desc_termsh_help, Source.Type.XML));
        res.put("/help", new Source(R.string.desc_main_help, Source.Type.XML));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        new AsyncLayoutInflater(this).inflate(R.layout.info_activity,
                (ViewGroup) getWindow().getDecorView().getRootView(),
                new AsyncLayoutInflater.OnInflateFinishedListener() {
                    @Override
                    public void onInflateFinished(@NonNull final View view, final int i,
                                                  @Nullable final ViewGroup viewGroup) {
                        try {
                            final HtmlTextView v = view.findViewById(R.id.desc);
                            final Uri uri = getIntent().getData();
                            if (uri == null) return;
                            final String content;
                            final Source.Type type;
                            if ("info".equals(uri.getScheme())) {
                                final Source source = res.get(uri.getPath());
                                if (source == null) return;
                                content = getString(source.id);
                                type = source.type;
                            } else if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())) {
                                try {
                                    content = InfoActivity.this.getPackageManager()
                                            .getResourcesForApplication(uri.getAuthority())
                                            .getString(Integer.parseInt(uri.getLastPathSegment()));
                                } catch (final NumberFormatException e) {
                                    v.setXmlText(getString(R.string.msg_error_getting_info_page_s,
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
                                    v.setHtmlText(getString(R.string.desc_rendering___));
                                    v.setXmlText(content, true);
                                    break;
                                case HTML:
                                    v.setHtmlText(getString(R.string.desc_rendering___));
                                    v.setHtmlText(content, true);
                                    break;
                                default:
                                    v.setTypeface(Typeface.MONOSPACE);
                                    v.setText(content);
                            }
                        } finally {
                            setContentView(view);
                        }
                    }
                });

    }
}
