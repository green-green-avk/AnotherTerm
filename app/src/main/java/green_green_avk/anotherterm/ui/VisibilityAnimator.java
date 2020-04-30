package green_green_avk.anotherterm.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import androidx.annotation.NonNull;

public final class VisibilityAnimator {
    @NonNull
    private final View view;
    private int state;
    private final float alpha;

    public VisibilityAnimator(@NonNull final View view) {
        this.view = view;
        this.state = view.getVisibility();
        this.alpha = view.getAlpha();
    }

    public void setVisibility(final int s) {
        if (state == s) return;
        if (s == View.VISIBLE) {
            view.setAlpha(0F);
            view.animate().alpha(alpha).setListener(null);
            view.setVisibility(s);
        } else {
            view.animate().alpha(0F).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(s);
                }
            });
        }
        state = s;
    }
}
