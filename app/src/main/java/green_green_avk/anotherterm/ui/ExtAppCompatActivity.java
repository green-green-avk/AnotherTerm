package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ExtAppCompatActivity extends AppCompatActivity {
    public final RequesterCompatDelegate activityRequester =
            new RequesterCompatDelegate(this);

    @NonNull
    public static ExtAppCompatActivity getByContext(@NonNull final Context context) {
        Context c = context;
        while (c instanceof ContextWrapper) {
            if (c instanceof ExtAppCompatActivity)
                return (ExtAppCompatActivity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        throw new IllegalArgumentException();
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityRequester.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityRequester.onResume();
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        activityRequester.onActivityResult(requestCode, resultCode, data);
    }
}
