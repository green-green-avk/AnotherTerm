package green_green_avk.anotherterm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.BackendUiSessionDialogs;
import green_green_avk.anotherterm.utils.ForegroundServices;
import green_green_avk.anotherterm.utils.Misc;

public final class ConsoleService extends Service {

    public static class Exception extends RuntimeException {
        public Exception(final String m) {
            super(m);
        }
    }

    private static final String EMSG_NI_CONNTYPE = "This Connection type is not implemented yet";
    private static final int FG_ID = 1;

    private static AnsiSession.Properties.Condition parseCondition(@Nullable final Object v) {
        if (C.COND_STR_PROCESS_EXIT_STATUS_0.equals(v))
            return AnsiSession.Properties.PROCESS_EXIT_STATUS_0;
        else
            return Misc.toBoolean(v)
                    ? AnsiSession.Properties.ALWAYS
                    : AnsiSession.Properties.NEVER;
    }

    private static ConsoleService instance = null;

    @Nullable
    public static ConsoleService getInstance() {
        return instance;
    }

    public static final Map<Integer, Session> sessions = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        tryFg();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent == null)
            stopSelf(); // Nothing to do after restart
        return START_STICKY; // Just in case
    }

    private static void tryStart(@NonNull final Context appCtx) {
        ForegroundServices.startForegroundService(appCtx, ConsoleService.class);
    }

    private static void tryStop() {
        if (instance != null)
            instance.stopSelf();
    }

    private void tryFg() {
        final Context appCtx = getApplicationContext();
        final TaskStackBuilder tsb = TaskStackBuilder.create(appCtx)
                .addNextIntentWithParentStack(new Intent(appCtx,
                        SessionsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        final Notification n = new NotificationCompat.Builder(appCtx,
                ForegroundServices.getAppNotificationChannelId(appCtx))
                .setContentTitle(getString(R.string.there_are_active_terminals))
                .setSmallIcon(R.drawable.ic_stat_serv)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(tsb.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
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

    public static final int INVALID_SESSION = -1;

    private static int currKey = 0;

    private static int obtainKey() {
        do {
            currKey = (currKey + 1) & Integer.MAX_VALUE;
        } while (sessions.containsKey(currKey));
        return currKey;
    }

    @NonNull
    public static BackendsList.Item getBackendByParams(@NonNull final Map<String, ?> cp) {
        final String type = (String) cp.get("type");
        final int id = BackendsList.getId(type);
        if (id < 0) throw new Exception(EMSG_NI_CONNTYPE);
        return BackendsList.get(id);
    }

    @UiThread
    @NonNull
    private static BackendModule createBackend(@NonNull final Map<String, ?> cp) {
        final Class<? extends BackendModule> klass = getBackendByParams(cp).impl;
        final BackendModule tbe;
        try {
            tbe = klass.newInstance();
        } catch (final IllegalAccessException e) {
            throw new Exception(EMSG_NI_CONNTYPE);
        } catch (final InstantiationException e) {
            throw new Exception(EMSG_NI_CONNTYPE);
        }
        return tbe;
    }

    @UiThread
    public static int startAnsiSession(@NonNull final Context ctx,
                                       @NonNull final Map<String, ?> cp) {
        return startAnsiSession(ctx, cp, createBackend(cp), true);
    }

    @UiThread
    public static int startAnsiSession(@NonNull final Context ctx,
                                       @NonNull final Map<String, ?> cp,
                                       @NonNull final BackendModule tbe) {
        return startAnsiSession(ctx, cp, tbe, false);
    }

    @UiThread
    private static int startAnsiSession(@NonNull final Context ctx,
                                        @NonNull final Map<String, ?> cp,
                                        @NonNull final BackendModule tbe,
                                        final boolean setBeParams) {
        final Context appCtx = ctx.getApplicationContext();
        final AnsiConsoleInput ci = new AnsiConsoleInput();
        final AnsiConsoleOutput co = new AnsiConsoleOutput();
        ci.consoleOutput = co;
        final String termComplianceStr = (String) cp.get("term_compliance");
        ci.setComplianceLevel("vt52compat".equals(termComplianceStr) ?
                0 : AnsiConsoleInput.defaultComplianceLevel);
        final String charsetStr = (String) cp.get("charset");
        try {
            final Charset charset =
                    charsetStr != null ? Charset.forName(charsetStr) : Charset.defaultCharset();
            ci.setCharset(charset);
            co.setCharset(charset);
        } catch (final IllegalArgumentException e) {
            Log.e("Charset", charsetStr, e);
        }
        String keyMapStr = (String) cp.get("keymap");
        if (keyMapStr != null && keyMapStr.isEmpty()) keyMapStr = null;
        co.setKeyMap(TermKeyMapManager.instance.get(keyMapStr));
        final int key = obtainKey();
        tbe.setContext(ctx);
        tbe.setOnWakeLockEvent(() -> execOnSessionChange(key), new Handler(Looper.getMainLooper()));
        tbe.setAcquireWakeLockOnConnect(Misc.toBoolean(cp.get("wakelock.acquire_on_connect")));
        tbe.setReleaseWakeLockOnDisconnect(Misc.toBoolean(cp.get("wakelock.release_on_disconnect")));
        tbe.setUi(new BackendUiSessionDialogs(key));
        if (setBeParams)
            tbe.setParameters(cp);

        final AnsiSession.Properties pp = new AnsiSession.Properties();
        pp.terminateOnDisconnect = parseCondition(cp.get("terminate.on_disconnect"));
        if (pp.terminateOnDisconnect == AnsiSession.Properties.NEVER) {
            pp.terminateOnDisconnect = AnsiSession.Properties.ALWAYS;
            pp.terminateOnDisconnectEnabled = false;
        } else {
            pp.terminateOnDisconnectEnabled = true;
        }

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
                if (pp.terminateOnDisconnectEnabled && pp.terminateOnDisconnect.check(tbe))
                    try {
                        stopSession(key);
                    } catch (final NoSuchElementException ignored) {
                    }
            }

            @Override
            public void onRead(@NonNull final ByteBuffer v) {
                ci.feed(v);
                ci.invalidateSink();
            }

            @Override
            public void onError(@NonNull final Throwable e) {
                final String msg = e.getMessage();
                tbe.getUi().showMessage(msg != null ? msg : e.toString());
            }

            @Override
            public void onMessage(@NonNull final String m) {
                tbe.getUi().showMessage(m);
            }
        });
        ci.backendModule = be;
        co.backendModule = be;

        final AnsiSession s = new AnsiSession(cp, ci, co, be, pp);

        final Object name = cp.get("name");
        ci.setWindowTitle(name != null ? name + " - #" + key : "#" + key);

        sessions.put(key, s);

        tryStart(appCtx);
        execOnSessionsListChange();

        be.connect();

        return key;
    }

    @UiThread
    public static int startGraphicsSession(@NonNull final Context ctx,
                                           @NonNull final GraphicsCompositor compositor) {
        final Context appCtx = ctx.getApplicationContext();
        final int key = obtainKey();

        final GraphicsSession s = new GraphicsSession(compositor);
        sessions.put(key, s);

        tryStart(appCtx);
        execOnSessionsListChange();

        return key;
    }

    @UiThread
    public static void stopSession(final int key) {
        final Session s = getSession(key);
        if (s instanceof AnsiSession)
            ((AnsiSession) s).backend.stop();
        else if (s instanceof GraphicsSession)
            ((GraphicsSession) s).compositor.stop();
        sessions.remove(key);
        if (instance != null)
            if (s instanceof AnsiSession)
                ContentUrisPermsManager.freeUnusedTemp(instance,
                        ((AnsiSession) s).boundUris);
        if (sessions.isEmpty())
            tryStop();
        execOnSessionChange(key);
        execOnSessionsListChange();
    }

    @UiThread
    @CheckResult
    public static boolean isSessionTerminated(final int key) {
        return !hasSession(key);
    }

    @UiThread
    @CheckResult
    public static boolean hasSession(final int key) {
        return sessions.get(key) != null;
    }

    @UiThread
    @CheckResult
    @NonNull
    public static Session getSession(final int key) {
        final Session s = sessions.get(key);
        if (s == null)
            throw new NoSuchElementException("No session with the specified key exists");
        return s;
    }

    @UiThread
    @CheckResult
    public static boolean hasAnsiSession(final int key) {
        return sessions.get(key) instanceof AnsiSession;
    }

    @UiThread
    @CheckResult
    @NonNull
    public static AnsiSession getAnsiSession(final int key) {
        final Session s = getSession(key);
        if (!(s instanceof AnsiSession))
            throw new NoSuchElementException("No ANSI session with the specified key exists");
        return (AnsiSession) s;
    }

    @UiThread
    @CheckResult
    public static boolean hasGraphicsSession(final int key) {
        return sessions.get(key) instanceof GraphicsSession;
    }

    @UiThread
    @CheckResult
    @NonNull
    public static GraphicsSession getGraphicsSession(final int key) {
        final Session s = getSession(key);
        if (!(s instanceof GraphicsSession))
            throw new NoSuchElementException("No Graphics session with the specified key exists");
        return (GraphicsSession) s;
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

    public static abstract class Listener {
        protected void onSessionsListChange() {
        }

        protected void onSessionChange(final int key) {
        }
    }

    private static final Set<Listener> listeners =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void addListener(@NonNull final Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(@NonNull final Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onTrimMemory(final int level) {
        if (level >= TRIM_MEMORY_RUNNING_LOW && level < TRIM_MEMORY_UI_HIDDEN
                || level >= TRIM_MEMORY_MODERATE)
            for (final Session session : sessions.values())
                if (session instanceof AnsiSession)
                    ((AnsiSession) session).input.optimize();
        super.onTrimMemory(level);
    }
}
