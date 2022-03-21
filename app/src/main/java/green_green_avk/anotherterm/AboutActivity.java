package green_green_avk.anotherterm;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

public final class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        new AsyncLayoutInflater(this).inflate(R.layout.about_activity,
                (ViewGroup) getWindow().getDecorView().getRootView(),
                (view, i, viewGroup) -> setContentView(view));
    }

    public void onAppInfo(final View view) {
        try {
            startActivity(
                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts(
                                    "package",
                                    BuildConfig.APPLICATION_ID,
                                    null
                            ))
            );
        } catch (final ActivityNotFoundException | SecurityException ignored) {
        }
    }
}
