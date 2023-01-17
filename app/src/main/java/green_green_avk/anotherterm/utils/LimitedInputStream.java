package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public final class LimitedInputStream extends InputStream {
    @NonNull
    private final InputStream wrapped;
    private long limit;
    private long limitMark;
    private boolean isLimitHit = false;

    public LimitedInputStream(@NonNull final InputStream stream, final long limit) {
        wrapped = stream;
        this.limit = limit;
        limitMark = limit;
    }

    public boolean isLimitHit() {
        return isLimitHit;
    }

    public long getLimit() {
        return limit;
    }

    @Override
    public int read() throws IOException {
        if (limit <= 0) {
            isLimitHit = true;
            return -1;
        }
        final int r = wrapped.read();
        if (r < 0) {
            return r;
        }
        limit--;
        return r;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (limit <= 0) {
            isLimitHit = true;
            return -1;
        }
        final int r = wrapped.read(b, off, (int) Math.min(len, limit));
        if (r <= 0) {
            return r;
        }
        limit -= r;
        return r;
    }

    @Override
    public long skip(final long n) throws IOException {
        if (limit <= 0) {
            isLimitHit = true;
            return 0;
        }
        final long r = wrapped.skip(Math.min(n, limit));
        limit -= r;
        return r;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(wrapped.available(), limit);
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public synchronized void mark(final int readLimit) {
        wrapped.mark(readLimit);
        limitMark = limit;
    }

    @Override
    public synchronized void reset() throws IOException {
        wrapped.reset();
        limit = limitMark;
    }
}
