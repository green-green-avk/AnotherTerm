package green_green_avk.anotherterm;

import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.ConsoleKeyboardView;
import green_green_avk.anotherterm.ui.ConsoleScreenView;

public final class AnsiSession extends Session {

    public static final class UiState {
        public enum MouseMode {UNDEFINED, DIRECT, OVERLAID}

        public final ConsoleScreenView.State csv = new ConsoleScreenView.State();
        public final ConsoleKeyboardView.State ckv = new ConsoleKeyboardView.State();
        public MouseMode mouseMode = MouseMode.UNDEFINED;
        public float fontSizeDp = 0F; // Invariant for resolution switching
        public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        public boolean keepScreenOn = false;
    }

    public static final class Properties {
        public interface Condition {
            boolean check(@NonNull BackendModule be);
        }

        public static final Condition NEVER = be -> false;
        public static final Condition ALWAYS = be -> true;
        public static final Condition PROCESS_EXIT_STATUS_0 = be -> {
            final BackendModule.DisconnectionReason dr = be.getDisconnectionReason();
            return dr instanceof BackendModule.ProcessExitDisconnectionReason
                    && ((BackendModule.ProcessExitDisconnectionReason) dr).status == 0;
        };

        @NonNull
        public Condition terminateOnDisconnect = ALWAYS;
        public boolean terminateOnDisconnectEnabled = false;
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

    public final UiState uiState = new UiState();

    /**
     * URIs with temporary permissions to revoke on the session end.
     */
    public final Set<Uri> boundUris = new HashSet<>();

    public AnsiSession(@NonNull final Map<String, ?> cp,
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

    @Override
    @NonNull
    public CharSequence getTitle() {
        String r = input.currScrBuf.windowTitle;
        if (r == null)
            r = connectionParams.get("name").toString();
        return r;
    }
}
