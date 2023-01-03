package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.lang.ref.WeakReference;

public final class DialogUtils {
    private DialogUtils() {
    }

    private static final class DialogLifecycleObserver implements DefaultLifecycleObserver {
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
        if (!(activity instanceof AppCompatActivity))
            throw new IllegalStateException("Underlying activity is not an AppCompatActivity");
        final Lifecycle lifecycle = ((AppCompatActivity) activity).getLifecycle();
        final DialogLifecycleObserver observer = new DialogLifecycleObserver(dialog);
        lifecycle.addObserver(observer);
        dialog.setOnDismissListener((_dialog) -> {
            lifecycle.removeObserver(observer);
            if (onDismiss != null)
                onDismiss.run();
        });
    }
}
