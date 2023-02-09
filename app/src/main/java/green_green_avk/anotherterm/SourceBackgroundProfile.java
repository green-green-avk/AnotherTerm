package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;

import green_green_avk.anotherterm.ui.drawables.BackgroundDrawable;

public final class SourceBackgroundProfile implements BackgroundProfile {
    @NonNull
    final Function<? super Context, ? extends Drawable> provider;

    public SourceBackgroundProfile(@NonNull final Function<? super Context, ? extends Drawable> provider) {
        this.provider = provider;
    }

    @Override
    @NonNull
    public Drawable getDrawable(@NonNull final Context ctx) throws Exception {
        return new BackgroundDrawable(provider.apply(ctx).mutate());
    }
}
