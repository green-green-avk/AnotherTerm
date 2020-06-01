package green_green_avk.anotherterm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.BackendUiDialogs;

public final class ConsoleService extends Service {

    public static class Exception extends RuntimeException {
        public Exception(final String m) {
            super(m);
        }
    }

    private static final String EMSG_NI_CONNTYPE = "This Connection type is not implemented yet";
    private static final int ID_FG = 1;
    private static final String NOTIFICATION_CHANNEL_ID = ConsoleService.class.getName();
    private static ConsoleService instance = null;

    public static ConsoleService getInstance() {
        return instance;
    }

    public static final Map<Integer, Session> sessions = new HashMap<>();
    public static final List<Integer> sessionKeys = new ArrayList<>(); // I doubt that org.apache.commons.collections4.list.TreeList is better here

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        tryFg();
    }

    private static void tryStart(final Context appCtx) {
        appCtx.startService(new Intent(appCtx.getApplicationContext(), ConsoleService.class));
    }

    private static void tryStop() {
        if (instance != null) instance.stopSelf();
    }

    private void tryFg() {
        final TaskStackBuilder tsb = TaskStackBuilder.create(getApplicationContext());
        tsb.addNextIntentWithParentStack(
                new Intent(getApplicationContext(), SessionsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel nc = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(nc);
        }
        final Notification n = new NotificationCompat.Builder(getApplicationContext(),
                NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.there_are_active_terminals))
//                .setContentText("Console is running")
                .setSmallIcon(R.drawable.ic_stat_serv)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(tsb.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        startForeground(ID_FG, n);
//        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        throw new UnsupportedOperationException("Not supposed to be bound");
    }

    private static int currKey = 0;

    private static int obtainKey() {
        return currKey++;
    }

    @NonNull
    public static BackendsList.Item getBackendByParams(@NonNull final Map<String, ?> cp) {
        final String type = (String) cp.get("type");
        final int id = BackendsList.getId(type);
        if (id < 0) throw new Exception(EMSG_NI_CONNTYPE);
        return BackendsList.get(id);
    }

    @UiThread
    public static int startSession(@NonNull final Context ctx, @NonNull final Map<String, ?> cp) {
        final Context appCtx = ctx.getApplicationContext();
        final Class<?> klass = getBackendByParams(cp).impl;
        final BackendModule tbe;
        try {
            tbe = (BackendModule) klass.newInstance();
        } catch (IllegalAccessException e) {
            throw new Exception(EMSG_NI_CONNTYPE);
        } catch (InstantiationException e) {
            throw new Exception(EMSG_NI_CONNTYPE);
        }
        final ConsoleInput ci = new ConsoleInput();
        final ConsoleOutput co = new ConsoleOutput();
        final String charsetStr = (String) cp.get("charset");
        try {
            final Charset charset = Charset.forName(charsetStr);
            ci.setCharset(charset);
            co.setCharset(charset);
        } catch (IllegalArgumentException e) {
            Log.e("Charset", charsetStr, e);
        }
        String keyMapStr = (String) cp.get("keymap");
        if (keyMapStr != null && keyMapStr.isEmpty()) keyMapStr = null;
        co.setKeyMap(TermKeyMapManager.get(keyMapStr));
        final int key = obtainKey();
        tbe.setContext(ctx);
        tbe.setOnWakeLockEvent(new Runnable() {
            @Override
            public void run() {
                execOnSessionChange(key);
            }
        }, new Handler());
        tbe.setReleaseWakeLockOnDisconnect("releaseOnDisconnect".equals(cp.get("wakelock_policy")));
        tbe.setUi(new BackendUiDialogs());
        tbe.setParameters(cp);

        final EventBasedBackendModuleWrapper be = new EventBasedBackendModuleWrapper(
                tbe, new EventBasedBackendModuleWrapper.Listener() {
            @Override
            public void onConnecting() {
                tbe.getUi().showToast(appCtx.getString(R.string.msg_connecting___));
                execOnSessionChange(key);
            }

            @Override
            public void onConnected() {
                tbe.getUi().showToast(appCtx.getString(R.string.msg_connected));
                execOnSessionChange(key);
            }

            @Override
            public void onDisconnected() {
                tbe.getUi().showMessage(appCtx.getString(R.string.msg_disconnected));
                execOnSessionChange(key);
            }

            @Override
            public void onRead(@NonNull final ByteBuffer v) {
                ci.feed(v);
                ci.invalidateSink();
            }

            @Override
            public void onError(@NonNull final Throwable e) {
//                ci.currScrBuf.setChars(e.toString());
                tbe.getUi().showMessage(e.getMessage());
            }
        });
        ci.consoleOutput = co;
        ci.backendModule = be;
        co.backendModule = be;

        final Session s = new Session(cp, ci, co, be);

        ci.setWindowTitle(getSessionTitle(s, key));

        sessions.put(key, s);
        sessionKeys.add(key);

        tryStart(appCtx);
        execOnSessionsListChange();

        be.connect();

        return key;
    }

    @UiThread
    public static void stopSession(final int key) {
        final Session s = getSession(key);
        s.backend.stop();
        sessionKeys.remove((Integer) key);
        sessions.remove(key);
        if (sessionKeys.size() <= 0) tryStop();
        execOnSessionsListChange();
    }

    @UiThread
    @CheckResult
    @NonNull
    public static Session getSession(final int key) {
        final Session s = sessions.get(key);
        if (s == null) throw new NoSuchElementException("No session with the specified key exists");
        return s;
    }

    @UiThread
    @CheckResult
    @NonNull
    private static String getSessionTitle(@NonNull final Session s, final int key) {
        return String.format(Locale.ROOT, "%1$s #%2$d", s.connectionParams.get("name"), key);
    }

    @UiThread
    @CheckResult
    @NonNull
    public static String getSessionTitle(final int key) {
        return getSessionTitle(getSession(key), key);
    }

    private static void execOnSessionsListChange() {
        for (final Listener listener : listeners) {
            listener.onSessionsListChange();
        }
    }

    private static void execOnSessionChange(final int key) {
        for (final Listener listener : listeners) {
            listener.onSessionChange(key);
        }
    }

    public static class Listener {
        protected void onSessionsListChange() {
        }

        protected void onSessionChange(final int key) {
        }
    }

    private static final Set<Listener> listeners =
            Collections.newSetFromMap(new WeakHashMap<Listener, Boolean>());

    public static void addListener(@NonNull final Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(@NonNull final Listener listener) {
        listeners.remove(listener);
    }
}
