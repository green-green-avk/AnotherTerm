package green_green_avk.wayland.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

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

    private int refsCount = 0;
    private boolean valid = true;

    /**
     * To be called on the common events thread.
     *
     * @return underlying memory or {@code null} if already destroyed.
     * @throws IOException if cannot obtain due to underlying OS.
     */
    @Nullable
    public ByteBuffer lock() throws IOException {
        if (!valid)
            return null;
        final ByteBuffer r = pool.lock();
        if (r != null)
            refsCount++;
        return r;
    }

    /**
     * To be called on the common events thread.
     *
     * @return {@code true} if no more uses left.
     */
    public boolean unlock() {
        pool.unlock();
        refsCount--;
        if (refsCount < 0)
            throw new Error("Ouch!");
        return refsCount == 0;
    }

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
            pool.removeBuffer(this);
        });
        return this;
    }
}
