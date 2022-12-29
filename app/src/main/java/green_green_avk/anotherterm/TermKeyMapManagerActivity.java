package green_green_avk.anotherterm;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public final class TermKeyMapManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.term_key_map_manager_activity);
        final ListView l = findViewById(R.id.list);
        final TermKeyMapAdapter a = new TermKeyMapAdapter(getApplicationContext());
        l.setAdapter(a);
        a.setOnClickListener(v -> {
            final String name = a.getName(l.getPositionForView(v));
            TermKeyMapEditorActivity.start(this, name);
        });
        a.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            final String name = a.getName(l.getPositionForView(v));
            menu.add(R.string.action_delete).setOnMenuItemClickListener(
                    item -> {
                        TermKeyMapManager.remove(name);
                        return true;
                    });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_term_key_map_manager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                startActivity(new Intent(this, TermKeyMapEditorActivity.class));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
