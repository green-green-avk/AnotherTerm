package green_green_avk.wayland.server;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;

import green_green_avk.wayland.os.WlMmap;
import green_green_avk.wayland.protocol.wayland.wl_shm;
import green_green_avk.wayland.protocol_core.WlInterface;

public final class WlShm implements WlGlobal {
    @NonNull
    private final WlMmap mmap;
    @NonNull
    private final int[] extraFormats;

    public WlShm(@NonNull final WlMmap mmap, @NonNull final int[] extraFormats) {
        this.mmap = mmap;
        this.extraFormats = extraFormats;
    }

    @Override
    @NonNull
    public Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
    getInterface() {
        return wl_shm.class;
    }

    @Override
    @NonNull
    public WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
    bind(@NonNull final WlClient client, @NonNull final WlInterface.NewId newId)
            throws BindException {
        final WlInterface<wl_shm.Requests, wl_shm.Events> res =
                WlResource.make(client, new wl_shm(), newId.id, new wl_shm.Requests() {
                    @Override
                    public void create_pool(@NonNull final WlInterface.NewId id,
                                            @NonNull final FileDescriptor fd, final int size) {
                        client.addResource(new WlShmPool(mmap, fd, size)
                                .makeResource(client, id));
                    }
                }, null);
        res.events.format(wl_shm.Enums.Format.argb8888);
        res.events.format(wl_shm.Enums.Format.xrgb8888);
        for (final int fmt : extraFormats)
            res.events.format(fmt);
        return res;
    }
}
