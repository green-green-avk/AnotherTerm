package green_green_avk.anotherterm.backends;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

public interface BackendUiInteractionActivityCtx {
    @UiThread
    void setActivity(@Nullable Activity ctx);
}
