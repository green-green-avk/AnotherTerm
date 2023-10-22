package green_green_avk.anotherterm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SettingsActivity extends AppCompatPreferenceActivity {
    private final List<Header> headers = new ArrayList<>();
    private final Set<String> fragments = new HashSet<>();

    private void ensureHeaders() {
        if (headers.isEmpty()) {
            loadHeaders(headers);
            fragments.clear();
            for (final Header header : headers)
                fragments.add(header.fragment);
        }
    }

    private void loadHeaders(@NonNull final List<Header> target) {
        // Native configuration dependent resource fetching does not work for xml:
        // xml-v23 subfolder is pretty useless...
        if (Build.VERSION.SDK_INT >= 31)
            loadHeadersFromResource(R.xml.pref_headers_v31, target);
        else if (Build.VERSION.SDK_INT >= 23)
            loadHeadersFromResource(R.xml.pref_headers_v23, target);
        else if (Build.VERSION.SDK_INT >= 19)
            loadHeadersFromResource(R.xml.pref_headers_v19, target);
        else
            loadHeadersFromResource(R.xml.pref_headers, target);
    }

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
        ensureHeaders();
        target.addAll(headers);
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        ensureHeaders();
        return fragments.contains(fragmentName);
    }

    public static final class TerminalPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreatePreferences(final Bundle bundle, final String s) {
            addPreferencesFromResource(R.xml.pref_terminal);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                findPreference("terminal_use_recents").getParent().setVisible(false);
            }
        }
    }

    public static void showPane(@NonNull final Activity ctx, @NonNull final String pane) {
        ctx.startActivity(new Intent(ctx, SettingsActivity.class)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, pane));
    }
}
