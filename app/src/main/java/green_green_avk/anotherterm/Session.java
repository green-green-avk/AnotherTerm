package green_green_avk.anotherterm;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Session {
    @Nullable
    public Bitmap thumbnail = null;

    @NonNull
    public abstract CharSequence getTitle();
}
