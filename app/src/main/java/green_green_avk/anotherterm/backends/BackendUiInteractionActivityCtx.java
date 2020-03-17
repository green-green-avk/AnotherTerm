package green_green_avk.anotherterm.backends;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

public interface BackendUiInteractionActivityCtx {
    @UiThread
    void setActivity(@Nullable Activity ctx);
}
