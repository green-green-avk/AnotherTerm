package green_green_avk.anotherterm;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.Set;

import green_green_avk.anotherterm.utils.Misc;

@RequiresApi(23)
public final class DeviceStorageAccessFragment extends Fragment {
    private static final String[] PERMS =
            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.device_storage_access_fragment, container,
                false);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Set<String> notGranted = Misc.checkSelfPermissions(getActivity(), PERMS);
        getView().<TextView>findViewById(R.id.f_state).setText(
                notGranted.size() == 0
                        ? R.string.state_enabled
                        : R.string.state_disabled);
        final Switch bSwitch = getView().findViewById(R.id.b_switch);
        bSwitch.setOnCheckedChangeListener(null);
        bSwitch.setChecked(notGranted.size() == 0);
        bSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> openSettings(buttonView));
    }

    public void openSettings(final View v) {
        final Activity activity = getActivity();
        final Set<String> notGranted = Misc.checkSelfPermissions(activity, PERMS);
        if (notGranted.size() == 0) {
            final String packageName = activity.getPackageName();
            try {
                this.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
            } catch (final ActivityNotFoundException | SecurityException e) {
                Toast.makeText(activity, R.string.msg_unable_to_open_app_settings,
                        Toast.LENGTH_LONG).show();
                getView().<Switch>findViewById(R.id.b_switch).setChecked(true);
            }
        } else {
            requestPermissions(notGranted.toArray(new String[0]), 0);
        }
    }
}
