package green_green_avk.anotherterm.ui.dragndrop;

import android.content.ClipData;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

public abstract class DragSpan {
    @Nullable
    public abstract ClipData getData();

    @Nullable
    public Object myLocalState = null;
    public int flags;

    @NonNull
    public abstract View.DragShadowBuilder getDragShadow(@NonNull View view);

    public boolean startDragAndDrop(@NonNull final View view) {
        return ViewCompat.startDragAndDrop(view, getData(),
                getDragShadow(view),
                myLocalState, flags);
    }
}
