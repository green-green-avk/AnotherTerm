package green_green_avk.anotherterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.whatsnew.WhatsNewDialog;

public final class SessionsActivity extends AppCompatActivity {

    private void showEditFavoriteDlg(@Nullable final String name) {
        showEditFavoriteDlg(name, false);
    }

    private void showEditFavoriteDlg(@Nullable final String name, final boolean makeNew) {
        final Intent intent = new Intent(this, FavoriteEditorActivity.class);
        if (name != null) {
            intent.putExtra(C.IFK_MSG_NAME, name);
            intent.putExtra(C.IFK_MSG_NEW, makeNew);
        }
        startActivity(intent);
    }

    private void showAdapterDialog(@NonNull final PreferenceStorage ps,
                                   @NonNull final Map<String, Integer> list,
                                   @NonNull final Collection<? extends BackendModule.Meta.Requirement> requirements) {
        BackendAdapterDialog.show(this, ps, list, requirements);
    }

    private void prepareFavoritesList() {
        final RecyclerView l = findViewById(R.id.favorites_list);
        final FavoritesAdapter a = new FavoritesAdapter();
        l.setAdapter(a);
        a.setOnClickListener(view -> {
            final int pos = l.getChildAdapterPosition(view);
            if (pos < 0)
                return;
            final String name = a.getName(pos);
            final PreferenceStorage ps = FavoritesManager.get(name);
            final int key;
            ps.put("name", name); // Some mark
            try {
                final Object adapter = ps.get("adapter");
                if (adapter == null) {
                    final BackendModule.Meta meta =
                            ConsoleService.getBackendByParams(ps.get()).meta;
                    final Map<String, Integer> adaptersList = meta.getAdapters(this);
                    if (adaptersList != null) {
                        final Collection<BackendModule.Meta.Requirement> requirements =
                                BackendModule.Meta.unfulfilled(this,
                                        meta.getRequirements(this));
                        if (adaptersList.isEmpty() && requirements.isEmpty())
                            throw new BackendException(getString(
                                    R.string.msg_no_adapters_connected));
                        showAdapterDialog(ps, adaptersList, requirements);
                        return;
                    }
                }
                key = ConsoleService.startAnsiSession(this, ps.get());
            } catch (final ConsoleService.Exception | BackendException e) {
                Toast.makeText(this, e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            ConsoleActivity.showSession(this, key);
        });
        a.setOnCreateContextMenuListener((menu, view, menuInfo) -> {
            final int pos = l.getChildAdapterPosition(view);
            if (pos < 0)
                return;
            final String name = a.getName(pos);
            getMenuInflater().inflate(R.menu.menu_favorite, menu);
            menu.findItem(R.id.fav_edit).setOnMenuItemClickListener(item -> {
                showEditFavoriteDlg(name);
                return true;
            });
            menu.findItem(R.id.fav_delete).setOnMenuItemClickListener(item -> {
                FavoritesManager.remove(name);
                return true;
            });
            menu.findItem(R.id.fav_clone).setOnMenuItemClickListener(item -> {
                showEditFavoriteDlg(name, true);
                return true;
            });
            menu.findItem(R.id.fav_copy).setOnMenuItemClickListener(item -> {
                if (!FavoritesManager.contains(name))
                    return true;
                final PreferenceStorage ps = FavoritesManager.get(name);
                ps.put("name", name);
                try {
                    UiUtils.uriToClipboard(this, BackendsList.toUri(ps.get()),
                            getString(R.string.title_terminal_s_link_s, name,
                                    getString(R.string.linktype_connection_settings)));
                } catch (final IllegalStateException e) {
                    return true;
                }
                Toast.makeText(this, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT)
                        .show();
                return true;
            });
        });
        a.registerAdapterDataObserver(observer);
    }

    private void prepareSessionsList() {
        final RecyclerView l = findViewById(R.id.sessions_list);
        final SessionsAdapter a = new SessionsAdapter();
        l.setAdapter(a);
        a.setOnClickListener(view -> {
            final int pos = l.getChildAdapterPosition(view);
            if (pos < 0) return;
            final int key = a.getKey(pos);
            ConsoleActivity.showSession(this, key);
        });
        a.setOnCreateContextMenuListener((menu, view, menuInfo) -> {
            final int pos = l.getChildAdapterPosition(view);
            if (pos < 0)
                return;
            final int key = a.getKey(pos);
            getMenuInflater().inflate(R.menu.menu_session, menu);
            menu.findItem(R.id.action_terminate).setOnMenuItemClickListener(item -> {
                try {
                    ConsoleService.stopSession(key);
                } catch (final NoSuchElementException ignored) {
                }
                return true;
            });
            if (ConsoleService.hasAnsiSession(key)) {
                menu.findItem(R.id.action_toggle_wake_lock).setOnMenuItemClickListener(item -> {
                    final BackendModule be;
                    try {
                        be = ConsoleService.getAnsiSession(key).backend.wrapped;
                    } catch (final NoSuchElementException e) {
                        return true;
                    }
                    if (be.isWakeLockHeld()) be.releaseWakeLock();
                    else be.acquireWakeLock();
                    return true;
                });
            } else {
                menu.removeItem(R.id.action_toggle_wake_lock);
            }
        });
        a.registerAdapterDataObserver(observer);
    }

    private final RecyclerView.AdapterDataObserver observer =
            new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    final RecyclerView wFavs =
                            SessionsActivity.this.findViewById(R.id.favorites_list);
                    final View wWelcome = SessionsActivity.this.findViewById(R.id.welcome);
                    if (wFavs.getAdapter().getItemCount() +
                            SessionsActivity.this.<RecyclerView>findViewById(R.id.sessions_list)
                                    .getAdapter().getItemCount() > 0) {
                        wWelcome.setVisibility(View.GONE);
                        wFavs.setVisibility(View.VISIBLE);
                    } else {
                        wFavs.setVisibility(View.GONE);
                        wWelcome.setVisibility(View.VISIBLE);
                    }
                }
            };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sessions_activity);
        prepareFavoritesList();
        prepareSessionsList();
        observer.onChanged();
        WhatsNewDialog.showUnseen(this);
        if (savedInstanceState == null)
            UiUtils.checkStartPermissions(this);
    }

    @Override
    protected void onDestroy() {
        this.<RecyclerView>findViewById(R.id.favorites_list)
                .getAdapter().unregisterAdapterDataObserver(observer);
        this.<RecyclerView>findViewById(R.id.sessions_list)
                .getAdapter().unregisterAdapterDataObserver(observer);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.<RecyclerView>findViewById(R.id.sessions_list).getAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sessions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_fav: {
                showEditFavoriteDlg(null);
                return true;
            }
            case R.id.action_scratchpad: {
                startActivity(new Intent(this, ScratchpadActivity.class));
                return true;
            }
            case R.id.action_term_color_mapping: {
                startActivity(new Intent(this, AnsiColorManagerActivity.class));
                return true;
            }
            case R.id.action_term_key_mapping: {
                startActivity(new Intent(this, TermKeyMapManagerActivity.class));
                return true;
            }
            case R.id.action_ssh_keys_settings: {
                startActivity(new Intent(this, SshKeysSettingsActivity.class));
                return true;
            }
            case R.id.action_saved_passwords_settings: {
                startActivity(new Intent(this, PasswordManagementActivity.class));
                return true;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            case R.id.action_help: {
                startActivity(new Intent(this, InfoActivity.class)
                        .setData(Uri.parse("info://local/help")));
                return true;
            }
            case R.id.action_about: {
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            }
        }
        return true;
    }

    @Keep
    public void onNewFav(final View view) {
        showEditFavoriteDlg(null);
    }
}
