package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import green_green_avk.anotherterm.App;
import green_green_avk.anotherterm.BuildConfig;

public final class Misc {
    private Misc() {
    }

    public static final Charset UTF8 = Charset.forName("UTF8");

    @NonNull
    public static String fromUTF8(@NonNull final byte[] buf) {
        return new String(buf, UTF8);
    }

    @NonNull
    public static byte[] toUTF8(@NonNull final String v) {
        return v.getBytes(UTF8);
    }

    @NonNull
    public static byte[] repeat(@NonNull final byte[] v, final int n) {
        if (n <= 0)
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        if (n == 1)
            return v;
        final byte[] r = Arrays.copyOf(v, v.length * n);
        for (long l = v.length; l != 0 && l < r.length; l <<= 1)
            System.arraycopy(r, 0, r, (int) l, (int) Math.min(l, r.length - l));
        return r;
    }

    public static void copy(@NonNull final OutputStream os, @NonNull final InputStream is)
            throws IOException {
        final byte[] buf = new byte[8192];
        while (true) {
            final int r = is.read(buf);
            if (r < 0) break;
            os.write(buf, 0, r);
        }
    }

    public interface CopyCallbacks {
        void onProgress(long copiedBytes) throws IOException;

        void onFinish() throws IOException;

        void onError() throws IOException;
    }

    public static void copy(@NonNull final OutputStream os, @NonNull final InputStream is,
                            @NonNull final CopyCallbacks callback, final int eachMillis)
            throws IOException {
        final byte[] buf = new byte[8192];
        long bytes = 0L;
        long ts = System.currentTimeMillis();
        try {
            while (true) {
                final int r = is.read(buf);
                if (r < 0) {
                    try {
                        callback.onProgress(bytes);
                        callback.onFinish();
                    } catch (final Throwable ignored) {
                    }
                    break;
                }
                os.write(buf, 0, r);
                bytes += r;
                final long _ts = System.currentTimeMillis();
                if (_ts >= ts + eachMillis) {
                    try {
                        callback.onProgress(bytes);
                    } catch (final Throwable ignored) {
                    }
                    ts = _ts;
                }
            }
        } catch (final IOException e) {
            try {
                callback.onProgress(bytes);
                callback.onError();
            } catch (final Throwable ignored) {
            }
            throw e;
        }
    }

    // IOUtils.toByteArray() from the Apache Commons IO
    // looks like something very strange in term of efficiency...
    @NonNull
    public static byte[] toArray(@NonNull final InputStream is,
                                 final long sizeLimit) throws IOException {
        final Queue<byte[]> q = new LinkedList<>();
        byte[] buf = new byte[4096];
        q.add(buf);
        int off = 0;
        int len = buf.length;
        int l;
        int total = 0;
        while ((l = is.read(buf, off, len)) >= 0) {
            off += l;
            len -= l;
            if (len == 0) {
                total += off;
                if (sizeLimit > 0 && total > sizeLimit)
                    throw new IOException(String.format("Content size exceeds %d bytes limit",
                            sizeLimit));
                buf = new byte[buf.length];
                q.add(buf);
                off = 0;
                len = buf.length;
            }
        }
        buf = new byte[(q.size() - 1) * buf.length + off];
        off = 0;
        for (final byte[] b : q) {
            System.arraycopy(b, 0, buf, off, Math.min(b.length, buf.length - off));
            off += b.length;
        }
        return buf;
    }

    private static final String fileProviderAuthority =
            BuildConfig.APPLICATION_ID + ".fileprovider";

    @NonNull
    public static Uri getFileUri(@NonNull final Context ctx, @NonNull final File file)
            throws FileNotFoundException {
        try {
            return FileProvider.getUriForFile(ctx, fileProviderAuthority, file);
        } catch (final IllegalArgumentException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @NonNull
    public static App getApplication(@NonNull Context ctx) {
        ctx = ctx.getApplicationContext();
        while (!(ctx instanceof App)) {
            if (ctx instanceof ContextWrapper)
                ctx = ((ContextWrapper) ctx).getBaseContext();
            else throw new ClassCastException("Unable to reach the application object");
        }
        return (App) ctx;
    }

    // Some sugar if boxing is affordable
    @NonNull
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseArray o) {
        return () -> new Iterator<Integer>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < o.size();
            }

            @Override
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return o.keyAt(i++);
            }
        };
    }

    @NonNull
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseBooleanArray o) {
        return () -> new Iterator<Integer>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < o.size();
            }

            @Override
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return o.keyAt(i++);
            }
        };
    }

    @NonNull
    public static Iterable<Integer> getTrueKeysIterable(@NonNull final SparseBooleanArray o) {
        return () -> new Iterator<Integer>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                while (i < o.size()) if (o.valueAt(i)) return true;
                else ++i;
                return false;
            }

            @Override
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return o.keyAt(i++);
            }
        };
    }

    @NonNull
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseIntArray o) {
        return () -> new Iterator<Integer>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < o.size();
            }

            @Override
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return o.keyAt(i++);
            }
        };
    }

    public static Integer[] box(final int[] v) {
        if (v == null)
            return null;
        final Integer[] r = new Integer[v.length];
        for (int i = 0; i < v.length; i++)
            r[i] = v[i];
        return r;
    }

    public static <T extends Comparable<T>> T clamp(@NonNull final T v, final T min, final T max) {
        if (v.compareTo(min) < 0) return min;
        if (v.compareTo(max) > 0) return max;
        return v;
    }

    public static int bitsAs(final int v, final int m, final int r) {
        return ((v & m) != 0) ? r : 0;
    }

    public static boolean bitsAs(final int v, final int m) {
        return (v & m) != 0;
    }

    public static void runOnThread(@NonNull final Runnable r) {
        new Thread(r).start();
    }

    public interface AsyncRunnable {
        @Nullable
        Object run();
    }

    public interface AsyncResult {
        void onResult(@Nullable Object v);
    }

    public static final class AsyncError extends Error {
        private AsyncError(final Throwable cause) {
            super(cause);
        }
    }

    public static void runAsync(@NonNull final AsyncRunnable r,
                                @NonNull final AsyncResult onResult) {
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(final Object... objects) {
                try {
                    return r.run();
                } catch (final Throwable e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        throw new AsyncError(e);
                    });
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Object o) {
                onResult.onResult(o);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void runAsyncWeak(@NonNull final AsyncRunnable r,
                                    @NonNull final AsyncResult onResult) {
        final WeakReference<AsyncResult> onResultRef = new WeakReference<>(onResult);
        runAsync(r, v -> {
            final AsyncResult _onResult = onResultRef.get();
            if (_onResult != null) _onResult.onResult(v);
        });
    }

    @RequiresApi(23)
    @NonNull
    public static Set<String> checkSelfPermissions(@NonNull final Context ctx,
                                                   @NonNull final String[] perms) {
        final Set<String> r = new HashSet<>();
        for (final String perm : perms)
            if (ctx.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) r.add(perm);
        return r;
    }

    @NonNull
    public static String[] getAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return Build.SUPPORTED_ABIS;
        else if (Build.CPU_ABI2 != null) return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        else return new String[]{Build.CPU_ABI};
    }
}
