package green_green_avk.anotherterm;

import android.graphics.Bitmap;

import java.util.Map;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.ConsoleScreenView;

public final class Session {

    public static final class UiState {
        final ConsoleScreenView.State csv = new ConsoleScreenView.State();
    }

    public final Map<String, ?> connectionParams;
    public final ConsoleInput input;
    public final ConsoleOutput output;
    public final EventBasedBackendModuleWrapper backend;
    public Bitmap thumbnail = null;

    public final UiState uiState = new UiState();

    public Session(Map<String, ?> cp, ConsoleInput ci, ConsoleOutput co, EventBasedBackendModuleWrapper be) {
        connectionParams = cp;
        input = ci;
        output = co;
        backend = be;
    }
}
