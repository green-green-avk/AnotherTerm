package green_green_avk.anotherterm;

import android.content.Context;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerUi;
import green_green_avk.anotherterm.utils.Misc;

public final class BackgroundsManagerUi extends ProfileManagerUi<BackgroundProfile> {
    private BackgroundsManagerUi() {
    }

    @Override
    @NonNull
    public BackgroundsManager getManager(@NonNull final Context ctx) {
        return Misc.getApplication(ctx).backgroundDrawableManager;
    }

    @Override
    @NonNull
    public BackgroundsAdapter createAdapter(@NonNull final Context ctx) {
        return new BackgroundsAdapter(ctx);
    }

    @Override
    public void startEditor(@NonNull final Context ctx) {
        // Not editable
    }

    public static final BackgroundsManagerUi instance = new BackgroundsManagerUi();
}
