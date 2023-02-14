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
    public abstract View.DragShadowBuilder getDragShadowBuilder(@NonNull View stylingView);

    public boolean startDragAndDrop(@NonNull final View stylingView) {
        return ViewCompat.startDragAndDrop(stylingView, getData(),
                getDragShadowBuilder(stylingView),
                myLocalState, flags);
    }
}
