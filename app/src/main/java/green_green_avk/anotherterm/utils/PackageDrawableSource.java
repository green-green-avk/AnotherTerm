package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.arch.core.util.Function;

import java.io.IOException;
import java.io.InputStream;

import green_green_avk.anotherterm.ui.drawables.CompoundDrawable;

public final class PackageDrawableSource
        extends PackageResourceSource<Function<? super Context, ? extends Drawable>> {
    public PackageDrawableSource(@NonNull final Context context,
                                 @NonNull final Enumerator enumerator) {
        super(context, enumerator);
    }

    @Override
    @NonNull
    protected Cache<Function<? super Context, ? extends Drawable>> getCache() {
        return DrawableCache.instance; // The keys are already unique - no sub-cache is required.
    }

    @NonNull
    private static Function<? super Context, ? extends Drawable> load(@NonNull final Context ctx,
                                                                      @NonNull final Key key) {
        final Resources res;
        try {
            res = ctx.getPackageManager().getResourcesForApplication(key.packageName);
        } catch (final PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException(key.toString());
        }
        final String type = res.getResourceTypeName(key.resourceId);
        switch (type) {
            case "drawable": {
                return (_ctx) -> {
                    final Context _resCtx;
                    try {
                        _resCtx = _ctx.createPackageContext(key.packageName,
                                Context.CONTEXT_RESTRICTED);
                    } catch (final PackageManager.NameNotFoundException e) {
                        throw new Resources.NotFoundException(key.toString());
                    }
                    final Drawable r =
                            AppCompatResources.getDrawable(_resCtx, key.resourceId);
                    if (r == null)
                        throw new Resources.NotFoundException(key.toString());
                    return r;
                };
            }
            case "raw": {
                final Drawable r;
                final InputStream stream = res.openRawResource(key.resourceId);
                try {
                    r = CompoundDrawable.fromPng(stream);
                } catch (final IOException e) {
                    throw new Resources.NotFoundException(key.toString());
                } finally {
                    try {
                        stream.close();
                    } catch (final IOException ignored) {
                    }
                }
                return (_ctx) -> CompoundDrawable.copy(_ctx, r);
            }
            default:
                throw new Resources.NotFoundException(key.toString());
        }
    }

    @Override
    @NonNull
    protected Function<? super Context, ? extends Drawable> onLoad(@NonNull final Object key)
            throws Exception {
        if (!(key instanceof Key))
            throw new Resources.NotFoundException(key.toString());
        final Function<? super Context, ? extends Drawable> r = load(context, (Key) key);
        // Touching it for the enumeration purpose only...
        // ... or not to touch?
        // TODO: Decide.
        // r.apply(context);
        return r;
    }
}
