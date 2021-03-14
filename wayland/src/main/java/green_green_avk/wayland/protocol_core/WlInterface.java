package green_green_avk.wayland.protocol_core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

public abstract class WlInterface<RT extends WlInterface.Requests,
        ET extends WlInterface.Events> {
    public static final class NewId {
        @NonNull
        public final String interfaceName;
        public final int version;
        public final int id;

        public NewId(@NonNull final String interfaceName, final int version, final int id) {
            this.interfaceName = interfaceName;
            this.version = version;
            this.id = id;
        }

        public NewId(@NonNull final Class<? extends WlInterface> interfaceNameClass,
                     final int version, final int id) {
            this(getName(interfaceNameClass), version, id);
        }

        public NewId(final int id) {
            this("", 0, id);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface IMethod {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface IDtor {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ISince {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface INullable {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Iface {
        Class<? extends WlInterface<? extends Requests, ? extends Events>> value();
    }

    public interface Callbacks {
    }

    public interface Requests extends Callbacks {
    }

    public interface Events extends Callbacks {
    }

    @NonNull
    public static String getName(@NonNull final Class<? extends WlInterface> v) {
        return v.getSimpleName();
    }

    public static int getVersion(@NonNull final Class<? extends WlInterface> v) {
        return (int) getField(v, "version");
    }

    @NonNull
    public static Class<Requests> getRequests(@NonNull final Class<? extends WlInterface> v) {
        try {
            return (Class<Requests>) getDeclarations(v, "Requests");
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException("Bad class", e);
        }
    }

    @NonNull
    public static Class<Events> getEvents(@NonNull final Class<? extends WlInterface> v) {
        try {
            return (Class<Events>) getDeclarations(v, "Events");
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException("Bad class", e);
        }
    }

    @Nullable
    public static Method getMethod(@NonNull final Class<? extends Callbacks> c, final int id) {
        for (final Method m : getBaseCallbacks(c).getMethods()) {
            final IMethod ann = m.getAnnotation(IMethod.class);
            if (ann == null) continue;
            if (ann.value() != id) continue;
            return m;
        }
        return null;
    }

    public static boolean isParamNullable(@NonNull final Method method, final int param) {
        final Annotation[] pas = method.getParameterAnnotations()[param];
        for (final Annotation pa : pas)
            if (pa instanceof INullable)
                return true;
        return false;
    }

    @Nullable
    public static Class<? extends WlInterface<? extends Requests, ? extends Events>>
    getParamInterface(@NonNull final Method method, final int param) {
        final Annotation[] pas = method.getParameterAnnotations()[param];
        for (final Annotation pa : pas)
            if (pa instanceof Iface)
                return ((Iface) pa).value();
        return null;
    }

    private static Object
    getField(@NonNull final Class<? extends WlInterface> v, @NonNull final String name) {
        try {
            return v.getDeclaredField(name).get(null);
        } catch (final NoSuchFieldException | NullPointerException | SecurityException
                | IllegalAccessException | ExceptionInInitializerError e) {
            throw new IllegalArgumentException("Bad class", e);
        }
    }

    @NonNull
    private static Class<? extends Callbacks>
    getDeclarations(@NonNull final Class<? extends WlInterface> v, @NonNull final String name) {
        try {
            for (final Class<?> c : v.getDeclaredClasses()) {
                if (name.equals(c.getSimpleName()))
                    return (Class<Callbacks>) c;
            }
        } catch (final SecurityException | ClassCastException e) {
            throw new IllegalArgumentException("Bad class", e);
        }
        throw new IllegalArgumentException("Bad class");
    }

    @NonNull
    private static Class<? extends Callbacks>
    getBaseCallbacks(@NonNull final Class<? extends Callbacks> v) {
        final Class<?>[] ifs = v.getInterfaces();
        for (final Class<?> i : ifs) {
            final Class<?>[] ii = i.getInterfaces();
            if (ii.length == 1 && (ii[0] == Requests.class || ii[0] == Events.class))
                return (Class<? extends Callbacks>) i;
        }
        throw new IllegalArgumentException();
    }

    @NonNull
    public static Class<? extends WlInterface>
    getBaseInterface(@NonNull final Class<? extends WlInterface> v) {
        Class<?> c = v;
        while (c != null) {
            final Class<?> s = c.getSuperclass();
            if (s == WlInterface.class)
                return (Class<? extends WlInterface>) c;
            c = s;
        }
        throw new IllegalArgumentException();
    }

    // Implementation

    public interface OnDestroy {
        void onDestroy();
    }

    public int id = 0;
    public RT requests = null;
    public ET events = null;
    public Callbacks callbacks = null;
    public OnDestroy onDestroy = null;

    public void destroy() {
        if (onDestroy != null)
            onDestroy.onDestroy();
        id = 0;
    }

    public boolean isDestroyed() {
        return id == 0;
    }
}
