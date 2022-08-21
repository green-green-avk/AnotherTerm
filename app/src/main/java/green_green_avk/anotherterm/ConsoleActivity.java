package green_green_avk.anotherterm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.NoSuchElementException;

import green_green_avk.anotherterm.ui.UiUtils;

public abstract class ConsoleActivity extends AppCompatActivity {
    @NonNull
    public static Intent getShowSessionIntent(@NonNull final Context ctx, final int key) {
        final App app;
        if (ctx instanceof App)
            app = (App) ctx;
        else if (ctx instanceof Activity)
            app = (App) ((Activity) ctx).getApplication();
        else
            app = null;
        return getShowSessionIntent(ctx, key,
                app != null && app.settings.terminal_use_recents);
    }

    @NonNull
    public static Intent getShowSessionIntent(@NonNull final Context ctx, final int key,
                                              final boolean inRecents) {
        final Session s = ConsoleService.getSession(key);
        final Class<? extends Activity> a;
        if (s instanceof AnsiSession)
            a = AnsiConsoleActivity.class;
        else
            throw new IllegalArgumentException("Bad session key");
        final Intent intent = new Intent(ctx, a)
                .putExtra(C.IFK_MSG_SESS_KEY, key);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setData(Uri.parse("key:" + key));
            if (inRecents)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        return intent;
    }

    public static void showSession(@NonNull final Context ctx, final int key) {
        try {
            ctx.startActivity(getShowSessionIntent(ctx, key));
        } catch (final IllegalArgumentException ignored) {
        }
    }

    public static void showSession(@NonNull final Context ctx, final int key,
                                   final boolean inRecents) {
        try {
            ctx.startActivity(getShowSessionIntent(ctx, key, inRecents));
        } catch (final IllegalArgumentException ignored) {
        }
    }

    protected int mSessionKey = ConsoleService.INVALID_SESSION;

    protected boolean getUseRecents() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                ((App) getApplication()).settings.terminal_use_recents;
    }

    @Override
    public void onMultiWindowModeChanged(final boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        getWindow().setFlags(isInMultiWindowMode ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ConsoleService.sessions.isEmpty()) {
            finish();
            return;
        }
        final Intent intent = getIntent();
        if (!intent.hasExtra(C.IFK_MSG_SESS_KEY)) {
            finish();
            return;
        }
        mSessionKey = intent.getIntExtra(C.IFK_MSG_SESS_KEY, 0);
    }

    public void onTerminate(final View view) {
        if (view != null) {
            UiUtils.confirm(this, getString(R.string.prompt_terminate_the_session),
                    () -> onTerminate(null));
            return;
        }
        try {
            ConsoleService.stopSession(mSessionKey);
        } catch (final NoSuchElementException ignored) {
        }
        finish();
    }
}
