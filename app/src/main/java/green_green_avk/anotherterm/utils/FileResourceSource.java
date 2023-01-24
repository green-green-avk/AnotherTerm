package green_green_avk.anotherterm.utils;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class FileResourceSource<T> extends DynamicResourceSource<T> {
    @NonNull
    protected abstract File onPath(@NonNull Object key);

    @NonNull
    protected abstract T onDecode(@NonNull InputStream in) throws IOException;

    @Override
    @NonNull
    protected T onLoad(@NonNull final Object key) throws IOException {
        try (final FileInputStream stream = new FileInputStream(onPath(key))) {
            return onDecode(stream);
        }
    }

    private final Object updateSetLock = new Object();
    private final HashSet<File> updateSet = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateManaged = () -> {
        final Set<File> changed;
        synchronized (updateSetLock) {
            changed = (Set<File>) updateSet.clone();
            updateSet.clear();
        }
        onManagedFilesChanged(changed);
    };

    private final class ManagedFileObserver extends FileObserver {
        private final File dir;

        public ManagedFileObserver(@NonNull final String path, final int mask) {
            super(path, mask);
            dir = new File(path);
        }

        @Override
        public void onEvent(final int event, @Nullable final String path) {
            if ((event & FileObserver.ALL_EVENTS) != 0 && path != null) {
                mainHandler.removeCallbacks(updateManaged);
                synchronized (updateSetLock) {
                    updateSet.add(new File(dir, path));
                }
                mainHandler.post(updateManaged);
            }
        }
    }

    private final Collection<ManagedFileObserver> fileObservers = new ArrayList<>();

    protected final void clearFileObservers() {
        for (final ManagedFileObserver fo : fileObservers)
            fo.stopWatching();
        fileObservers.clear();
    }

    protected final void addFileObserver(@NonNull final File file) {
        final ManagedFileObserver fo = new ManagedFileObserver(file.getPath(),
                FileObserver.ATTRIB |
                        (file.isDirectory() ?
                                FileObserver.CREATE | FileObserver.DELETE |
                                        FileObserver.MOVED_FROM | FileObserver.MOVED_TO :
                                FileObserver.CLOSE_WRITE
                        )
        );
        fileObservers.add(fo);
        fo.startWatching();
    }

    protected abstract void onManagedFilesChanged(@NonNull Set<? extends File> changed);
}
