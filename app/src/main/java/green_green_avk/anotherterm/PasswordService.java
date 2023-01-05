package green_green_avk.anotherterm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.utils.ForegroundServices;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.anotherterm.utils.Password;

public final class PasswordService extends Service {
    private static final int FG_ID = 3;

    private static PasswordService instance = null;

    public static PasswordService getInstance() {
        return instance;
    }

    public interface ContextProvider {
        @Nullable
        Context get();
    }

    @Nullable
    private static ContextProvider contextProvider = null;

    public static void setContextProvider(@NonNull final ContextProvider v) {
        contextProvider = v;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (isEmpty())
            stopSelf();
        else
            tryFg();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent == null)
            stopSelf(); // Nothing to do after restart
        return START_STICKY; // Just in case
    }

    @MainThread
    private static void tryStart(@NonNull final Context appCtx) {
        ForegroundServices.startForegroundService(appCtx, PasswordService.class);
    }

    @MainThread
    private static void tryStop() {
        if (instance != null)
            instance.stopSelf();
    }

    @MainThread
    private void tryFg() {
        final Notification n = new NotificationCompat.Builder(this,
                ForegroundServices.getAppNotificationChannelId(this))
                .setContentTitle(getString(R.string.there_are_in_memory_stored_passwords))
                .setSmallIcon(R.drawable.ic_stat_pwd)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this,
                                PasswordManagementActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE))
                .build();
        startForeground(FG_ID, n);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    @Nullable
    public IBinder onBind(final Intent intent) {
        return null;
    }

    private static final Map<String, Password> storage = new HashMap<>();

    private static void putRef(@NonNull final String target, @NonNull final Password pwd) {
        synchronized (storage) {
            remove(target, null);
            storage.put(target, pwd);
            assert contextProvider != null;
            final Context ctx = contextProvider.get();
            if (ctx != null) {
                final Context appCtx = ctx.getApplicationContext();
                Misc.postOnMainThread(() -> {
                    if (!isEmpty())
                        tryStart(appCtx);
                });
            }
        }
    }

    public static boolean isEmpty() {
        synchronized (storage) {
            return storage.isEmpty();
        }
    }

    /**
     * Gets a password from the storage.
     *
     * @param target a target
     * @return a copy of the password or {@code null} if not set
     */
    @Nullable
    public static Password get(@NonNull final String target) {
        synchronized (storage) {
            final Password r = storage.get(target);
            if (r == null)
                return null;
            return r.copy();
        }
    }

    /**
     * Puts a password copy into the storage and destroy the previous one.
     *
     * @param target a target
     * @param pwd    a password
     */
    public static void put(@NonNull final String target, @NonNull final CharSequence pwd) {
        putRef(target, Password.from(pwd));
    }

    /**
     * Erases a stored password.
     *
     * @param target a target
     * @param pwd    if not {@code null}, remove only if the stored password matches
     */
    public static void remove(@NonNull final String target, @Nullable final CharSequence pwd) {
        synchronized (storage) {
            final Password prev;
            if (pwd == null) {
                prev = storage.remove(target);
            } else {
                prev = storage.get(target);
                if (prev != null && prev.matches(pwd))
                    storage.remove(target);
                else
                    return;
            }
            if (prev != null) {
                prev.erase();
                Misc.postOnMainThread(() -> {
                    if (isEmpty())
                        tryStop();
                });
            }
        }
    }

    /**
     * Erases all the stored passwords.
     */
    public static void clear() {
        synchronized (storage) {
            for (final Password pwd : storage.values()) {
                if (pwd != null)
                    pwd.erase();
            }
            storage.clear();
            Misc.postOnMainThread(() -> {
                if (isEmpty())
                    tryStop();
            });
        }
    }

    /**
     * Enumerates all the stored passwords.
     *
     * @return a set of targets
     */
    @NonNull
    public static Set<String> enumerate() {
        synchronized (storage) {
            return new HashSet<>(storage.keySet());
        }
    }
}
