package green_green_avk.anotherterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayInputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.utils.HtmlUtils;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.anotherterm.utils.PreferenceStorage;

public final class ShareInputActivity extends AppCompatActivity {

    // 32 pages minimum per the whole process env... Good enough.
    private final static int ENV_VAR_MAX = 4096;

    private void putIfSet(@NonNull final PreferenceStorage ps,
                          @NonNull final String name, @Nullable final String value,
                          @NonNull final String mime) {
        if (value != null) {
            if (value.length() > ENV_VAR_MAX / 2) {
                final byte[] textBytes = Misc.toUTF8(value);
                final Uri uri = StreamProvider.obtainUri(
                        new ByteArrayInputStream(textBytes), mime,
                        getString(R.string.name_stream_default), textBytes.length,
                        null);
                ps.put(name + "_uri", uri.toString());
            } else {
                ps.put(name, value);
            }
        }
    }

    private static void putIfSet(@NonNull final PreferenceStorage ps,
                                 @NonNull final String name, @Nullable final String[] value) {
        if (value != null) ps.put(name, TextUtils.join(" ", value));
    }

    private static void putIfSet(@NonNull final PreferenceStorage ps,
                                 @NonNull final String name, @Nullable final String value) {
        if (value != null) ps.put(name, value);
    }

    private void showSession(final int key) {
        startActivity(new Intent(this, ConsoleActivity.class)
                .putExtra(C.IFK_MSG_SESS_KEY, key));
    }

    private void fillSendArgs(@NonNull final PreferenceStorage ps) {
        final ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        putIfSet(ps, "$input.action", getIntent().getAction());
        putIfSet(ps, "$input.mime", intentReader.getType());
        putIfSet(ps, "$input.email_to", intentReader.getEmailTo());
        putIfSet(ps, "$input.email_cc", intentReader.getEmailCc());
        putIfSet(ps, "$input.email_bcc", intentReader.getEmailBcc());
        putIfSet(ps, "$input.subject", intentReader.getSubject(), "text/plain");
        final String text = intentReader.getHtmlText();
        if (text != null) {
            putIfSet(ps, "$input.html", text, "text/html");
        } else { // ShareCompat can't... :(
            final Intent intent = getIntent();
            Iterable<String> htmlTexts =
                    intent.getStringArrayListExtra(IntentCompat.EXTRA_HTML_TEXT);
            if (htmlTexts == null) {
                final ArrayList<CharSequence> texts =
                        intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT);
                if (texts != null) {
                    htmlTexts = () -> new Iterator<String>() {
                        private final Iterator<CharSequence> it = texts.iterator();

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public String next() {
                            return HtmlUtils.toHtml(it.next());
                        }
                    };
                }
            }
            if (htmlTexts != null) {
                int i = 1;
                for (final String t : htmlTexts) {
                    putIfSet(ps, "$input.html" + (i == 1 ? "" : i), t, "text/html");
                    i++;
                }
            }
        }
        if (intentReader.getStreamCount() > 0)
            ps.put("$input.uris", TextUtils.join(" ", new AbstractList<Uri>() {
                @Override
                public Uri get(final int index) {
                    return intentReader.getStream(index);
                }

                @Override
                public int size() {
                    return intentReader.getStreamCount();
                }
            }));
    }

    private void fillOpenArgs(@NonNull final PreferenceStorage ps) {
        final Intent intent = getIntent();
        final Uri data = intent.getData();
        if (data == null) return;
        ps.put("$input.uri", data.toString());
        putIfSet(ps, "$input.mime", intent.getType());
        putIfSet(ps, "$input.action", intent.getAction());
    }

    private void prepareFavoritesList() {
        final RecyclerView l = findViewById(R.id.favorites_list);
        l.setLayoutManager(new LinearLayoutManager(this));
        final FavoritesAdapter a = new FavoritesAdapter(true);
        l.setAdapter(a);
        a.setOnClickListener(view -> {
            final String name = a.getName(l.getChildAdapterPosition(view));
            final PreferenceStorage ps = FavoritesManager.get(name);
            final int key;
            ps.put("name", name); // Some mark
            final String action = getIntent().getAction();
            if (Intent.ACTION_SEND.equals(action) ||
                    Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                fillSendArgs(ps);
            } else {
                fillOpenArgs(ps);
            }
            try {
                key = ConsoleService.startSession(ShareInputActivity.this, ps.get());
            } catch (final ConsoleService.Exception | BackendException e) {
                Toast.makeText(ShareInputActivity.this, e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            showSession(key);
            finish();
        });
        a.registerAdapterDataObserver(observer);
    }

    private final RecyclerView.AdapterDataObserver observer =
            new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    final RecyclerView wFavs =
                            ShareInputActivity.this.findViewById(R.id.favorites_list);
                    final View empty = ShareInputActivity.this.findViewById(R.id.empty);
                    if (wFavs.getAdapter().getItemCount() > 0) {
                        empty.setVisibility(View.GONE);
                    } else {
                        empty.setVisibility(View.VISIBLE);
                    }
                }
            };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_input_activity);
        prepareFavoritesList();
        observer.onChanged();
        NewsDialog.showUnseen(this);
    }

    @Override
    protected void onDestroy() {
        this.<RecyclerView>findViewById(R.id.favorites_list)
                .getAdapter().unregisterAdapterDataObserver(observer);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share_input, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onInfo(final MenuItem menuItem) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/share_input")));
    }
}
