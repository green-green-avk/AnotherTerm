package green_green_avk.anotherterm;

import android.support.annotation.Nullable;

import java.io.Serializable;

public interface TermKeyMapRules {
    int getAppMode(int code);

    @Nullable
    String get(int code, int modifiers, int appMode);

    interface Editable extends TermKeyMapRules, Serializable {
        void setAppMode(int code, int appMode);

        void set(int code, int modifiers, int appMode, @Nullable String keyOutput);

//        void set(int code, int modifiersSet, int modifiersClr, int appMode, @Nullable String keyOutput);
    }
}
