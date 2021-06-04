package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class FixedImageSpan extends DynamicDrawableSpan {
    @NonNull
    protected final Drawable mDrawable;

    public FixedImageSpan(@NonNull final Drawable drawable) {
        super();
        mDrawable = drawable;
        init();
    }

    public FixedImageSpan(@NonNull final Drawable drawable, final int verticalAlignment) {
        super(verticalAlignment);
        mDrawable = drawable;
        init();
    }

    public FixedImageSpan(@NonNull final Context ctx, @DrawableRes final int res) {
        this(UiUtils.requireDrawable(ctx, res));
    }

    public FixedImageSpan(@NonNull final Context ctx, @DrawableRes final int res,
                          final int verticalAlignment) {
        this(UiUtils.requireDrawable(ctx, res), verticalAlignment);
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    protected void init() {
        mDrawable.mutate().setBounds(0, 0,
                mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
    }
}
