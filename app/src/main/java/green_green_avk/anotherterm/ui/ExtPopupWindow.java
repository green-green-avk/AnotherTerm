package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

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
    private final LifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onDestroy(@NonNull final LifecycleOwner owner) {
            dismiss();
        }
    };
    private WeakReference<Lifecycle> lifecycleRef = new WeakReference<>(null);
    protected PopupWindow.OnDismissListener onDismissListenerWrapper = () -> {
        if (onDismissListener != null)
            onDismissListener.onDismiss();
        final Lifecycle lifecycle = lifecycleRef.get();
        if (lifecycle != null)
            lifecycle.removeObserver(lifecycleObserver);
    };

    protected final void setOnActivityDestroy(@NonNull final View ref) {
        final Activity activity = UiUtils.getActivity(ref.getContext());
        if (!(activity instanceof AppCompatActivity))
            throw new IllegalStateException("Underlying activity is not an AppCompatActivity");
        final Lifecycle lifecycle = ((AppCompatActivity) activity).getLifecycle();
        lifecycleRef = new WeakReference<>(lifecycle);
        lifecycle.addObserver(lifecycleObserver);
    }

    public ExtPopupWindow(@NonNull final Context context) {
        super(context);
    }

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
    public void dismiss() {
        try {
            super.dismiss();
        } catch (final IllegalArgumentException ignored) {
            // "not attached to window manager" exception in API 22 at least.
            // It is silenced in new Androids.
        }
    }

    @Override
    public void showAtLocation(@NonNull final View parent,
                               final int gravity, final int x, final int y) {
        if (isShowing())
            return;
        setOnActivityDestroy(parent);
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
            try {
                super.showAtLocation(actView, gravity, loc[0], loc[1]);
            } catch (final WindowManager.BadTokenException e2) {
                try {
                    dismiss(); // Restoring the window state
                } catch (final RuntimeException ignored) {
                    // Very old Androids
                }
                parentRootView = null;
                throw e2;
            }
        }
        super.setOnDismissListener(onDismissListenerWrapper);
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
