package green_green_avk.anotherterm.backends;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface BackendUiInteraction {
    @Nullable
    String promptPassword(@NonNull String message) throws InterruptedException;

    boolean promptYesNo(@NonNull String message) throws InterruptedException;

    void showMessage(@NonNull String message);

    void showToast(@NonNull String message);

    byte[] promptContent(@NonNull String message, @NonNull String mimeType) throws InterruptedException;

    boolean promptPermissions(@NonNull String[] perms) throws InterruptedException;
}
