package green_green_avk.anotherterm.backends;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public interface BackendUiInteraction {
    /**
     * Erases sensitive data returned by {@code prompt*()} calls.
     *
     * @param v data to erase
     */
    void erase(@NonNull CharSequence v);

    @Nullable
    CharSequence promptPassword(@NonNull CharSequence message) throws InterruptedException;

    boolean promptYesNo(@NonNull CharSequence message) throws InterruptedException;

    void showMessage(@NonNull CharSequence message);

    void showToast(@NonNull CharSequence message);

    @Nullable
    byte[] promptContent(@NonNull CharSequence message, @NonNull String mimeType, long sizeLimit)
            throws InterruptedException, IOException;

    boolean promptPermissions(@NonNull String[] perms) throws InterruptedException;
}
