package green_green_avk.anotherterm;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

public final class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        new AsyncLayoutInflater(this).inflate(R.layout.about_activity,
                (ViewGroup) getWindow().getDecorView().getRootView(),
                new AsyncLayoutInflater.OnInflateFinishedListener() {
                    @Override
                    public void onInflateFinished(@NonNull final View view, final int i,
                                                  @Nullable final ViewGroup viewGroup) {
                        setContentView(view);
                    }
                });
    }
}
