package green_green_avk.wayland.server;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import green_green_avk.wayland.os.WlEventHandler;
import green_green_avk.wayland.os.WlSocket;
import green_green_avk.wayland.protocol.wayland.wl_callback;
import green_green_avk.wayland.protocol.wayland.wl_display;
import green_green_avk.wayland.protocol.wayland.wl_registry;
import green_green_avk.wayland.protocol_core.WlErrorException;
import green_green_avk.wayland.protocol_core.WlInterface;
import green_green_avk.wayland.protocol_core.WlMarshalling;

public class WlClient {
    private static final int RESOURCES_MAX = 1024 * 1024;
    public static final int DISPLAY_ID = 1;
    @NonNull
    public final WlDisplay display;
    @NonNull
    public final WlSocket socket;
    @Nullable
    public final WlEventHandler sendHandler;
    @NonNull
    public final SparseArray<WlInterface> resources = new SparseArray<>();
    @NonNull
    private final WlInterface<wl_display.Requests, wl_display.Events> wlDisplayRes;
    @Nullable
    private WlInterface<wl_registry.Requests, wl_registry.Events> wlRegistryRes = null;

    @NonNull
    public <RT extends WlInterface.Requests, ET extends WlInterface.Events> WlInterface<RT, ET>
    addResource(@NonNull final WlInterface<RT, ET> resource) {
        if (resources.size() >= RESOURCES_MAX)
            throw new WlErrorException(wlDisplayRes, wl_display.Enums.Error.no_memory, "Too many resources");
        resources.put(resource.id, resource);
        return resource;
    }

    public void removeResource(final int id) {
        resources.remove(id);
    }

    public void removeResourceAndNotify(final int id) {
        wlDisplayRes.events.delete_id(id);
        removeResource(id);
    }

    public void returnCallback(@NonNull final WlInterface.NewId callback) {
        final WlInterface<wl_callback.Requests, wl_callback.Events> cb = new wl_callback();
        addResource(WlResource.make(WlClient.this, cb, callback.id,
                null, null));
        cb.events.done(WlClient.this.display.nextSerial());
        removeResourceAndNotify(cb.id);
    }

    public void returnError(@NonNull final WlInterface resource,
                            final long code, @Nullable final String message) {
        wlDisplayRes.events.error(resource, code, message != null ? message : "");
    }

    public void returnError(@NonNull final WlMarshalling.ParseException e) {
        returnError(wlDisplayRes, wl_display.Enums.Error.invalid_object, e.getMessage());
    }

    public void returnError(@NonNull final WlInterface resource, @NonNull final Exception e) {
        returnError(resource, wl_display.Enums.Error.invalid_method, e.getMessage());
    }

    public void returnError(@NonNull final Exception e) {
        returnError(wlDisplayRes, wl_display.Enums.Error.invalid_object, e.getMessage());
    }

    /**
     * Thread safe, call {@link #init()} on the common events thread to finish the initialization.
     *
     * @param display
     * @param socket
     */
    public WlClient(@NonNull final WlDisplay display, @NonNull final WlSocket socket,
                    @Nullable final WlEventHandler sendHandler) {
        this.display = display;
        this.socket = socket;
        this.sendHandler = sendHandler;
        wlDisplayRes = new wl_display();
        addResource(WlResource.make(this, wlDisplayRes, DISPLAY_ID, new wl_display.Requests() {
            @Override
            public void sync(@NonNull final WlInterface.NewId callback) {
                returnCallback(callback);
            }

            @Override
            public void get_registry(@NonNull final WlInterface.NewId registry) {
                final WlInterface<wl_registry.Requests, wl_registry.Events> reg = new wl_registry();
                wlRegistryRes = addResource(WlResource.make(WlClient.this, reg, registry.id,
                        new wl_registry.Requests() {
                            @Override
                            public void bind(final long name, @NonNull final WlInterface.NewId id) {
                                final WlGlobal g;
                                g = WlClient.this.display.globals.get((int) name);
                                if (g == null) {
                                    returnError(reg, wl_display.Enums.Error.invalid_object,
                                            "Global " + name + " does not exist");
                                    return;
                                }
                                final WlInterface<? extends WlInterface.Requests,
                                        ? extends WlInterface.Events> gr;
                                try {
                                    gr = g.bind(WlClient.this, id);
                                } catch (final WlGlobal.BindException e) {
                                    returnError(reg, wl_display.Enums.Error.invalid_object,
                                            "Global " + name + " cannot be bound: "
                                                    + e.getMessage());
                                    return;
                                }
                                addResource(gr);
                            }
                        }, null));
                for (int i = 0; i < WlClient.this.display.globals.size(); i++) {
                    final int name = WlClient.this.display.globals.keyAt(i);
                    final WlGlobal g = WlClient.this.display.globals.valueAt(i);
                    reg.events.global(name,
                            WlInterface.getName(g.getInterface()),
                            WlInterface.getVersion(g.getInterface()));
                }
            }
        }, null));
    }

    public void init() {
        display.clients.add(this);
    }

    public void destroy() {
        for (int i = 0; i < resources.size(); i++) {
            final WlInterface res = resources.valueAt(i);
            wlDisplayRes.events.delete_id(res.id);
            res.destroy();
        }
        resources.clear();
        display.clients.remove(this);
    }

    void onGlobal(final int name, @NonNull final WlGlobal global) {
        wlRegistryRes.events.global(name,
                WlInterface.getName(global.getInterface()),
                WlInterface.getVersion(global.getInterface()));
    }

    void onGlobalRemove(final int name) {
        wlRegistryRes.events.global_remove(name);
    }

    public void send(@NonNull final WlInterface resource,
                     @NonNull final Method method,
                     @NonNull final Object[] args) {
        final List<FileDescriptor> fds = new ArrayList<>(16);
        final ByteBuffer buffer = WlMarshalling.makeRPC(fds, resource, method, args);
        send(buffer, fds.toArray(new FileDescriptor[0]));
    }

    private void send(@NonNull final ByteBuffer buffer, @Nullable final FileDescriptor[] fds) {
        if (sendHandler != null) {
            sendHandler.post(() -> {
                try {
                    _send(buffer, fds);
                } catch (final IOException ignored) {
                }
            });
        } else {
            try {
                _send(buffer, fds);
            } catch (final IOException ignored) {
            }
        }
    }

    private void _send(@NonNull final ByteBuffer buffer, @Nullable final FileDescriptor[] fds)
            throws IOException {
        if (fds != null && fds.length > 0)
            socket.setFileDescriptorsForSend(fds);
        final OutputStream os = socket.getOutputStream();
        os.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        os.flush();
    }
}
