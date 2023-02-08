package green_green_avk.anotherterm;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BackgroundProfile {
    @NonNull
    Drawable getDrawable() throws Exception;

    /**
     * Profiles should not have any presentation attributes but it is an exception.
     *
     * @return a desired preview image or {@code null} to use adapter's default
     */
    @Nullable
    default Drawable getPreviewDrawable() {
        return null;
    }

    /**
     * Profiles should not have any presentation attributes but it is an exception.
     *
     * @return a desired title or {@code null} to use adapter's default
     */
    @Nullable
    default CharSequence getTitle() {
        return null;
    }
}
