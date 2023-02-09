package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BackgroundProfile {
    /**
     * @param ctx to get for
     * @return a background
     * @throws Exception on any failure
     */
    @NonNull
    Drawable getDrawable(@NonNull Context ctx) throws Exception;

    /**
     * Profiles should not have any presentation attributes but it is an exception.
     *
     * @param ctx to get for
     * @return a desired preview image or {@code null} to use adapter's default
     */
    @Nullable
    default Drawable getPreviewDrawable(@NonNull final Context ctx) {
        return null;
    }

    /**
     * Profiles should not have any presentation attributes but it is an exception.
     *
     * @param ctx to get for
     * @return a desired title or {@code null} to use adapter's default
     */
    @Nullable
    default CharSequence getTitle(@NonNull final Context ctx) {
        return null;
    }
}
