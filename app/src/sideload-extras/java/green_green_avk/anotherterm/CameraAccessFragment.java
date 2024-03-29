package green_green_avk.anotherterm;

import android.Manifest;
import android.os.Build;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Collection;
import java.util.Collections;

@RequiresApi(api = Build.VERSION_CODES.M)
public final class CameraAccessFragment extends PermissionsAccessFragment {
    private static final Collection<String> PERMS =
            Collections.singleton(Manifest.permission.CAMERA);

    @NonNull
    @Override
    public Collection<String> getPerms() {
        return PERMS;
    }

    @LayoutRes
    @Override
    public int getLayout() {
        return R.layout.camera_access_fragment;
    }
}
