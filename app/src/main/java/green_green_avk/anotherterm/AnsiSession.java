package green_green_avk.anotherterm;

import android.content.pm.ActivityInfo;

import androidx.annotation.NonNull;

import java.util.Map;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.AnsiConsoleKeyboardView;
import green_green_avk.anotherterm.ui.ConsoleScreenView;

public final class AnsiSession extends Session {

    public static final class UiState {
        public final ConsoleScreenView.State csv = new ConsoleScreenView.State();
        public final AnsiConsoleKeyboardView.State ckv = new AnsiConsoleKeyboardView.State();
        public float fontSizeDp = 0F; // Invariant for resolution switching
        public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        public boolean keepScreenOn = false;
    }

    public static final class Properties {
        public boolean terminateOnDisconnect = false;
    }

    @NonNull
    public final Map<String, ?> connectionParams;
    @NonNull
    public final AnsiConsoleInput input;
    @NonNull
    public final AnsiConsoleOutput output;
    @NonNull
    public final EventBasedBackendModuleWrapper backend;
    @NonNull
    public final Properties properties;

    public final UiState uiState = new UiState();

    public AnsiSession(@NonNull final Map<String, ?> cp,
                       @NonNull final AnsiConsoleInput ci,
                       @NonNull final AnsiConsoleOutput co,
                       @NonNull final EventBasedBackendModuleWrapper be,
                       @NonNull final Properties pp) {
        connectionParams = cp;
        input = ci;
        output = co;
        backend = be;
        properties = pp;
    }

    @Override
    @NonNull
    public CharSequence getTitle() {
        String r = input.currScrBuf.windowTitle;
        if (r == null) r = connectionParams.get("name").toString();
        return r;
    }
}
