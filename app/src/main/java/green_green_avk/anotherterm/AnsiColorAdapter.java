package green_green_avk.anotherterm;

import android.content.Context;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import green_green_avk.anotherterm.ui.ProfileAdapter;
import green_green_avk.anotherterm.ui.UniAdapter;

public final class AnsiColorAdapter extends ProfileAdapter<AnsiColorProfile> {
    public AnsiColorAdapter(@NonNull final Context context) {
        super(context);
    }

    @Override
    @NonNull
    protected AnsiColorManager getManager() {
        return AnsiColorManagerUi.instance.getManager(context);
    }

    @Override
    public void startEditor(@Nullable final String name) {
        AnsiColorEditorActivity.start(context, name);
    }

    @NonNull
    public static AnsiColorAdapter getSelf(@Nullable final Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }

    @NonNull
    public static AnsiColorAdapter getSelf(@Nullable final RecyclerView.Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }
}
