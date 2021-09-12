package green_green_avk.anotherterm.ui.forms;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class FormValidator {

    protected abstract void onRefresh();

    private final Set<Object> marks = Collections.newSetFromMap(new WeakHashMap<>());

    public final void updateMark(@NonNull final Object mark, final boolean set) {
        if (set)
            marks.add(mark);
        else
            marks.remove(mark);
        refresh();
    }

    public final boolean isValid() {
        return marks.isEmpty();
    }

    public void refresh() {
        onRefresh();
    }
}
