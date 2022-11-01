package green_green_avk.anotherterm;

import android.Manifest;
import android.os.Build;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Collection;
import java.util.Collections;

@RequiresApi(api = Build.VERSION_CODES.S)
public final class BluetoothAccessFragment extends PermissionsAccessFragment {
    static final Collection<String> PERMS =
            Collections.singleton(Manifest.permission.BLUETOOTH_CONNECT);

    @NonNull
    @Override
    public Collection<String> getPerms() {
        return PERMS;
    }

    @LayoutRes
    @Override
    public int getLayout() {
        return R.layout.bluetooth_access_fragment;
    }
}
