package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.ReturnThis;
import androidx.recyclerview.widget.RecyclerView;

import green_green_avk.anotherterm.ui.ProfileAdapter;
import green_green_avk.anotherterm.ui.UniAdapter;
import green_green_avk.anotherterm.ui.drawables.SquarePreviewDrawable;
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

    @Override
    @Nullable
    protected Drawable onGetPreview(@NonNull final ProfileManager.Meta meta) {
        final BackgroundProfile profile = getManager().get(meta);
        final Drawable preview = profile.getPreviewDrawable(context);
        if (preview != null) {
            return preview;
        }
        try {
            return new SquarePreviewDrawable(profile.getDrawable(context));
        } catch (final Exception e) {
            return null;
        }
    }

    @Override
    @Nullable
    protected CharSequence onGetTitle(@NonNull final ProfileManager.Meta meta) {
        final BackgroundProfile profile = getManager().get(meta);
        final CharSequence title = profile.getTitle(context);
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
