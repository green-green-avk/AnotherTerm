package green_green_avk.anotherterm.backends;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public interface BackendUiInteraction {
    @Nullable
    String promptPassword(@NonNull String message) throws InterruptedException;

    boolean promptYesNo(@NonNull String message) throws InterruptedException;

    void showMessage(@NonNull String message);

    void showToast(@NonNull String message);

    @Nullable
    byte[] promptContent(@NonNull String message, @NonNull String mimeType, long sizeLimit)
            throws InterruptedException, IOException;

    boolean promptPermissions(@NonNull String[] perms) throws InterruptedException;
}
