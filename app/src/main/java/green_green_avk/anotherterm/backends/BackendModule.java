package green_green_avk.anotherterm.backends;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.utils.Misc;

public abstract class BackendModule {

    public static abstract class DisconnectionReason {
        public static final int NONE = 0;
        public static final int PROCESS_EXIT = 1;
    }

    public static final class ProcessExitDisconnectionReason extends DisconnectionReason {
        // Full 32-bit exit value:
        // https://unix.stackexchange.com/questions/418784/what-is-the-min-and-max-values-of-exit-codes-in-linux
        // https://www.austingroupbugs.net/view.php?id=594
        public final int status;

        public ProcessExitDisconnectionReason(final int status) {
            this.status = status;
        }
    }

    public static class Meta {
        @NonNull
        protected final Set<String> schemes;
        @NonNull
        public final Map<Method, ExportedUIMethod> methods;

        public Meta(@NonNull final Class<?> klass) {
            this.schemes = Collections.emptySet();
            this.methods = initMethods(klass);
        }

        public Meta(@NonNull final Class<?> klass, @NonNull final String defaultScheme) {
            this.schemes = Collections.singleton(defaultScheme);
            this.methods = initMethods(klass);
        }

        public Meta(@NonNull final Class<?> klass, @NonNull final Set<String> defaultSchemes) {
            this.schemes = Collections.unmodifiableSet(defaultSchemes);
            this.methods = initMethods(klass);
        }

        @NonNull
        protected Map<Method, ExportedUIMethod> initMethods(@NonNull final Class<?> klass) {
            final Map<Method, ExportedUIMethod> map = new HashMap<>();
            for (final Method m : klass.getDeclaredMethods()) {
                final ExportedUIMethod a = m.getAnnotation(ExportedUIMethod.class);
                if (a == null) continue;
                map.put(m, a);
            }
            return Collections.unmodifiableMap(map);
        }

        @NonNull
        public Map<String, ?> getDefaultParameters() {
            return Collections.emptyMap();
        }

        @Nullable
        public Map<String, String> checkParameters(@NonNull final Map<String, ?> params) {
            return null;
        }

        /**
         * @return disconnection reason types bits
         */
        public int getDisconnectionReasonTypes() {
            return DisconnectionReason.NONE;
        }

        public static final int ADAPTER_READY = 0;
        public static final int ADAPTER_ALREADY_IN_USE = 1; // in use by us
        public static final int ADAPTER_BUSY = 2; // by someone else

        public static abstract class Requirement {
            @DrawableRes
            public final int icon;
            @StringRes
            public final int description;

            protected Requirement(@DrawableRes final int icon, @StringRes final int description) {
                this.icon = icon;
                this.description = description;
            }

            public static final class Permissions extends Requirement {
                @NonNull
                public final Set<String> permissions;

                public Permissions(@DrawableRes final int icon, @StringRes final int description,
                                   @NonNull final Set<String> permissions) {
                    super(icon, description);
                    this.permissions = permissions;
                }
            }
        }

        @NonNull
        public Collection<Requirement> getRequirements(@NonNull final Context ctx) {
            return Collections.emptySet();
        }

        @NonNull
        public static Collection<Requirement> unfulfilled(@NonNull final Context ctx,
                                                          @NonNull final Collection<? extends Requirement> requirements) {
            final List<Requirement> r = new ArrayList<>();
            for (final Requirement req : requirements)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        req instanceof Requirement.Permissions) {
                    final Set<String> perms = ((Requirement.Permissions) req).permissions;
                    if (!Misc.checkSelfPermissions(ctx, perms).isEmpty())
                        r.add(req);
                } else {
                    r.add(req);
                }
            return r;
        }

        /**
         * @return {@code null} if not applicable for the module or
         * &lt;unique_name&gt; &lt;description&gt; as the key and one of
         * {@link #ADAPTER_READY}/{@link #ADAPTER_ALREADY_IN_USE}/{@link #ADAPTER_BUSY}
         * as the value.
         */
        @Nullable
        public Map<String, Integer> getAdapters(@NonNull final Context ctx) {
            return null;
        }

        @NonNull
        public Set<String> getUriSchemes() {
            return schemes;
        }

        @NonNull
        public Uri toUri(@NonNull final Map<String, ?> params) {
            final Uri.Builder b = new Uri.Builder()
                    .scheme(getUriSchemes().iterator().next())
                    .path("opts");
            for (final String k : params.keySet()) {
                if (!k.isEmpty() && k.charAt(0) != '!') // Private parameters
                    b.appendQueryParameter(k, params.get(k).toString());
            }
            return b.build();
        }

