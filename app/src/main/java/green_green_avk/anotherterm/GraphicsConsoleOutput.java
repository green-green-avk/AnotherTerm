package green_green_avk.anotherterm;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;

public interface GraphicsConsoleOutput {
    boolean getKeyAutorepeat();

    @AnyRes
    int getLayoutRes();

    /**
     * On key. (for IM)
     */
    void feed(int code, boolean shift, boolean alt, boolean ctrl);

    /**
     * On key down / up. (for X)
     */
    void feed(int code, boolean pressed);

    void feed(@NonNull String v);

    void paste(@NonNull String v);
}
