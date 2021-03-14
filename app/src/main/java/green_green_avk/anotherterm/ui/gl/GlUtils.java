package green_green_avk.anotherterm.ui.gl;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class GlUtils {
    private GlUtils() {
    }

    @NonNull
    public static FloatBuffer makeDirectFloatBuffer(@NonNull final float[] c) {
        final FloatBuffer b =
                ByteBuffer.allocateDirect(Float.SIZE / Byte.SIZE * c.length)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(c);
        b.flip();
        return b;
    }
}
