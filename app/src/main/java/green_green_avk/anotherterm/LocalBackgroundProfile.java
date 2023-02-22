package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.ui.drawables.BackgroundDrawable;

public final class LocalBackgroundProfile implements BackgroundProfile {
    @DrawableRes
    private final int drawableId;

    public LocalBackgroundProfile(final int drawableId) {
        this.drawableId = drawableId;
    }

    @Override
    @NonNull
    public Drawable getDrawable(@NonNull final Context ctx) {
        return new BackgroundDrawable(UiUtils.requireDrawable(ctx, drawableId).mutate());
    }
}
