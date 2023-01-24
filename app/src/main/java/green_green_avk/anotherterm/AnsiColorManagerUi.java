package green_green_avk.anotherterm;

import android.content.Context;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerUi;
import green_green_avk.anotherterm.utils.Misc;

public final class AnsiColorManagerUi extends ProfileManagerUi<AnsiColorProfile> {
    private AnsiColorManagerUi() {
    }

    @Override
    @NonNull
    public AnsiColorManager getManager(@NonNull final Context ctx) {
        return Misc.getApplication(ctx).ansiColorManager;
    }

    @Override
    @NonNull
    public AnsiColorAdapter createAdapter(@NonNull final Context ctx) {
        return new AnsiColorAdapter(ctx);
    }

    @Override
    public void startEditor(@NonNull final Context ctx) {
        AnsiColorEditorActivity.start(ctx, null);
    }

    public static final AnsiColorManagerUi instance = new AnsiColorManagerUi();
}
