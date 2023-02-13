package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import green_green_avk.anotherterm.ui.drawables.CompoundDrawable;

public final class FileDrawableSource
        extends FileResourceSource<Function<? super Context, ? extends Drawable>> {
    @NonNull
    private final File dataDir;
    @NonNull
    private final File resDir;

    public FileDrawableSource(@NonNull final Context ctx, @NonNull final String name) {
        dataDir = new File(ctx.getApplicationInfo().dataDir);
        resDir = new File(dataDir, name);
        cache = new SubCache<Function<? super Context, ? extends Drawable>>(
                DrawableCache.instance) {
            @Override
            @NonNull
            protected Object onMapKey(@NonNull final Object itemKey) {
                return fromKey(itemKey);
            }
        };
        updateObservers();
    }

    private static final FileFilter filter = pathname -> !pathname.isDirectory();

    private final Cache<Function<? super Context, ? extends Drawable>> cache;

    @Override
    @NonNull
    protected Cache<Function<? super Context, ? extends Drawable>> getCache() {
        return cache;
    }

    @Nullable
    private File[] enumerateFiles() {
        try {
            return resDir.listFiles(filter);
        } catch (final SecurityException e) {
            return null;
        }
    }

    @NonNull
    private File fromKey(@NonNull final Object key) {
        return new File(resDir, URLEncoder.encode(key.toString()));
    }

    @NonNull
    private Object toKey(@NonNull final File path) {
        return URLDecoder.decode(path.getName());
    }

    @Override
    @NonNull
    public Set<Object> enumerate() {
        final File[] list = enumerateFiles();
        if (list == null) {
            return Collections.emptySet();
        }
        final Set<Object> r = new HashSet<>();
        for (final File file : list) {
            r.add(toKey(file));
        }
        return r;
    }

    @Override
    @NonNull
    protected File onPath(@NonNull final Object key) {
        return fromKey(key);
    }

    @Override
    @NonNull
    protected Function<? super Context, ? extends Drawable> onDecode(@NonNull final InputStream in)
            throws IOException {
        final Drawable r = CompoundDrawable.fromPng(in);
        return (ctx) -> CompoundDrawable.copy(ctx, r);
    }

    private void updateObservers() {
        if (!resDir.isDirectory()) {
            addFileObserver(dataDir);
            return;
        }
        addFileObserver(resDir);
        final File[] list = enumerateFiles();
        if (list == null) {
            return;
        }
        for (final File file : list) {
            addFileObserver(file);
        }
    }

    @Override
    protected void onManagedFilesChanged(@NonNull final Set<? extends File> changed) {
        boolean isChanged = false;
        for (final File file : changed) {
            if (resDir.equals(file.getParentFile())) {
                final Object key = toKey(file);
                invalidate(key);
                isChanged = true;
                callOnChanged(key);
            }
        }
        updateObservers();
        if (isChanged) {
            callOnChanged();
        }
    }
}
