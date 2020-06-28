package green_green_avk.anotherterm;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

@RequiresApi(23)
public final class DozeFragment extends Fragment {
    public DozeFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.doze_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        final String packageName = activity.getApplicationContext().getPackageName();
        final PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        final boolean isOpt = !pm.isIgnoringBatteryOptimizations(packageName);
        ((TextView) getView().findViewById(R.id.f_is_optimized)).setText(
                isOpt
                        ? R.string.optimized
                        : R.string.not_optimized);
        final Switch bSwitch = getView().findViewById(R.id.b_switch);
        bSwitch.setOnCheckedChangeListener(null);
        bSwitch.setChecked(isOpt);
        bSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        openSettings(buttonView);
                    }
                });
    }

    public void openSettings(final View v) {
        final Intent intent = new Intent();
        final Activity activity = getActivity();
        final String packageName = activity.getApplicationContext().getPackageName();
        final PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName))
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        else {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
        }
        this.startActivity(intent);
    }
}
