package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.GraphicsConsoleKeyboardView;

public final class GraphicsSession extends Session {

    public static final class UiState {
        public enum MouseMode {UNDEFINED, DIRECT, OVERLAID}

        //        public final ConsoleScreenView.State csv = new ConsoleScreenView.State();
        public final GraphicsConsoleKeyboardView.State ckv = new GraphicsConsoleKeyboardView.State();
        public MouseMode mouseMode = MouseMode.UNDEFINED;
//        public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
//        public boolean keepScreenOn = false;
    }

    @NonNull
    public final GraphicsCompositor compositor;

    public final GraphicsSession.UiState uiState = new GraphicsSession.UiState();

    public GraphicsSession(@NonNull final GraphicsCompositor compositor) {
        this.compositor = compositor;
    }

    @Override
    @NonNull
    public CharSequence getTitle() {
        return compositor.title;
    }
}
