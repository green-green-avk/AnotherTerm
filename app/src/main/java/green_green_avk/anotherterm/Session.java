package green_green_avk.anotherterm;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.ConsoleScreenView;

public final class Session {

    public static final class UiState {
        final ConsoleScreenView.State csv = new ConsoleScreenView.State();
    }

    @NonNull
    public final Map<String, ?> connectionParams;
    @NonNull
    public final ConsoleInput input;
    @NonNull
    public final ConsoleOutput output;
    @NonNull
    public final EventBasedBackendModuleWrapper backend;
    @Nullable
    public Bitmap thumbnail = null;

    public final UiState uiState = new UiState();

    public Session(@NonNull final Map<String, ?> cp, @NonNull final ConsoleInput ci,
                   @NonNull final ConsoleOutput co,
                   @NonNull final EventBasedBackendModuleWrapper be) {
        connectionParams = cp;
        input = ci;
        output = co;
        backend = be;
    }
}
