package green_green_avk.anotherterm.ui.forms;

import androidx.annotation.Nullable;

public abstract class ViewValueBinder<RESULT, VIEW_VALUE> {
    @Nullable
    protected abstract String onCheck(VIEW_VALUE v);

    public abstract RESULT get();

    public abstract int getId();
}
