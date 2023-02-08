package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.ui.drawables.BackgroundDrawable;

public final class LocalBackgroundProfile implements BackgroundProfile {
    @NonNull
    final Context context;
    @DrawableRes
    final int drawableId;

    public LocalBackgroundProfile(@NonNull final Context context, final int drawableId) {
        this.context = context;
        this.drawableId = drawableId;
    }

    @Override
    @NonNull
    public Drawable getDrawable() {
        return new BackgroundDrawable(UiUtils.requireDrawable(context, drawableId)
                .mutate());
    }
}
