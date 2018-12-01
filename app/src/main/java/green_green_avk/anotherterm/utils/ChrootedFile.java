package green_green_avk.anotherterm.utils;

import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import green_green_avk.ptyprocess.PtyProcess;

public final class ChrootedFile {
    public interface Ops {
        ParcelFileDescriptor open(String path, int flags) throws IOException;
    }

    private final Ops ops;

    private final String path;

    private String originalPath = null;

    public ChrootedFile(@NonNull final Ops ops, @NonNull final String pathname) {
        path = pathname;
        this.ops = ops;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    @Nullable
    public String getParentPath() {
        return new File(getPath()).getParent();
    }

    @Nullable
    public ChrootedFile getParent() {
        final String pp = getParentPath();
        return pp == null ? null : new ChrootedFile(ops, getParentPath());
    }

    @NonNull
    public ChrootedFile getChild(@NonNull final String name) {
        return new ChrootedFile(ops, getPath() + "/" + name);
    }

    public void refresh() {
        originalPath = null;
    }

    @NonNull
    private ParcelFileDescriptor openFd(final boolean create) throws IOException {
        return ops.open(getPath(), create
                ? PtyProcess.O_WRONLY | PtyProcess.O_CREAT
                : PtyProcess.O_PATH);
    }

    @NonNull
    private String readOriginalPath(final boolean create) throws IOException {
        final ParcelFileDescriptor fd = openFd(create);
        try {
            return PtyProcess.getPathByFd(fd.getFd());
        } finally {
            try {
                fd.close();
            } catch (final IOException ignored) {
            }
        }
    }

    @NonNull
    private String getOriginalPath(final boolean create) throws IOException {
        if (originalPath == null) originalPath = readOriginalPath(create);
        return originalPath;
    }

    @NonNull
    public String getOriginalPath() throws IOException {
        return getOriginalPath(false);
    }

    @NonNull
    public File getOriginalFile() throws IOException {
        return new File(getOriginalPath());
    }

    @NonNull
    public ChrootedFile create() throws IOException {
        getOriginalPath(true);
        return this;
    }

    public boolean canRead() {
        try {
            return getOriginalFile().canRead();
        } catch (final IOException e) {
            return false;
        }
    }

    public boolean canWrite() {
        try {
            return getOriginalFile().canWrite();
        } catch (final IOException e) {
            return false;
        }
    }

    public boolean exists() {
        try {
            getOriginalPath();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    public boolean isDirectory() {
        try {
            return getOriginalFile().isDirectory();
        } catch (final IOException e) {
            return false;
        }
    }

    public boolean isFile() {
        try {
            return getOriginalFile().isFile();
        } catch (final IOException e) {
            return false;
        }
    }
}
