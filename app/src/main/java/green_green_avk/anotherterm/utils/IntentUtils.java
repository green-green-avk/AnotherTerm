package green_green_avk.anotherterm.utils;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class IntentUtils {
    private IntentUtils() {
    }

    public static void putExtraIfSet(@NonNull final Intent intent,
                                     @NonNull final String key,
                                     @Nullable final String value) {
        if (value != null) intent.putExtra(key, value);
    }

    public static void putSpaceListExtraIfSet(@NonNull final Intent intent,
                                              @NonNull final String key,
                                              @Nullable final String value) {
        if (value != null) intent.putExtra(key, value.trim().split("\\s+"));
    }
}
