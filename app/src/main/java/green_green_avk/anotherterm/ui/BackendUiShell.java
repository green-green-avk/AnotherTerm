package green_green_avk.anotherterm.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.backends.BackendUiInteractionShellCtx;
import green_green_avk.anotherterm.utils.Misc;

// TODO: finish implementation

public final class BackendUiShell implements BackendUiInteraction, BackendUiInteractionShellCtx {
    private volatile InputStream stdIn = null;
    private volatile OutputStream stdOut = null;
    private volatile OutputStream stdErr = null;

    private static void put(@Nullable final OutputStream stream, @NonNull final String message) {
        if (stream != null) {
            try {
                stream.write(Misc.toUTF8(message));
            } catch (final IOException ignored) {
            }
        }
    }

    @Nullable
    @Override
    public String promptPassword(@NonNull final String message) throws InterruptedException {
        return null;
    }

    @Override
    public boolean promptYesNo(@NonNull final String message) throws InterruptedException {
        return false;
    }

    @Override
    public void showMessage(@NonNull final String message) {
        put(stdErr, message + "\n");
    }

    @Override
    public void showToast(@NonNull final String message) {
        put(stdErr, message + "\n");
    }

    @Override
    public byte[] promptContent(@NonNull final String message, @NonNull String mimeType)
            throws InterruptedException {
        return new byte[0];
    }

    @Override
    public boolean promptPermissions(@NonNull final String[] perms) throws InterruptedException {
        return false;
    }

    @Override
    public void setIO(@Nullable final InputStream stdIn,
                      @Nullable final OutputStream stdOut,
                      @Nullable final OutputStream stdErr) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }
}
