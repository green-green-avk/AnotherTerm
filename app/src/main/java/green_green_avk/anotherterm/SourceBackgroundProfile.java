package green_green_avk.anotherterm;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

import green_green_avk.anotherterm.ui.drawables.BackgroundDrawable;

public final class SourceBackgroundProfile implements BackgroundProfile {
    @NonNull
    final Callable<? extends Drawable> drawableCallable;

    public SourceBackgroundProfile(@NonNull final Callable<? extends Drawable> drawableCallable) {
        this.drawableCallable = drawableCallable;
    }

    @Override
    @NonNull
    public Drawable getDrawable() throws Exception {
        return new BackgroundDrawable(drawableCallable.call().mutate());
    }
}
