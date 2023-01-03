package green_green_avk.anotherterm;

import android.content.Context;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerUi;

public final class TermKeyMapManagerUi extends ProfileManagerUi<TermKeyMapRules> {
    private TermKeyMapManagerUi() {
    }

    public static final TermKeyMapManagerUi instance = new TermKeyMapManagerUi();

    @Override
    @NonNull
    public TermKeyMapManager getManager(@NonNull final Context ctx) {
        return TermKeyMapManager.instance;
    }

    @Override
    @NonNull
    public TermKeyMapAdapter createAdapter(@NonNull final Context ctx) {
        return new TermKeyMapAdapter(ctx);
    }

    @Override
    public void startEditor(@NonNull final Context ctx) {
        TermKeyMapEditorActivity.start(ctx, null);
    }
}
