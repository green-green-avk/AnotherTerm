package green_green_avk.anotherterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.utils.PreferenceStorage;

public final class SessionsActivity extends AppCompatActivity {

    private void showEditFavoriteDlg(final String name) {
        showEditFavoriteDlg(name, false);
    }

    private void showEditFavoriteDlg(final String name, final boolean makeNew) {
        final Intent intent = new Intent(this, FavoriteEditorActivity.class);
        if (name != null) {
            intent.putExtra(C.IFK_MSG_NAME, name);
            intent.putExtra(C.IFK_MSG_NEW, makeNew);
        }
        startActivity(intent);
    }

    private void showSession(final int key) {
        startActivity(new Intent(this, ConsoleActivity.class).putExtra(C.IFK_MSG_SESS_KEY, key));
    }

    private void showSession(final boolean fromTail) {
        startActivity(new Intent(this, ConsoleActivity.class).putExtra(C.IFK_MSG_SESS_TAIL, fromTail));
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
                int key;
                try {
                    ps.put("name", name); // Some mark
                    key = ConsoleService.startSession(SessionsActivity.this, ps.get());
                } catch (final ConsoleService.Exception e) {
                    Toast.makeText(SessionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                } catch (final BackendException e) {
                    Toast.makeText(SessionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                menu.findItem(R.id.fav_edit).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showEditFavoriteDlg(name);
                        return true;
                    }
                });
                menu.findItem(R.id.fav_delete).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        FavoritesManager.remove(name);
                        return true;
                    }
                });
                menu.findItem(R.id.fav_clone).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
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
            public void onClick(View v) {
                final int key = a.getKey(l.getChildAdapterPosition(v));
                showSession(key);
            }
        });
        a.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                final int key = a.getKey(l.getChildLayoutPosition(view));
                getMenuInflater().inflate(R.menu.menu_session, menu);
                menu.findItem(R.id.action_terminate).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ConsoleService.stopSession(key);
                        return true;
                    }
                });
            }
        });
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sessions);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        prepareFavoritesList();
        prepareSessionsList();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sessions, menu);
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((RecyclerView) findViewById(R.id.sessions_list)).getAdapter().notifyDataSetChanged();
    }
}
