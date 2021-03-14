package green_green_avk.wayland.os;

import androidx.annotation.NonNull;

public interface WlEventHandler {
    void post(@NonNull Runnable runnable);
}
