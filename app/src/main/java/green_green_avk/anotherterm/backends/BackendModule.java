package green_green_avk.anotherterm.backends;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.CheckResult;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BackendModule {

    public static class Meta {
        protected final Set<String> schemes;
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
                params.put(k, uri.getQueryParameter(k));
            }
            return params;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExportedUIMethod {
        @StringRes int titleRes() default 0;

        int order() default 0;
    }

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
            throw (RuntimeException) e.getCause();
        }
        return null;
    }

    protected Context context = null;

    public static final class ParametersWrapper {
        private final Map<String, ?> map;

        public ParametersWrapper(final Map<String, ?> params) {
            map = params;
        }

        public String getString(final String key, final String def) {
            if (!map.containsKey(key)) return def;
            try {
                return map.get(key).toString();
            } catch (final RuntimeException e) {
                throw new BackendException(e);
            }
        }

        public int getInt(final String key, final int def) {
            if (!map.containsKey(key)) return def;
            final Object v = map.get(key);
            if (v instanceof Integer) return (int) v;
            if (v instanceof Long) return (int) (long) v;
            throw new BackendException("Incompatible type of the key `" + key + "': " + v.getClass().getSimpleName());
        }

        public boolean getBoolean(final String key, final boolean def) {
            if (!map.containsKey(key)) return def;
            final Object v = map.get(key);
            if (v instanceof Boolean) return (boolean) v;
            throw new BackendException("Incompatible type of the key `" + key + "': " + v.getClass().getSimpleName());
        }

        public <T> T getFromMap(final String key, final Map<String, T> optsMap, final T def) {
            if (!map.containsKey(key)) return def;
            final Object v = map.get(key);
            if (!(v instanceof String)) throw new BackendException("Bad option type: " + key);
            if (!optsMap.containsKey(v)) throw new BackendException("Bad option value: " + key);
            return optsMap.get(v);
        }
    }

    public static final class ParametersUriParseException extends RuntimeException {
        public ParametersUriParseException() {
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

    protected BackendUiInteraction ui;

    public abstract void setOutputStream(@NonNull OutputStream stream);

    @NonNull
    public abstract OutputStream getOutputStream();

    public interface OnMessageListener {
        void onMessage(@NonNull Object msg);
    }

    public abstract void setOnMessageListener(OnMessageListener l);

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

    public static final class WakeLockRef {
        @NonNull
        private final WeakReference<BackendModule> ref;

        private WakeLockRef(@NonNull final BackendModule self) {
            this.ref = new WeakReference<>(self);
        }

        public boolean isHeld() {
            BackendModule self = ref.get();
            if (self == null) return false;
            return self.isWakeLockHeld();
        }

        public void acquire() {
            BackendModule self = ref.get();
            if (self == null) return;
            self.acquireWakeLock();
        }

        /**
         * @param timeout [ms]
         */
        public void acquire(final long timeout) {
            BackendModule self = ref.get();
            if (self == null) return;
            self.acquireWakeLock(timeout);
        }

        public void release() {
            BackendModule self = ref.get();
            if (self == null) return;
            self.releaseWakeLock();
        }
    }

    private final Object mWakeLockLock = new Object();
    private volatile PowerManager.WakeLock mWakeLock = null;
    private final Handler mWakeLockHandler = new Handler(Looper.getMainLooper());
    private final Runnable mWakeLockReleaser = new Runnable() {
        @Override
        public void run() {
            releaseWakeLock();
        }
    };
    private Runnable onWakeLockEvent = null;
    private Handler onWakeLockEventHandler = null;
    private final Object onWakeLockEventLock = new Object();

    private void setWakeLock(@NonNull final Context ctx) {
        final PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        synchronized (mWakeLockLock) {
            unsetWakeLock();
            final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    ctx.getPackageName() + ".BackendModule:");
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

    private boolean releaseWakeLockOnDisconnect = false;

    public boolean isReleaseWakeLockOnDisconnect() {
        return releaseWakeLockOnDisconnect;
    }

    public void setReleaseWakeLockOnDisconnect(final boolean v) {
        releaseWakeLockOnDisconnect = v;
    }
}
