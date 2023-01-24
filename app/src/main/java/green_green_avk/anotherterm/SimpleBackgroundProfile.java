package green_green_avk.anotherterm;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class SimpleBackgroundProfile implements BackgroundProfile {
    @NonNull
    private final Drawable sourceDrawable;

    public SimpleBackgroundProfile(@NonNull final Drawable drawable) {
        sourceDrawable = drawable;
    }

    @Override
    @NonNull
    public Drawable getDrawable() {
        return sourceDrawable;
    }
}
