package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.lang.ref.WeakReference;

public final class DialogUtils {
    private DialogUtils() {
    }

    private static final class DialogLifecycleObserver
            implements DefaultLifecycleObserver {
        @NonNull
        private final WeakReference<Dialog> dialogRef;

        private DialogLifecycleObserver(@NonNull final Dialog dialog) {
            dialogRef = new WeakReference<>(dialog);
        }

        @Override
        public void onDestroy(@NonNull final LifecycleOwner owner) {
            final Dialog dialog = dialogRef.get();
            if (dialog != null)
                dialog.dismiss();
        }
    }

    public static void wrapLeakageSafe(@NonNull final Dialog dialog,
                                       @Nullable final Runnable onDismiss) {
        final Activity activity = UiUtils.getActivity(dialog.getContext());
        if (!(activity instanceof LifecycleOwner))
            throw new IllegalArgumentException("Underlying activity is not a LifecycleOwner");
        final Lifecycle lifecycle = ((LifecycleOwner) activity).getLifecycle();
        final DialogLifecycleObserver observer = new DialogLifecycleObserver(dialog);
        lifecycle.addObserver(observer);
        dialog.setOnDismissListener((_dialog) -> {
            lifecycle.removeObserver(observer);
            if (onDismiss != null)
                onDismiss.run();
        });
    }

    private static final class DialogViewStateListener
            implements View.OnAttachStateChangeListener {
        @NonNull
        private final WeakReference<Dialog> dialogRef;

        private DialogViewStateListener(@NonNull final Dialog dialog) {
            dialogRef = new WeakReference<>(dialog);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull final View v) {
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull final View v) {
            final Dialog dialog = dialogRef.get();
            if (dialog != null)
                dialog.dismiss();
        }
    }

    public static void wrapLeakageSafe(@NonNull final View view, @NonNull final Dialog dialog,
                                       @Nullable final Runnable onDismiss) {
        final DialogViewStateListener listener = new DialogViewStateListener(dialog);
        view.addOnAttachStateChangeListener(listener);
        dialog.setOnDismissListener((_dialog) -> {
            view.removeOnAttachStateChangeListener(listener);
            if (onDismiss != null)
                onDismiss.run();
        });
    }
}
