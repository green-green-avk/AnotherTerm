package green_green_avk.anotherterm.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.backends.BackendUiInteractionShellCtx;
import green_green_avk.anotherterm.utils.Misc;

// TODO: finish implementation

public final class BackendUiShell implements BackendUiInteraction, BackendUiInteractionShellCtx {
    private volatile InputStream stdIn = null;
    private volatile OutputStream stdOut = null;
    private volatile OutputStream stdErr = null;

    @Override
    public void erase(@NonNull final CharSequence v) {
    }

    private static void put(@Nullable final OutputStream stream, @NonNull final String message) {
        if (stream != null) {
            try {
                stream.write(Misc.toUTF8(message));
            } catch (final IOException ignored) {
            }
        }
    }

    @Override
    public boolean promptFields(@NonNull final List<CustomFieldOpts> fieldsOpts)
            throws InterruptedException {
        return false;
    }

    @Override
    @Nullable
    public CharSequence promptPassword(@NonNull final CharSequence message,
                                       @NonNull final List<CustomFieldOpts> extras)
            throws InterruptedException {
        return null;
    }

    @Override
    public boolean promptYesNo(@NonNull final CharSequence message) throws InterruptedException {
        return false;
    }

    @Override
    public void showMessage(@NonNull final CharSequence message) {
        put(stdErr, message + "\n");
    }

    @Override
    public void showToast(@NonNull final CharSequence message) {
        put(stdErr, message + "\n");
    }

    @Override
    @Nullable
    public byte[] promptContent(@NonNull final CharSequence message, @NonNull final String mimeType,
                                final long sizeLimit) throws InterruptedException, IOException {
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
