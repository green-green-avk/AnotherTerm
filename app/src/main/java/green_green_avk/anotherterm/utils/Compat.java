package green_green_avk.anotherterm.utils;

import android.os.Build;

import androidx.annotation.NonNull;

import java.nio.CharBuffer;

public final class Compat {
    private Compat() {
    }

    /**
     * https://stackoverflow.com/questions/27460960/bug-when-using-characterbuffers-in-android
     * but without adding a Java 6 module.
     */
    public static CharBuffer subSequence(@NonNull final CharBuffer that,
                                         final int start, final int end) {
        if (Build.VERSION.SDK_INT >= 19) return that.subSequence(start, end);
        if (that.hasArray()) {
            if (end > that.remaining()) throw new IndexOutOfBoundsException("Bad end");
            if (start > end) throw new IndexOutOfBoundsException("Bad start");
            final int p = that.arrayOffset() + that.position();
            try {
                return CharBuffer.wrap(that.array(), start + p, end - start);
            } catch (final ArrayIndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException(e.getMessage());
            }
        }
        throw new IllegalArgumentException("Unable to handle native buffers for API < 19");
    }
}
