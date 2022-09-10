package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.R;

public final class ConfirmingImageButton extends androidx.appcompat.widget.AppCompatImageButton {
    @Nullable
    private OnClickListener onClickListener;
    @Nullable
    private CharSequence confirmationMessage;
    @Nullable
    private Drawable capDrawable = null;

    private int wrappingPhase = 0;

    public ConfirmingImageButton(@NonNull final Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ConfirmingImageButton(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ConfirmingImageButton(@NonNull final Context context, @Nullable final AttributeSet attrs,
                                 final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    private void init(final Context context, final AttributeSet attrs,
                      final int defStyleAttr, final int defStyleRes) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && onClickListener != null)
            onClickListener = VectorCompat.fixOnClickListener(this, attrs,
                    onClickListener);
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ConfirmingImageButton,
                defStyleAttr, defStyleRes);
        try {
            confirmationMessage =
                    a.getText(R.styleable.ConfirmingImageButton_confirmationMessage);
        } finally {
            a.recycle();
        }
        wrappingPhase = 1;
        UiUtils.wrapViewClickForConfirmation(this, () -> confirmationMessage,
                () -> {
                    if (onClickListener != null)
                        onClickListener.onClick(this);
                });
        wrappingPhase = 2;
        capDrawable = UiUtils.requireDrawable(context, R.drawable.bg_ball_ctl).mutate();
    }

    @Override
    public void setOnClickListener(@Nullable final OnClickListener onClickListener) {
        switch (wrappingPhase) {
            case 0:
                /*
                 * See
                 * {@link androidx.appcompat.app.AppCompatViewInflater#checkOnClickListener(View, AttributeSet)}
                 * source code for explanation.
                 *
                 * Using our own {@code android:onClick} wrapper in order to avoid enabling of
                 * {@link androidx.appcompat.app.AppCompatDelegate#setCompatVectorFromResourcesEnabled(boolean)}
                 */
                //super.setOnClickListener(onClickListener);
                this.onClickListener = onClickListener;
                break;
            case 1:
                super.setOnClickListener(onClickListener);
                break;
            default:
                this.onClickListener = onClickListener;
        }
    }

    @CheckResult
    @Nullable
    public CharSequence getConfirmationMessage() {
        return confirmationMessage;
    }

    public void setConfirmationMessage(@Nullable final CharSequence confirmationMessage) {
        this.confirmationMessage = confirmationMessage;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (capDrawable != null) {
            capDrawable.setBounds(getDrawable().getBounds());
            canvas.save();
            try {
                canvas.concat(getImageMatrix());
                capDrawable.draw(canvas);
            } finally {
                canvas.restore();
            }
        }
    }
}
