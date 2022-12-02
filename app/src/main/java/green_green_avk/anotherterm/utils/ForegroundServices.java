package green_green_avk.anotherterm.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.R;

public final class ForegroundServices {
    private ForegroundServices() {
    }

    private static final String APP_NOTIFICATION_CHANNEL_ID = "App";

    public static void startForegroundService(@NonNull final Context ctx,
                                              @NonNull final Class<? extends Service> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.startForegroundService(new Intent(ctx, clazz));
        else
            ctx.startService(new Intent(ctx, clazz));
    }

    @NonNull
    public static String getAppNotificationChannelId(@NonNull final Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel nc = new NotificationChannel(APP_NOTIFICATION_CHANNEL_ID,
                    ctx.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(nc);
        }
        return APP_NOTIFICATION_CHANNEL_ID;
    }
}
