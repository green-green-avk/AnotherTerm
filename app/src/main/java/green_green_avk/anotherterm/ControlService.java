package green_green_avk.anotherterm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import java.util.Locale;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.utils.ForegroundServices;
import green_green_avk.anotherterm.utils.PreferenceStorage;

public final class ControlService extends Service {
    private static final int FG_ID = 2;

    /**
     * Start session request.
     */
    public static final String ACTION_START_SESSION =
            BuildConfig.NAMESPACE + ".intent.action.START_SESSION";
    /**
     * Unique favorite token to avoid spoofing.
     */
    public static final String EXTRA_FAV_TOKEN =
            BuildConfig.NAMESPACE + ".intent.extra.FAV_TOKEN";
    public static final String EXTRA_FAV_TOKEN_INPUT_NAME = getEnvName(EXTRA_FAV_TOKEN);
    public static final String FAV_TOKEN_KEY = "!token";
    public static final int FAV_TOKEN_LENGTH_MIN = 32;

    @NonNull
    private static String getEnvName(@NonNull final String name) {
        return "$input." + name.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]", "_");
    }

    @Nullable
    private static PreferenceStorage getFav(@NonNull final Intent intent) {
        final String token = intent.getStringExtra(EXTRA_FAV_TOKEN);
        if (token == null || token.length() < FAV_TOKEN_LENGTH_MIN) return null;
        for (final String name : FavoritesManager.enumerate()) {
            final PreferenceStorage ps = FavoritesManager.get(name);
            final int id = BackendsList.getId(ps.get("type"));
            if (id < 0 || !BackendsList.get(id).exportable) continue;
            if (!token.equals(ps.get(FAV_TOKEN_KEY))) continue;
            final Bundle ee = intent.getExtras();
            for (final String k : ee.keySet()) {
                if (EXTRA_FAV_TOKEN.equals(k)) continue; // Hide the token value
                final Object v = ee.get(k);
                if (!(v instanceof String)) continue;
                ps.put(getEnvName(k), v);
            }
            ps.put(EXTRA_FAV_TOKEN_INPUT_NAME, ""); // Mark as run by token
            ps.put("name", name); // Some mark
            return ps;
        }
        return null;
    }

    private void startSession(@NonNull final Intent intent) {
        final PreferenceStorage ps = getFav(intent);
        if (ps == null) return;
        try {
            ConsoleService.startAnsiSession(this, ps.get());
        } catch (final ConsoleService.Exception | BackendException ignored) {
        }
    }

    /**
     * This service is extremely short-living.
     * It implements the trampoline logic to start a terminal session.
     */
    private void tryFg() {
        final Context appCtx = getApplicationContext();
        final TaskStackBuilder tsb = TaskStackBuilder.create(appCtx)
                .addNextIntentWithParentStack(new Intent(appCtx,
                        SessionsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        final Notification n = new NotificationCompat.Builder(appCtx,
                ForegroundServices.getAppNotificationChannelId(appCtx))
                .setContentTitle(getString(R.string.there_are_active_session_startups))
                .setSmallIcon(R.drawable.ic_stat_serv)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(tsb.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .build();
        try {
            startForeground(FG_ID, n);
        } catch (final Exception e) {
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        tryFg();
        if (intent != null && ACTION_START_SESSION.equals(intent.getAction()))
            startSession(intent);
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Override
    @Nullable
    public IBinder onBind(final Intent intent) {
        // Not yet...
        return null;
    }
}
