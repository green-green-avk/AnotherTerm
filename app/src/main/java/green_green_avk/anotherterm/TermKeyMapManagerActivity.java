package green_green_avk.anotherterm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public final class TermKeyMapManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term_key_map_manager);
        final ListView l = findViewById(R.id.list);
        final TermKeyMapAdapter a = new TermKeyMapAdapter(getApplicationContext());
        l.setAdapter(a);
        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = a.getName(l.getPositionForView(v));
                startActivity(new Intent(TermKeyMapManagerActivity.this,
                        TermKeyMapEditorActivity.class).putExtra(C.IFK_MSG_NAME, name));
            }
        });
        a.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                final String name = a.getName(l.getPositionForView(v));
                menu.add(R.string.action_delete).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        TermKeyMapManager.remove(name);
                        return true;
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_term_key_map_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                startActivity(new Intent(this, TermKeyMapEditorActivity.class));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
