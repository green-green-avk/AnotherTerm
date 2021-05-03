package green_green_avk.anotherterm;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IConsoleOutput {
    boolean getKeyAutorepeat();

    boolean getStickyModifiersEnabled();

    @AnyRes
    int getLayoutRes();

    @Nullable
    String getKeySeq(int code, boolean shift, boolean alt, boolean ctrl);

    /**
     * On key. (for ANSI)
     */
    void feed(int code, boolean shift, boolean alt, boolean ctrl);

    /**
     * On key down / up. (for X)
     */
    void feed(int code, boolean pressed);

    void feed(@NonNull String v);

    void paste(@NonNull String v);
}
