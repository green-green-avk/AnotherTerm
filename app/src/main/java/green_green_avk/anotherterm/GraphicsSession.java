package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ConsoleKeyboardView;

public final class GraphicsSession extends Session {

    public static final class UiState {
        //        public final ConsoleScreenView.State csv = new ConsoleScreenView.State();
        public final ConsoleKeyboardView.State ckv = new ConsoleKeyboardView.State();
//        public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
//        public boolean keepScreenOn = false;
    }

    @NonNull
    public final GraphicsCompositor compositor;

    public final AnsiSession.UiState uiState = new AnsiSession.UiState();

    public GraphicsSession(@NonNull final GraphicsCompositor compositor) {
        this.compositor = compositor;
    }

    @Override
    @NonNull
    public CharSequence getTitle() {
        return compositor.title;
    }
}
