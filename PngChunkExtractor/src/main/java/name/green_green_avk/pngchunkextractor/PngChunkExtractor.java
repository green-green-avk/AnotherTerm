package name.green_green_avk.pngchunkextractor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public final class PngChunkExtractor {
    public abstract static class BaseException extends RuntimeException {
        private BaseException() {
        }

        private BaseException(final Throwable cause) {
            super(cause);
        }
    }

    public static final class NotPngException extends BaseException {
        private NotPngException() {
        }
    }

    public abstract static class ChunkException extends BaseException {
        public final int offset;

        private ChunkException(final int offset) {
            this.offset = offset;
        }

        private ChunkException(final Throwable cause, final int offset) {
            super(cause);
            this.offset = offset;
        }

        @Override
        @Nullable
        public String getMessage() {
            return "Chunk parsing failed at offset " + offset + ": " + super.getMessage();
        }
    }

    public static final class BadChunkCrcException extends ChunkException {
        private BadChunkCrcException(final int offset) {
            super(offset);
        }
    }

    public static final class ChunkProcessingException extends ChunkException {
        private ChunkProcessingException(final Throwable cause, final int offset) {
            super(cause, offset);
        }
    }

    public interface Callbacks {
        boolean filter(int chunkType);

        void process(int chunkType, @NonNull byte[] data);
    }

    private static final byte[] MAGIC =
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @NonNull
    private final Callbacks callbacks;
    @NonNull
    private final InputStream inputStream;

    @Nullable
    private BaseException exception = null;
    private int currentChunkOffset = 0;
    private int currentChunkLength = MAGIC.length;

    private int state = -4;

    private int inHeader() {
        return state <= -4 ? state + 12 : 0;
    }

    private boolean inHeaderEnd() {
        return state == -12;
    }

    private int inBody() {
        return state;
    }

    private int inCrc() {
        return state + 4;
    }

    private boolean inCrcEnd() {
        return state == -4;
    }

    private final byte[] currentHeader = new byte[8];
    private byte[] currentBody = null;
    private final byte[] currentCrc = new byte[4];
    private final CRC32 currentCrcValue = new CRC32();

    private int magicState = MAGIC.length;

    private static int getBeInt(@NonNull final byte[] v, final int offset) {
        return v[offset + 3] & 0xFF | (v[offset + 2] & 0xFF) << 8 |
                (v[offset + 1] & 0xFF) << 16 | v[offset] << 24;
    }

    private void consumeByte(final byte v) {
        if (exception != null)
            return;
        if (magicState > 0) {
            if (MAGIC[MAGIC.length - magicState] != v) {
                exception = new NotPngException();
            } else {
                magicState--;
                if (magicState == 0) {
                    currentChunkOffset = currentChunkLength;
                    currentChunkLength = -1; // Unknown yet
                }
            }
            return;
        }
        if (inHeader() > 0) {
            currentHeader[currentHeader.length - inHeader()] = v;
            state--;
            if (inHeaderEnd()) {
                state = getBeInt(currentHeader, 0);
                currentChunkLength = state;
                if (callbacks.filter(getBeInt(currentHeader, 4))) {
                    currentBody = new byte[state];
                }
                currentCrcValue.update(currentHeader, 4, 4);
            }
            return;
        }
        if (inBody() > 0) {
            if (currentBody != null) {
                currentBody[currentBody.length - inBody()] = v;
            }
            currentCrcValue.update(v);
            state--;
            return;
        }
        currentCrc[currentCrc.length - inCrc()] = v;
        state--;
        if (inCrcEnd()) {
            if (getBeInt(currentCrc, 0) != (int) currentCrcValue.getValue()) {
                exception = new BadChunkCrcException(currentChunkOffset);
            } else if (currentBody != null) {
                try {
                    callbacks.process(getBeInt(currentHeader, 4), currentBody);
                } catch (final Throwable e) {
                    exception = new ChunkProcessingException(e, currentChunkOffset);
                }
            }
            currentBody = null;
            currentCrcValue.reset();
            currentChunkOffset += currentChunkLength;
            currentChunkLength = -1; // Unknown yet
        }
    }

    private final InputStream outputStream = new InputStream() {
        @Override
        public int read() throws IOException {
            final int v = inputStream.read();
            if (v >= 0) {
                consumeByte((byte) v);
            }
            return v;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            final int bytesRead = inputStream.read(b, off, len);
            if (bytesRead > 0) {
                for (int i = 0; i < bytesRead; i++) {
                    consumeByte(b[i + off]);
                }
            }
            return bytesRead;
        }
    };

    public PngChunkExtractor(@NonNull final Callbacks callbacks,
                             @NonNull final InputStream inputStream) {
        this.callbacks = callbacks;
        this.inputStream = inputStream;
    }

    /**
     * @return how we failed in the {@link Callbacks#process(int, byte[])} method
     */
    @Nullable
    public BaseException getException() {
        return exception;
    }

    /**
     * @return where we are now (or stopped parsing)
     */
    public int getCurrentChunkOffset() {
        return currentChunkOffset;
    }

    @NonNull
    public InputStream getStream() {
        return outputStream;
    }
}
