package green_green_avk.anotherterm;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

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
        if (requirement instanceof BackendModule.Meta.Requirement.Permissions) {
            final Set<String> perms =
                    ((BackendModule.Meta.Requirement.Permissions) requirement).permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    BluetoothAccessFragment.PERMS.equals(perms))
                return (ctx, req) -> SettingsActivity.showPane((Activity) ctx,
                        BluetoothAccessFragment.class.getName());
        }
        return null;
    }
}
