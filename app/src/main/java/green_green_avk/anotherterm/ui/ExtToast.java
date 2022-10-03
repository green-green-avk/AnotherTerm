package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.CheckResult;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public final class ExtToast {
    @NonNull
    private final ExtPopupWindow window;
    private final int[] pos = new int[2];
    private View ref = null;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private void calc() {
        if (ref == null)
            return;
        ref.getLocationInWindow(pos);
        pos[0] += ref.getWidth() / 2 - window.getContentView().getWidth() / 2;
        pos[1] += ref.getHeight() / 2 - window.getContentView().getHeight() / 2;
    }

    public ExtToast(@NonNull final Context context, @LayoutRes final int layoutRes) {
        @SuppressLint("InflateParams") final TextView wText = (TextView) LayoutInflater
                .from(context).inflate(layoutRes, null);
        wText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        window = new ExtPopupWindow(wText,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        window.setOnDismissListener(() -> {
            ref = null;
            uiHandler.removeCallbacksAndMessages(null);
        });
        window.setClippingEnabled(true);
        window.setSplitTouchEnabled(false);
        window.setTouchable(false);
        window.setAnimationStyle(android.R.style.Animation_Toast);
        wText.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            calc();
            window.update(pos[0], pos[1], -1, -1);
        });
    }

    public <T extends View> T getContentView() {
        return (T) window.getContentView();
    }

    @CheckResult
    @Nullable
    public CharSequence getText() {
        return ((TextView) window.getContentView()).getText();
    }

    public void setText(@Nullable final CharSequence v) {
        ((TextView) window.getContentView()).setText(v);
    }

    public void setText(@StringRes final int v) {
        ((TextView) window.getContentView()).setText(v);
    }

    public void show(@NonNull final View refView, final int delayMillis) {
        hide();
        ref = refView;
        calc();
        try {
            window.showAtLocation(refView, Gravity.NO_GRAVITY, pos[0], pos[1]);
        } catch (final WindowManager.BadTokenException e) {
            return; // Bad time: just skip
        }
        uiHandler.postDelayed(this::hide, delayMillis);
    }

    public void hide() {
        window.dismiss();
    }
}
