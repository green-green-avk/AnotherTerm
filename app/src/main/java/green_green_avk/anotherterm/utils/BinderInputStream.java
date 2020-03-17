package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.nio.ByteBuffer;

public final class BinderInputStream extends InputStream {
    private final ByteBuffer leftovers;
    private ByteBuffer boundBuffer = null;

    public BinderInputStream(final int capacity) {
        leftovers = ByteBuffer.allocate(capacity);
        leftovers.flip();
    }

    public void bind(@NonNull final ByteBuffer buf) {
        boundBuffer = buf;
    }

    public void release() {
        leftovers.compact();
        try {
            leftovers.put(boundBuffer);
        } finally {
            boundBuffer = null;
            leftovers.flip();
        }
    }

    @Override
    public int available() {
        return leftovers.remaining() + boundBuffer.remaining();
    }

    @Override
    public int read() {
        if (leftovers.remaining() > 0) return leftovers.get();
        if (boundBuffer.remaining() > 0) return boundBuffer.get();
        return -1;
    }

    private int get(@NonNull final byte[] b, final int off, final int len,
                    @NonNull final ByteBuffer buffer) {
        final int l = Math.min(len, buffer.remaining());
        System.arraycopy(buffer.array(), buffer.arrayOffset() + buffer.position(),
                b, off, l);
        buffer.position(buffer.position() + l);
        return l;
    }

    @Override
    public int read(@NonNull final byte[] b, final int off, final int len) {
        int r = get(b, off, len, leftovers);
        r += get(b, off + r, len - r, boundBuffer);
        return r;
    }

    @Override
    public long skip(final long n) {
        long r = n;
        long l = Math.min(r, leftovers.remaining());
        leftovers.position(leftovers.position() + (int) l);
        r -= l;
        l = Math.min(r, boundBuffer.remaining());
        boundBuffer.position(boundBuffer.position() + (int) l);
        r -= l;
        return n - r;
    }
}
