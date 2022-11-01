package green_green_avk.anotherterm;

import android.Manifest;
import android.os.Build;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@RequiresApi(api = Build.VERSION_CODES.M)
public final class DeviceStorageAccessFragment extends PermissionsAccessFragment {
    private static final Collection<String> PERMS =
            Collections.unmodifiableCollection(Arrays.asList(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ));

    @NonNull
    @Override
    public Collection<String> getPerms() {
        return PERMS;
    }

    @LayoutRes
    @Override
    public int getLayout() {
        return R.layout.device_storage_access_fragment;
    }
}
