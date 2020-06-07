package green_green_avk.anotherterm;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public final class TermKeyMapManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term_key_map_manager);
        final ListView l = findViewById(R.id.list);
        final TermKeyMapAdapter a = new TermKeyMapAdapter(getApplicationContext());
        l.setAdapter(a);
        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String name = a.getName(l.getPositionForView(v));
                TermKeyMapEditorActivity.start(TermKeyMapManagerActivity.this, name);
            }
        });
        a.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(final ContextMenu menu, final View v,
                                            final ContextMenu.ContextMenuInfo menuInfo) {
                final String name = a.getName(l.getPositionForView(v));
                menu.add(R.string.action_delete).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                TermKeyMapManager.remove(name);
                                return true;
                            }
                        });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_term_key_map_manager, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                startActivity(new Intent(this, TermKeyMapEditorActivity.class));
                return true;
            }
        }
        return true;
    }
}
