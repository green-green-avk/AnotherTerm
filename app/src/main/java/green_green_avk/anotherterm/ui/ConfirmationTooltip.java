package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.R;

public final class ConfirmationTooltip {
    @NonNull
    private final View ownerView;
    @NonNull
    private final ExtPopupWindow window;
    @NonNull
    private final TextView tooltipView;

    private int offset = 0;

    private final Point position = new Point(0, 0);

    @SuppressLint("InflateParams")
    public ConfirmationTooltip(@NonNull final View ownerView) {
        final Context ctx = ownerView.getContext();
        this.ownerView = ownerView;
        tooltipView = (TextView) LayoutInflater.from(ctx)
                .inflate(R.layout.confirmation_tooltip, null);
        tooltipView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        window = new ExtPopupWindow(tooltipView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setClippingEnabled(true);
        window.setSplitTouchEnabled(false);
        window.setAnimationStyle(R.style.Animation_WheelPopup);
        tooltipView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            calcPos();
            window.update(position.x, position.y, -1, -1);
        });
    }

    @CheckResult
    public int getOffset() {
        return offset;
    }

    public void setOffset(final int v) {
        offset = v;
    }

    @CheckResult
    @Nullable
    public CharSequence getText() {
        return tooltipView.getText();
    }

    public void setText(@Nullable final CharSequence v) {
        tooltipView.setText(v);
    }

    public void show() {
        calcPos();
        window.showAtLocation(ownerView,
                Gravity.NO_GRAVITY, position.x, position.y);
    }

    public void hide() {
        window.dismiss();
    }

    private void calcPos() {
        final int[] lPos = new int[2];
        ownerView.getLocationInWindow(lPos);
        final int[] gPos = new int[2];
        final View gView = UiUtils.getWindowLocationInActivityWindow(gPos, ownerView,
                ownerView.getWidth() / 2, ownerView.getHeight() / 2);
        lPos[0] += ownerView.getWidth() / 2;
        lPos[1] += ownerView.getHeight() / 2;
        gPos[0] += lPos[0];
        gPos[1] += lPos[1];
        final int cX = gView.getWidth() / 2;
        final int cY = gView.getHeight() / 2;
        final int off = Math.round(offset * 0.707F);
        if (gPos[0] < cX) {
            position.x = lPos[0] + off;
        } else {
            position.x = lPos[0] - off - tooltipView.getWidth();
        }
        if (gPos[1] < cY) {
            position.y = lPos[1] + off;
        } else {
            position.y = lPos[1] - off - tooltipView.getHeight();
        }
    }
}
