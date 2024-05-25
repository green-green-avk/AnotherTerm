package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.InfoActivity;
import green_green_avk.anotherterm.R;

public class InfoButton extends androidx.appcompat.widget.AppCompatImageButton {
    public InfoButton(@NonNull final Context context) {
        super(context);
    }

    public InfoButton(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InfoButton(@NonNull final Context context, @Nullable final AttributeSet attrs,
                      final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs) {
        final TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.InfoButton,
                        0, 0);
        try {
            setURL(a.getString(R.styleable.InfoButton_url));
        } finally {
            a.recycle();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setURL(@Nullable final String url) {
        if (url != null)
            setOnClickListener(w -> InfoActivity.show(w.getContext(), url));
        else
            setOnClickListener(null);
    }
}
