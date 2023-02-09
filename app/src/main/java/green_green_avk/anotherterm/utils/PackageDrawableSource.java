package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

public final class PackageDrawableSource extends PackageResourceSource<Callable<Drawable>> {
    public PackageDrawableSource(@NonNull final Context context,
                                 @NonNull final Enumerator enumerator) {
        super(context, enumerator);
    }

    @Override
    @NonNull
    protected Callable<Drawable> onLoad(@NonNull final Object key) throws Exception {
        if (!(key instanceof Key))
            throw new Resources.NotFoundException(key.toString());
        final PackageManager pm = context.getPackageManager();
        // Touching it for the enumeration purpose only
        final Drawable t = pm.getDrawable(
                ((Key) key).packageName,
                ((Key) key).resourceId,
                ((Key) key).applicationInfo
        );
        if (t == null)
            throw new Resources.NotFoundException(key.toString());
        return () -> {
            final Drawable r = pm.getDrawable(
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
