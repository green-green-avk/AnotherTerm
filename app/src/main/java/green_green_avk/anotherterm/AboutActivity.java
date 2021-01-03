package green_green_avk.anotherterm;

import android.os.Bundle;
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
}
