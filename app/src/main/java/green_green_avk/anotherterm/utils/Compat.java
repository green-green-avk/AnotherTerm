package green_green_avk.anotherterm.utils;

import android.os.Build;

import androidx.annotation.NonNull;

import java.nio.CharBuffer;

public final class Compat {
    private Compat() {
    }

    /**
     * See:
     * <ul>
     * <li><a href="https://stackoverflow.com/questions/27460960/bug-when-using-characterbuffers-in-android">https://stackoverflow.com/questions/27460960/bug-when-using-characterbuffers-in-android</a></li>
     * <li><a href="https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip">https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip</a></li>
     * </ul>
     */
    @NonNull
    public static CharBuffer subSequence(@NonNull final CharBuffer that,
                                         final int start, final int end) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            return that.subSequence(start, end);
        return (CharBuffer) ((CharSequence) that).subSequence(start, end);
    }
}
