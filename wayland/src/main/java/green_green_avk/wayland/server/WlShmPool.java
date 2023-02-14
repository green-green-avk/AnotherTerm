package green_green_avk.wayland.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.wayland.os.WlMmap;
import green_green_avk.wayland.protocol.wayland.wl_shm_pool;
import green_green_avk.wayland.protocol_core.WlInterface;

public final class WlShmPool extends wl_shm_pool {
    @NonNull
    private final WlMmap mmap;
    private final Object mmapLock = new Object();
    @Nullable
    private volatile FileDescriptor fd;
    private volatile boolean fdExpired = false;
    private volatile int size;
    private volatile int newSize; // for delayed resize
    public volatile ByteBuffer mem = null;
    private final Set<WlBuffer> buffers = Collections.newSetFromMap(new WeakHashMap<>());
    private volatile int refsCount = 0;

    // thread safe
    public void lock() throws IOException {
        synchronized (mmapLock) {
            refsCount++;
            if (mem == null) {
                if (fd == null)
                    throw new Error("Ouch!");
                mem = mmap.mmap(fd, size);
            }
        }
    }

    // thread safe
    public void unlock() {
        synchronized (mmapLock) {
            refsCount--;
            if (refsCount < 0)
                throw new Error("Houston, we have a problem...");
            tryUnmap();
            tryRemap();
            tryCloseFd();
        }
    }

    private void tryCloseFd() {
        if (fd != null && fdExpired && size == newSize && buffers.isEmpty()) {
            mmap.close(fd);
            fd = null;
        }
    }

    private void tryUnmap() {
        if (fdExpired && refsCount == 0 && buffers.isEmpty()) {
            if (mem != null) {
                mmap.munmap(mem);
                mem = null;
            }
        }
    }

    private void tryRemap() {
        if (size != newSize && fd != null && refsCount == 0) {
            if (mem != null) {
                mmap.munmap(mem);
                mem = null;
            }
            size = newSize;
        }
    }

    private WlBuffer addBuffer(@NonNull final WlBuffer buffer) {
        synchronized (mmapLock) {
            buffers.add(buffer);
            return buffer;
        }
    }

    void removeBuffer(@NonNull final WlBuffer buffer) {
        synchronized (mmapLock) {
            buffers.remove(buffer);
            tryUnmap();
            tryRemap();
            tryCloseFd();
        }
    }

    WlShmPool(@NonNull final WlMmap mmap, @NonNull final FileDescriptor fd, final int size) {
        this.mmap = mmap;
        this.fd = fd;
        this.size = size;
        this.newSize = size;
    }

    @NonNull
    WlShmPool makeResource(@NonNull final WlClient client,
                           @NonNull final WlInterface.NewId newId) {
        WlResource.make(client, this, newId.id, new Requests() {
            @Override
            public void create_buffer(@NonNull final NewId id, final int offset,
                                      final int width, final int height,
                                      final int stride, final long format) {
                client.addResource(addBuffer(new WlBuffer(WlShmPool.this, offset,
                        width, height, stride, format).makeResource(client, id)));
            }

            @Override
            public void destroy() {
                client.removeResourceAndNotify(id);
                WlShmPool.this.destroy();
            }

            @Override
            public void resize(final int size) {
                synchronized (mmapLock) {
                    newSize = size;
                    tryRemap();
                }
            }
        }, () -> {
            synchronized (mmapLock) {
                fdExpired = true;
                tryUnmap();
                tryRemap();
                tryCloseFd();
            }
        });
        return this;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mem != null)
            mmap.munmap(mem);
        if (fd != null)
            mmap.close(fd);
        super.finalize();
    }
}
