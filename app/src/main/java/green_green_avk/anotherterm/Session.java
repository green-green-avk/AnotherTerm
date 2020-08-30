package green_green_avk.anotherterm;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.ConsoleKeyboardView;
import green_green_avk.anotherterm.ui.ConsoleScreenView;

public final class Session {

    public static final class UiState {
        public final ConsoleScreenView.State csv = new ConsoleScreenView.State();
        public final ConsoleKeyboardView.State ckv = new ConsoleKeyboardView.State();
        public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        public boolean keepScreenOn = false;
    }

    public static final class Properties {
        public boolean terminateOnDisconnect = false;
    }

    @NonNull
    public final Map<String, ?> connectionParams;
    @NonNull
    public final ConsoleInput input;
    @NonNull
    public final ConsoleOutput output;
    @NonNull
    public final EventBasedBackendModuleWrapper backend;
    @NonNull
    public final Properties properties;
    @Nullable
    public Bitmap thumbnail = null;

    public final UiState uiState = new UiState();

    public Session(@NonNull final Map<String, ?> cp,
                   @NonNull final ConsoleInput ci,
                   @NonNull final ConsoleOutput co,
                   @NonNull final EventBasedBackendModuleWrapper be,
                   @NonNull final Properties pp) {
        connectionParams = cp;
        input = ci;
        output = co;
        backend = be;
        properties = pp;
    }
}
