package green_green_avk.anotherterm;

import android.content.Context;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import green_green_avk.anotherterm.ui.ProfileAdapter;
import green_green_avk.anotherterm.ui.UniAdapter;

public final class TermKeyMapAdapter extends ProfileAdapter<TermKeyMapRules> {
    public TermKeyMapAdapter(@NonNull final Context context) {
        super(context);
    }

    @Override
    @NonNull
    protected TermKeyMapManager getManager() {
        return TermKeyMapManager.instance;
    }

    @Override
    public void startEditor(@Nullable final String name) {
        TermKeyMapEditorActivity.start(context, name);
    }

    @NonNull
    public static TermKeyMapAdapter getSelf(@Nullable final Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }

    @NonNull
    public static TermKeyMapAdapter getSelf(@Nullable final RecyclerView.Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }
}
