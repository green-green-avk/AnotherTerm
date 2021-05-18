package green_green_avk.anotherterm;

import android.content.Context;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.apache.commons.io.input.ReaderInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.utils.Misc;

public final class ScratchpadManager {

    public static final class Entry {
        @NonNull
        public final String name;
        public final long timestamp;
        public final long size;

        private Entry(@NonNull final File file) {
            name = file.getName();
            timestamp = file.lastModified();
            size = file.length();
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return name.equals(((Entry) o).name);
        }
    }

    private final Context ctx;
    public final File location;
    public final String locationDesc;

    private final FileObserver observer;
    private final Handler mainHandler;
    private final Set<Runnable> listeners =
            Collections.newSetFromMap(new WeakHashMap<>());

    public ScratchpadManager(@NonNull final Context ctx, @NonNull final String dir) {
        this.ctx = ctx;
        mainHandler = new Handler(Looper.getMainLooper());
        location = new File(ctx.getApplicationInfo().dataDir, dir);
        locationDesc = "$DATA_DIR/" + dir + "/";
        try {
            if (location.exists() && !location.isDirectory())
                location.delete();
            location.mkdirs();
        } catch (final SecurityException ignored) {
        }
        observer = new FileObserver(location.getPath(),
                FileObserver.MOVED_FROM | FileObserver.MOVED_TO |
                        FileObserver.CREATE | FileObserver.DELETE |
                        FileObserver.DELETE_SELF | FileObserver.MOVE_SELF) {
            @Override
            public void onEvent(final int event, @Nullable final String path) {
                if ((event & FileObserver.ALL_EVENTS) == 0)
                    return;
                mainHandler.removeCallbacksAndMessages(null);
                for (final Runnable r : listeners)
                    mainHandler.post(r);
            }
        };
        observer.startWatching();
    }

    @Override
    protected void finalize() throws Throwable {
        observer.stopWatching();
        super.finalize();
    }

    @UiThread
    public void addListener(@NonNull final Runnable r) {
        listeners.add(r);
    }

    @NonNull
    public Set<Entry> enumerate() {
        final File[] list;
        try {
            list = location.listFiles();
        } catch (final SecurityException e) {
            return Collections.emptySet();
        }
        if (list == null) return Collections.emptySet();
        final Set<Entry> r = new HashSet<>();
        for (final File file : list) {
            try {
                r.add(new Entry(file));
            } catch (final SecurityException ignored) {
            }
        }
        return r;
    }

    @NonNull
    public Uri getUri(@NonNull final String name) throws FileNotFoundException {
        return Misc.getFileUri(ctx, new File(location, name));
    }

    @NonNull
    public String getDesc(@NonNull final String name, final int len) throws IOException {
        final File file = new File(location, name);
        final InputStream is;
        try {
            is = new FileInputStream(file);
        } catch (final SecurityException e) {
            throw new FileNotFoundException(e.getLocalizedMessage());
        }
        final byte[] desc = new byte[len];
        final int r;
        try {
            r = is.read(desc);
        } finally {
            try {
                is.close();
            } catch (final Throwable ignored) {
            }
        }
        if (r <= 0) return "";
        final String text = new String(desc, 0, r, Misc.UTF8);
        if (r < file.length())
            return ctx.getString(R.string.msg_s___, text);
        return text;
    }

    public void put(@NonNull final String name, @NonNull final String content) throws IOException {
        final OutputStream os;
        try {
            os = new FileOutputStream(new File(location, name));
        } catch (final SecurityException e) {
            throw new FileNotFoundException(e.getLocalizedMessage());
        }
        final InputStream is =
                new ReaderInputStream(new StringReader(content), Misc.UTF8);
        try {
            Misc.copy(os, is);
        } finally {
            try {
                os.close();
            } catch (final Throwable ignored) {
            }
        }
    }

    @NonNull
    public String put(@NonNull final String content) throws IOException {
        String name = UUID.randomUUID().toString() + ".txt";
        put(name, content);
        return name;
    }

    public void remove(@NonNull final String name) {
        try {
            new File(location, name).delete();
        } catch (final SecurityException ignored) {
        }
    }
}
