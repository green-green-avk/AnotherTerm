package green_green_avk.anotherterm.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import green_green_avk.anotherterm.R;

public abstract class ProfileManagerActivity<T> extends AppCompatActivity {
    @NonNull
    protected abstract ProfileManagerUi<T> getUi();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_manager_activity);
        final RecyclerView l = findViewById(R.id.list);
        final ProfileAdapter<T> a = getUi().createAdapter(this);
        l.setAdapter(a.getRecyclerAdapter());
        a.setOnClickListener(meta -> a.startEditor(meta.name));
        a.setOnCreateContextMenuListener((menu, meta, menuInfo) ->
                menu.add(R.string.action_delete).setOnMenuItemClickListener(item -> {
                    getUi().getManager(this).remove(meta.name);
                    return true;
                })
        );
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile_manager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                getUi().startEditor(this);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
