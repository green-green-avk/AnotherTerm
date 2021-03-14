package green_green_avk.wayland.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Proxy;

import green_green_avk.wayland.protocol_core.WlInterface;

public final class WlResource {
    private WlResource() {
    }

    @NonNull
    public static <RT extends WlInterface.Requests, ET extends WlInterface.Events>
    WlInterface<RT, ET> make(@NonNull final WlClient client,
                             @NonNull final WlInterface<RT, ET> object,
                             final int id,
                             @Nullable final WlInterface.Requests cbs,
                             @Nullable final WlInterface.OnDestroy onDestroy) {
        make(client, object, id);
        object.onDestroy = onDestroy;
        object.callbacks = cbs;
        return object;
    }

    @NonNull
    public static <RT extends WlInterface.Requests, ET extends WlInterface.Events>
    WlInterface<RT, ET> make(@NonNull final WlClient client,
                             @NonNull final WlInterface<RT, ET> object,
                             final int id) {
        final Class<? extends WlInterface> base = WlInterface.getBaseInterface(object.getClass());
        object.id = id;
        object.events = (ET) Proxy.newProxyInstance(
                WlInterface.class.getClassLoader(),
                new Class[]{WlInterface.getEvents(base)}, (proxy, method, args) -> {
                    client.send(object, method, args);
                    return null;
                });
        return object;
    }
}
