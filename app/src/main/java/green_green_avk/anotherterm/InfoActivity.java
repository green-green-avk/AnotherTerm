package green_green_avk.anotherterm;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.AsyncLayoutInflater;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

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
        res.put("/keymap_escapes", new Source(R.string.desc_keymap_escapes, Source.Type.XML));
        res.put("/shell_env_man", new Source(R.string.desc_shell_env_help, Source.Type.XML));
        res.put("/termsh_man", new Source(R.string.desc_termsh_help, Source.Type.XML));
        res.put("/help", new Source(R.string.desc_main_help, Source.Type.XML));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        new AsyncLayoutInflater(this).inflate(R.layout.activity_info,
                (ViewGroup) getWindow().getDecorView().getRootView(),
                new AsyncLayoutInflater.OnInflateFinishedListener() {
                    @Override
                    public void onInflateFinished(@NonNull final View view, final int i,
                                                  @Nullable final ViewGroup viewGroup) {
                        final HtmlTextView v = view.findViewById(R.id.desc);
                        final Uri uri = getIntent().getData();
                        if (uri != null) {
                            if ("info".equals(uri.getScheme())) {
                                final Source source = res.get(uri.getPath());
                                if (source == null) return;
                                switch (source.type) {
                                    case XML:
                                        v.setHtmlText(getString(R.string.desc_loading___));
                                        v.setXmlText(getString(source.id), true);
                                        break;
                                    case HTML:
                                        v.setHtmlText(getString(R.string.desc_loading___));
                                        v.setHtmlText(getString(source.id), true);
                                        break;
                                    default:
                                        v.setTypeface(Typeface.MONOSPACE);
                                        v.setText(source.id);
                                }
                            }
                        }
                        setContentView(view);
                    }
                });

    }
}
