package green_green_avk.wayland.server;

import androidx.annotation.NonNull;

import green_green_avk.wayland.protocol.wayland.wl_buffer;
import green_green_avk.wayland.protocol_core.WlInterface;

public final class WlBuffer extends wl_buffer {
    @NonNull
    public final WlShmPool pool;
    public final int offset;
    public final int width;
    public final int height;
    public final int stride;
    public final long format;

    public boolean lock() {
        locked = true;
        return valid;
    }

    public void unlock() {
        locked = false;
    }

    private volatile boolean locked = false;
    private volatile boolean valid = true;

    WlBuffer(@NonNull final WlShmPool pool, final int offset,
             final int width, final int height,
             final int stride, final long format) {
        this.pool = pool;
        this.offset = offset;
        this.width = width;
        this.height = height;
        this.stride = stride;
        this.format = format;
    }

    @NonNull
    WlBuffer makeResource(@NonNull final WlClient client,
                          @NonNull final WlInterface.NewId newId) {
        WlResource.make(client, this, newId.id, new Requests() {
            @Override
            public void destroy() {
                client.removeResourceAndNotify(id);
                WlBuffer.this.destroy();
            }
        }, () -> {
            valid = false;
            pool.removeBuffer(WlBuffer.this);
        });
        return this;
    }
}
