package green_green_avk.anotherterm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;
import java.util.Map;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.utils.PreferenceStorage;

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

    private void showSession(final int key) {
        startActivity(new Intent(this, ConsoleActivity.class)
                .putExtra(C.IFK_MSG_SESS_KEY, key));
    }

    private void showSession(final boolean fromTail) {
        startActivity(new Intent(this, ConsoleActivity.class)
                .putExtra(C.IFK_MSG_SESS_TAIL, fromTail));
    }

    private void showAdapterDialog(@NonNull final PreferenceStorage ps,
                                   @NonNull final Map<String, Integer> list) {
        final String[] ii = new String[list.size()];
        final boolean[] iib = new boolean[list.size()];
        int i = 0;
        for (final Map.Entry<String, Integer> ent : list.entrySet()) {
            ii[i] = (ent.getValue() == BackendModule.Meta.ADAPTER_READY ? ent.getKey() :
                    String.format(Locale.getDefault(), "%s [busy]", ent.getKey()));
            iib[i] = ent.getValue() != BackendModule.Meta.ADAPTER_READY;
            i++;
        }
        new AlertDialog.Builder(this).setItems(ii, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                if (iib[which]) return;
                ps.put("adapter", ii[which].split(" ", 2)[0]);
                final int key;
                try {
                    key = ConsoleService.startSession(SessionsActivity.this, ps.get());
                } catch (final ConsoleService.Exception | BackendException e) {
                    Toast.makeText(SessionsActivity.this, e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }
                showSession(key);
                dialog.dismiss();
            }
        }).setCancelable(true).show();
    }

    private void prepareFavoritesList() {
        final RecyclerView l = findViewById(R.id.favorites_list);
        l.setLayoutManager(new LinearLayoutManager(this));
        final FavoritesAdapter a = new FavoritesAdapter();
        l.setAdapter(a);
        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final String name = a.getName(l.getChildAdapterPosition(view));
                final PreferenceStorage ps = FavoritesManager.get(name);
                final int key;
                ps.put("name", name); // Some mark
                try {
                    final Object adapter = ps.get("adapter");
                    if (adapter == null) {
                        final Map<String, Integer> adaptersList = ConsoleService.getBackendByParams(
                                ps.get()).meta.getAdapters(SessionsActivity.this);
                        if (adaptersList != null) {
                            if (adaptersList.isEmpty())
                                throw new BackendException(getString(
                                        R.string.msg_no_adapters_connected));
                            showAdapterDialog(ps, adaptersList);
                            return;
                        }
                    }
                    key = ConsoleService.startSession(SessionsActivity.this, ps.get());
                } catch (final ConsoleService.Exception | BackendException e) {
                    Toast.makeText(SessionsActivity.this, e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                showSession(key);
            }
        });
        a.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(final ContextMenu menu, final View view,
                                            final ContextMenu.ContextMenuInfo menuInfo) {
                final String name = a.getName(l.getChildLayoutPosition(view));
                getMenuInflater().inflate(R.menu.menu_favorite, menu);
                menu.findItem(R.id.fav_edit).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                showEditFavoriteDlg(name);
                                return true;
                            }
                        });
                menu.findItem(R.id.fav_delete).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                FavoritesManager.remove(name);
                                return true;
                            }
                        });
                menu.findItem(R.id.fav_clone).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                showEditFavoriteDlg(name, true);
                                return true;
                            }
                        });
            }
        });
    }

    private void prepareSessionsList() {
        final RecyclerView l = findViewById(R.id.sessions_list);
        l.setLayoutManager(new LinearLayoutManager(this));
        final SessionsAdapter a = new SessionsAdapter();
        l.setAdapter(a);
        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final int key = a.getKey(l.getChildAdapterPosition(v));
                showSession(key);
            }
        });
        a.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(final ContextMenu menu, final View view,
                                            final ContextMenu.ContextMenuInfo menuInfo) {
                getMenuInflater().inflate(R.menu.menu_session, menu);
                menu.findItem(R.id.action_terminate).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                // TODO: Possibly redundant precautions
                                final int key;
                                try {
                                    key = a.getKey(l.getChildLayoutPosition(view));
                                } catch (final IndexOutOfBoundsException e) {
                                    a.notifyDataSetChanged();
                                    return true;
                                }
                                ConsoleService.stopSession(key);
                                return true;
                            }
                        });
                menu.findItem(R.id.action_toggle_wake_lock).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                // TODO: Possibly redundant precautions
                                final int key;
                                try {
                                    key = a.getKey(l.getChildLayoutPosition(view));
                                } catch (final IndexOutOfBoundsException e) {
                                    a.notifyDataSetChanged();
                                    return true;
                                }
                                final BackendModule be =
                                        ConsoleService.getSession(key).backend.wrapped;
                                if (be.isWakeLockHeld()) be.releaseWakeLock();
                                else be.acquireWakeLock();
                                return true;
                            }
                        });
            }
        });
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sessions_activity);

        prepareFavoritesList();
        prepareSessionsList();
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
            case R.id.action_term_key_mapping: {
                startActivity(new Intent(this, TermKeyMapManagerActivity.class));
                return true;
            }
            case R.id.action_ssh_keys_settings: {
                startActivity(new Intent(this, SshKeysSettingsActivity.class));
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

    @Override
    protected void onResume() {
        super.onResume();
        this.<RecyclerView>findViewById(R.id.sessions_list).getAdapter().notifyDataSetChanged();
    }
}
