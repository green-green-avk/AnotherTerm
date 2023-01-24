package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.ReturnThis;
import androidx.recyclerview.widget.RecyclerView;

import green_green_avk.anotherterm.ui.ProfileAdapter;
import green_green_avk.anotherterm.ui.UniAdapter;
import green_green_avk.anotherterm.utils.ProfileManager;

public final class BackgroundsAdapter extends ProfileAdapter<BackgroundProfile> {
    public BackgroundsAdapter(@NonNull final Context context) {
        super(context);
    }

    @Override
    @NonNull
    protected BackgroundsManager getManager() {
        return BackgroundsManagerUi.instance.getManager(context);
    }

    @Override
    public void startEditor(@Nullable final String name) {
        // Not editable
    }

    @Override
    @ReturnThis
    public BackgroundsAdapter setEditorEnabled(final boolean v) {
        // Not editable
        return this;
    }

    private static final class PreviewDrawable extends Drawable {
        @NonNull
        private final Drawable drawable;

        private PreviewDrawable(@NonNull final Drawable drawable) {
            this.drawable = drawable;
        }

        private final Rect _bounds = new Rect();

        @Override
        public void draw(@NonNull final Canvas canvas) {
            drawable.copyBounds(_bounds);
            try {
                drawable.setBounds(getBounds());
                drawable.draw(canvas);
            } finally {
                drawable.setBounds(_bounds);
            }
        }

        @Override
        public int getIntrinsicWidth() {
            return Short.MAX_VALUE;
        }

        @Override
        public int getIntrinsicHeight() {
            return Short.MAX_VALUE;
        }

        @Override
        public void setAlpha(final int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable final ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return drawable.getOpacity();
        }
    }

    @Override
    @Nullable
    protected Drawable onGetPreview(@NonNull final ProfileManager.Meta meta) {
        final BackgroundProfile profile = getManager().get(meta);
        final Drawable preview = profile.getPreviewDrawable();
        return preview != null ? preview :
                new PreviewDrawable(getManager().get(meta).getDrawable());
    }

    @Override
    @Nullable
    protected CharSequence onGetTitle(@NonNull final ProfileManager.Meta meta) {
        final BackgroundProfile profile = getManager().get(meta);
        final CharSequence title = profile.getTitle();
        return title != null ? title : super.onGetTitle(meta);
    }

    @NonNull
    public static BackgroundsAdapter getSelf(@Nullable final Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }

    @NonNull
    public static BackgroundsAdapter getSelf(@Nullable final RecyclerView.Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }
}
