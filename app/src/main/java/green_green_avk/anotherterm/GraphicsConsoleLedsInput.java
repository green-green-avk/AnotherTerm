package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

/**
 * Stub yet...
 */
public final class GraphicsConsoleLedsInput {
    public IConsoleOutput consoleOutput = null;

    public boolean numLed = false;
    public boolean capsLed = false;
    public boolean scrollLed = false;

    public interface OnInvalidateSink {
        void onInvalidateSink();
    }

    public void addOnInvalidateSink(@NonNull final OnInvalidateSink h) {
    }

    public void removeOnInvalidateSink(@NonNull final OnInvalidateSink h) {
    }
}
