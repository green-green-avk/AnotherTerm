package green_green_avk.anotherterm;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
public final class DozeFragment extends Fragment {
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.doze_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        final String packageName = activity.getPackageName();
        final PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        final boolean isOpt = !pm.isIgnoringBatteryOptimizations(packageName);
        getView().<TextView>findViewById(R.id.f_state).setText(
                isOpt
                        ? R.string.optimized
                        : R.string.not_optimized);
        final Switch bSwitch = getView().findViewById(R.id.b_switch);
        bSwitch.setOnCheckedChangeListener(null);
        bSwitch.setChecked(isOpt);
        bSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> openSettings(buttonView));
    }

    public void openSettings(final View v) {
        final Activity activity = getActivity();
        final String packageName = activity.getApplicationContext().getPackageName();
        final PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                this.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:" + packageName)));
                return;
            } catch (final ActivityNotFoundException | SecurityException ignored) {
            }
        }
        try {
            this.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        } catch (final ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, R.string.msg_unable_to_open_doze_settings,
                    Toast.LENGTH_LONG).show();
        }
    }
}
