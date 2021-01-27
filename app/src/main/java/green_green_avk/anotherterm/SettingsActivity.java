package green_green_avk.anotherterm;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragment;

import java.util.List;

public final class SettingsActivity extends AppCompatPreferenceActivity {

    private static boolean isXLargeTablet(@NonNull final Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) ||
                this.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onBuildHeaders(final List<Header> target) {
        // Native configuration dependent resource fetching does not works for xml:
        // xml-v23 subfolder is pretty useless...
        if (Build.VERSION.SDK_INT >= 23)
            loadHeadersFromResource(R.xml.pref_headers_v23, target);
        else
            loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || TerminalPreferenceFragment.class.getName().equals(fragmentName)
                || HwKeyMapEditorFragment.class.getName().equals(fragmentName)
                || CustomFontsFragment.class.getName().equals(fragmentName)
                || DozeFragment.class.getName().equals(fragmentName)
                || DeviceStorageAccessFragment.class.getName().equals(fragmentName)
                || PluginsManagerFragment.class.getName().equals(fragmentName);
    }

    public static final class TerminalPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreatePreferences(final Bundle bundle, final String s) {
            addPreferencesFromResource(R.xml.pref_terminal);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                findPreference("terminal_use_recents").setVisible(false);
        }
    }
}
