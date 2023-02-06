package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;

public final class PackageDrawableSource extends PackageResourceSource<Drawable> {
    public PackageDrawableSource(@NonNull final Context context,
                                 @NonNull final Enumerator enumerator) {
        super(context, enumerator);
    }

    @Override
    @NonNull
    protected Drawable onLoad(@NonNull final Object key) throws IOException {
        if (!(key instanceof Key))
            throw new FileNotFoundException(key.toString());
        final PackageManager pm = context.getPackageManager();
        final Drawable r = pm.getDrawable(
                ((Key) key).packageName,
                ((Key) key).resourceId,
                ((Key) key).applicationInfo
        );
        if (r == null)
            throw new FileNotFoundException(key.toString());
        return r;
    }
}
