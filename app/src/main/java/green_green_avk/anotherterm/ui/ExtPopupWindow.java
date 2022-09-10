package green_green_avk.anotherterm.ui;

import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * Nested popup windows simulation.
 * <p>
 * ({@link WindowManager.LayoutParams#FIRST_SUB_WINDOW} -
 * {@link WindowManager.LayoutParams#LAST_SUB_WINDOW} cannot be nested.)
 * <p>
 * Nested popups in dialogs are not supported.
 * <p>
 * Maybe it needs to be made like {@code com.android.internal.view.TooltipPopup}:
 * though there is no way to obtain the real application window view of the popup view,
 * it is still possible.
 */
public class ExtPopupWindow extends PopupWindow {
    @Nullable
    protected WeakReference<View> parentRootView = null;
    @Nullable
    protected PopupWindow.OnDismissListener onDismissListener = null;

    public ExtPopupWindow(@Nullable final View contentView,
                          final int width, final int height) {
        super(contentView, width, height);
    }

    public ExtPopupWindow(@Nullable final View contentView,
                          final int width, final int height,
                          final boolean focusable) {
        super(contentView, width, height, focusable);
    }

    @Override
    public void setOnDismissListener(@Nullable final OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void showAtLocation(@NonNull final View parent,
                               final int gravity, final int x, final int y) {
        if (isShowing())
            return;
        super.setOnDismissListener(null);
        try {
            super.showAtLocation(parent, gravity, x, y);
            parentRootView = null;
        } catch (final WindowManager.BadTokenException e) {
            try {
                dismiss(); // Yep, we need it
            } catch (final RuntimeException ignored) {
                // Very old Androids
            }
            parentRootView = new WeakReference<>(parent.getRootView());
            final int[] loc = new int[2];
            final View actView =
                    UiUtils.getWindowLocationInActivityWindow(loc, parent, x, y);
            super.showAtLocation(actView, gravity, loc[0], loc[1]);
        }
        super.setOnDismissListener(onDismissListener);
    }

    @Override
    public void update(final int x, final int y, final int width, final int height,
                       final boolean force) {
        if (parentRootView == null)
            super.update(x, y, width, height, force);
        else {
            final View parent = parentRootView.get();
            if (parent == null)
                throw new IllegalStateException("The root view of the parent window was expired");
            final int[] loc = new int[2];
            UiUtils.getWindowLocationInActivityWindow(loc, parent, x, y);
            super.update(loc[0], loc[1], width, height, force);
        }
    }
}
