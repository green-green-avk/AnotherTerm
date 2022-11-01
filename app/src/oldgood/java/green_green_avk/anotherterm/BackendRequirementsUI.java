package green_green_avk.anotherterm;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.backends.BackendModule;

final class BackendRequirementsUI {
    private BackendRequirementsUI() {
    }

    interface Solution {
        void solution(@NonNull Context context,
                      @NonNull BackendModule.Meta.Requirement requirement);
    }

    @Nullable
    static Solution resolve(@NonNull final Context context,
                            @NonNull final BackendModule.Meta.Requirement requirement) {
        return null;
    }
}
