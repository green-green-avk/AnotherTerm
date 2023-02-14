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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
        private final File target;

        public ManagedFileObserver(@NonNull final File file, final int mask) {
            super(file.getPath(), mask);
            target = file;
        }

        @Override
        public void onEvent(final int event, @Nullable final String path) {
            if ((event & FileObserver.ALL_EVENTS) != 0) {
                switch (event) {
                    case FileObserver.DELETE_SELF:
                    case FileObserver.MOVE_SELF:
                        mainHandler.post(() -> removeFileObserver(target));
                        break;
                }
                mainHandler.removeCallbacks(updateManaged);
                synchronized (updateSetLock) {
                    updateSet.add(path != null ? new File(target, path) : target);
                }
                mainHandler.post(updateManaged);
            }
        }
    }

    private final Map<File, ManagedFileObserver> fileObservers = new HashMap<>();

    protected final void removeFileObserver(@NonNull final File file) {
        final ManagedFileObserver fo = fileObservers.remove(file);
        if (fo != null) {
            fo.stopWatching();
        }
    }

    protected final void addFileObserver(@NonNull final File file) {
        if (fileObservers.containsKey(file)) {
            return;
        }
        final ManagedFileObserver fo = new ManagedFileObserver(file,
                FileObserver.ATTRIB | FileObserver.DELETE_SELF | FileObserver.MOVE_SELF |
                        FileObserver.CREATE | FileObserver.DELETE |
                        FileObserver.MOVED_FROM | FileObserver.MOVED_TO |
                        FileObserver.CLOSE_WRITE
        );
        fo.startWatching();
        fileObservers.put(file, fo);
    }

    protected abstract void onManagedFilesChanged(@NonNull Set<? extends File> changed);
}