        @NonNull
        public Map<String, ?> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque()) throw new ParametersUriParseException();
            final Map<String, String> params = new HashMap<>();
            for (final String k : uri.getQueryParameterNames()) {
                // TODO: '+' decoding issue before Jelly Bean
                if (!k.isEmpty() && k.charAt(0) != '!') // Private parameters: no spoofing
                    params.put(k, uri.getQueryParameter(k));
            }
            return params;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExportedUIMethodOnThread {
        enum Thread {WRITE}

        Thread thread() default Thread.WRITE;

        boolean before() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExportedUIMethod {
        @StringRes int titleRes() default 0;

        @StringRes int longTitleRes() default 0;

        int order() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ExportedUIMethodIntEnum {
        int[] values() default {};

        @StringRes int[] titleRes() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ExportedUIMethodStrEnum {
        String[] values() default {};

        @StringRes int[] titleRes() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ExportedUIMethodFlags {
        long[] values() default {};

        @StringRes int[] titleRes() default {};
    }

    @NonNull
    static Meta getMeta(@NonNull final Class<?> klass, @NonNull final String defaultScheme) {
        try {
            final Field f = klass.getField("meta");
            return (Meta) f.get(null);
        } catch (final NoSuchFieldException ignored) {
        } catch (final IllegalAccessException ignored) {
        } catch (final NullPointerException ignored) {
        } catch (final ClassCastException ignored) {
        } catch (final SecurityException e) {
            Log.e("Backend module", "Different class loaders", e);
        }
        return new Meta(klass, defaultScheme);
    }

    public Object callMethod(@NonNull final Method m, final Object... args) {
        try {
            return m.invoke(this, args);
        } catch (final IllegalAccessException ignored) {
        } catch (final InvocationTargetException e) {
            final Throwable t = e.getCause();
            if (t instanceof Error)
                throw (Error) t;
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
        }
        return null;
    }

    protected Context context = null;

    public static final class ParametersWrapper {
        private final Map<String, ?> map;

        public ParametersWrapper(final Map<String, ?> params) {
            map = params;
        }

        @NonNull
        private static String dumpType(@Nullable final Object v) {
            return v != null ? v.getClass().getSimpleName() : "<null>";
        }

        public String getString(final String key, final String def) {
            if (!map.containsKey(key))
                return def;
            try {
                return map.get(key).toString();
            } catch (final RuntimeException e) {
                throw new BackendException(e);
            }
        }

        public int getInt(final String key, final int def) {
            if (!map.containsKey(key))
                return def;
            final Object v = map.get(key);
            if (v instanceof Integer)
                return (int) v;
            if (v instanceof Long)
                return (int) (long) v;
            throw new BackendException("Incompatible type of the key `" + key + "': " +
                    dumpType(v));
        }

        public boolean getBoolean(final String key, final boolean def) {
            if (!map.containsKey(key))
                return def;
            final Object v = map.get(key);
            if (v instanceof Boolean)
                return (boolean) v;
            throw new BackendException("Incompatible type of the key `" + key + "': " +
                    dumpType(v));
        }

        public <T> T getFromMap(final String key, final Map<String, ? extends T> optsMap,
                                final T def) {
            if (!map.containsKey(key))
                return def;
            final Object v = map.get(key);
            if (!(v instanceof String))
                throw new BackendException("Bad option type: " + key);
            if (!optsMap.containsKey(v))
                throw new BackendException("Bad option value: " + key);
            return optsMap.get(v);
        }
    }

    public static final class ParametersUriParseException extends RuntimeException {
        public ParametersUriParseException() {
            super();
        }

        public ParametersUriParseException(final String message) {
            super(message);
        }

        public ParametersUriParseException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public void setContext(@NonNull final Context context) {
        this.context = context.getApplicationContext();
        setWakeLock(this.context);
    }

    public abstract void setParameters(@NonNull Map<String, ?> params);

    protected BackendUiInteraction ui = null;

    public abstract void setOutputStream(@NonNull OutputStream stream);

    @NonNull
    public abstract OutputStream getOutputStream();

    public static abstract class StateMessage {
        @NonNull
        public final String message;

        public StateMessage() {
            this.message = "";
        }

        public StateMessage(@NonNull final String message) {
            this.message = message;
        }

        @Override
        @NonNull
        public String toString() {
            return this.getClass().getSimpleName() + ": " + message;
        }
    }

    public static final class DisconnectStateMessage extends StateMessage {
        public DisconnectStateMessage() {
            super();
        }

        public DisconnectStateMessage(@NonNull final String message) {
            super(message);
        }
    }

    public interface OnMessageListener {
        void onMessage(@NonNull Object msg);
    }

    public abstract void setOnMessageListener(@Nullable OnMessageListener l);

    public BackendUiInteraction getUi() {
        return ui;
    }

    public void setUi(final BackendUiInteraction ui) {
        this.ui = ui;
    }

    /**
     * Preparing to stop the whole session: revoke sensitive session data, for example.
     */
    @CallSuper
    public void stop() {
        unsetWakeLock();
    }

    public abstract boolean isConnected();

    public abstract void connect();

    public abstract void disconnect();

    public abstract void resize(int col, int row, int wp, int hp);

    @NonNull
    public abstract String getConnDesc();

    /**
     * @return disconnection reason (process exit status for PTY etc) ({@code null} if unknown)
     */
    @Nullable
    public DisconnectionReason getDisconnectionReason() {
        return null;
    }

    public static final class WakeLockRef {
        @NonNull
        private final WeakReference<BackendModule> ref;

        private WakeLockRef(@NonNull final BackendModule self) {
            this.ref = new WeakReference<>(self);
        }

        public boolean isHeld() {
            final BackendModule self = ref.get();
            if (self == null)
                return false;
            return self.isWakeLockHeld();
        }

        public void acquire() {
            final BackendModule self = ref.get();
            if (self == null)
                return;
            self.acquireWakeLock();
        }

        /**
         * @param timeout [ms]
         */
        public void acquire(final long timeout) {
            final BackendModule self = ref.get();
            if (self == null)
                return;
            self.acquireWakeLock(timeout);
        }

        public void release() {
            final BackendModule self = ref.get();
            if (self == null)
                return;
            self.releaseWakeLock();
        }
    }

    private final Object mWakeLockLock = new Object();
    private volatile PowerManager.WakeLock mWakeLock = null;
    private final Handler mWakeLockHandler = new Handler(Looper.getMainLooper());
    private final Runnable mWakeLockReleaser = this::releaseWakeLock;
    private Runnable onWakeLockEvent = null;
    private Handler onWakeLockEventHandler = null;
    private final Object onWakeLockEventLock = new Object();

    private void setWakeLock(@NonNull final Context ctx) {
        final PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        synchronized (mWakeLockLock) {
            unsetWakeLock();
            final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    ctx.getPackageName() + "/" + this.getClass().getName());
            wl.setReferenceCounted(false);
            mWakeLock = wl;
        }
    }

    private void unsetWakeLock() {
        synchronized (mWakeLockLock) {
            releaseWakeLock();
            mWakeLock = null;
        }
    }

    private void execOnWakeLockEvent() {
        final Runnable l;
        final Handler h;
        synchronized (onWakeLockEventLock) {
            l = onWakeLockEvent;
            h = onWakeLockEventHandler;
        }
        if (l != null) {
            if (h == null) {
                l.run();
            } else {
                h.post(l);
            }
        }
    }

    public void setOnWakeLockEvent(@Nullable final Runnable l, @Nullable final Handler h) {
        synchronized (onWakeLockEventLock) {
            onWakeLockEvent = l;
            onWakeLockEventHandler = h;
        }
    }

    @CheckResult
    public boolean isWakeLockHeld() {
        synchronized (mWakeLockLock) {
            final PowerManager.WakeLock wl = mWakeLock;
            if (wl == null) return false;
            return wl.isHeld();
        }
    }

    @SuppressLint("WakelockTimeout")
    public void acquireWakeLock() {
        synchronized (mWakeLockLock) {
            mWakeLockHandler.removeCallbacksAndMessages(null);
            final PowerManager.WakeLock wl = mWakeLock;
            if (wl == null) return;
            wl.acquire();
        }
        execOnWakeLockEvent();
    }

    /**
     * @param timeout [ms]
     */
    @SuppressLint("WakelockTimeout")
    public void acquireWakeLock(final long timeout) {
        synchronized (mWakeLockLock) {
            mWakeLockHandler.removeCallbacksAndMessages(null);
            final PowerManager.WakeLock wl = mWakeLock;
            if (wl == null) return;
            wl.acquire();
            mWakeLockHandler.postDelayed(mWakeLockReleaser, timeout);
        }
        execOnWakeLockEvent();
    }

    public void releaseWakeLock() {
        synchronized (mWakeLockLock) {
            mWakeLockHandler.removeCallbacksAndMessages(null);
            final PowerManager.WakeLock wl = mWakeLock;
            if (wl == null) return;
            wl.release();
        }
        execOnWakeLockEvent();
    }

    /**
     * @return A wake lock accessor for this connection.
     */
    @NonNull
    public WakeLockRef getWakeLock() {
        return new WakeLockRef(this);
    }

    // It's the module implementation responsibility to properly handle
    // releaseWakeLockOnDisconnect property.

    private volatile boolean releaseWakeLockOnDisconnect = false;

    public boolean isReleaseWakeLockOnDisconnect() {
        return releaseWakeLockOnDisconnect;
    }

    public void setReleaseWakeLockOnDisconnect(final boolean v) {
        releaseWakeLockOnDisconnect = v;
    }

    // It's the module implementation responsibility to properly handle
    // acquireWakeLockOnConnect property.

    private volatile boolean acquireWakeLockOnConnect = false;

    public boolean isAcquireWakeLockOnConnect() {
        return acquireWakeLockOnConnect;
    }

    public void setAcquireWakeLockOnConnect(final boolean v) {
        acquireWakeLockOnConnect = v;
    }
}
