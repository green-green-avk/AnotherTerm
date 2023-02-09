package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;

public final class PackageDrawableSource
        extends PackageResourceSource<Function<? super Context, ? extends Drawable>> {
    public PackageDrawableSource(@NonNull final Context context,
                                 @NonNull final Enumerator enumerator) {
        super(context, enumerator);
    }

    @Override
    @NonNull
    protected Function<? super Context, ? extends Drawable> onLoad(@NonNull final Object key)
            throws Exception {
        if (!(key instanceof Key))
            throw new Resources.NotFoundException(key.toString());
        // Touching it for the enumeration purpose only
        final Drawable t = context.getPackageManager().getDrawable(
                ((Key) key).packageName,
                ((Key) key).resourceId,
                ((Key) key).applicationInfo
        );
        if (t == null)
            throw new Resources.NotFoundException(key.toString());
        return (ctx) -> {
            final Drawable r = ctx.getPackageManager().getDrawable(
                    ((Key) key).packageName,
                    ((Key) key).resourceId,
                    ((Key) key).applicationInfo
            );
            if (r == null)
                throw new Resources.NotFoundException(key.toString());
            return r;
        };
    }
}
