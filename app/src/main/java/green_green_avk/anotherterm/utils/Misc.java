package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import green_green_avk.anotherterm.App;
import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.C;

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
        for (long l = v.length; l != 0 && l < r.length; l <<= 1) {
            System.arraycopy(r, 0, r, (int) l,
                    (int) Math.min(l, r.length - l));
        }
        return r;
    }

    public static char[] repeat(@NonNull final char[] v, final int n) {
        if (n <= 0)
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        if (n == 1)
            return v;
        final char[] r = Arrays.copyOf(v, v.length * n);
        for (long l = v.length; l != 0 && l < r.length; l <<= 1) {
            System.arraycopy(r, 0, r, (int) l,
                    (int) Math.min(l, r.length - l));
        }
        return r;
    }

    public static void repeatFill(@NonNull final char[] dest, final int start, final int end,
                                  final int recordLength) {
        final int destLen = end - start;
        if (destLen <= recordLength)
            return;
        int len = recordLength;
        for (; len < destLen >> 1; len <<= 1) {
            System.arraycopy(dest, start, dest, start + len, len);
        }
        System.arraycopy(dest, start, dest, start + len,
                destLen - len);
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
            System.arraycopy(b, 0, buf, off,
                    Math.min(b.length, buf.length - off));
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
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseArray<?> o) {
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

    @NonNull
    public static Set<String> getLiveKeySet(@NonNull final SharedPreferences sp) {
        return new AbstractSet<String>() {
            @Override
            @NonNull
            public Iterator<String> iterator() {
                return sp.getAll().keySet().iterator();
            }

            @Override
            public int size() {
                return sp.getAll().size();
            }

            @Override
            public boolean contains(@Nullable final Object o) {
                return o instanceof String && sp.contains((String) o);
            }
        };
    }

    public static boolean isOrdered(@Nullable final Collection<?> v) {
        return v instanceof List || v instanceof Queue
                || v instanceof LinkedHashSet || v instanceof SortedSet
                || v instanceof LinkedHashMap || v instanceof SortedMap;
    }

    @NonNull
    public static <T> T requireNonNullElse(@Nullable final T v, @NonNull final T def) {
        return v != null ? v : def;
    }

    public static Integer[] box(final int[] v) {
        if (v == null)
            return null;
        final Integer[] r = new Integer[v.length];
        for (int i = 0; i < v.length; i++)
            r[i] = v[i];
        return r;
    }

    public static boolean equals(@NonNull final CharSequence a, @NonNull final char[] b) {
        if (a.length() != b.length)
            return false;
        for (int i = 0; i < b.length; i++) {
            if (a.charAt(i) != b[i])
                return false;
        }
        return true;
    }

    @NonNull
    public static char[] toArray(@NonNull final CharSequence v) {
        if (v instanceof String)
            return ((String) v).toCharArray();
        else if (v instanceof Password)
            return ((Password) v).toArray();
        final char[] r = new char[v.length()];
        if (v instanceof CharBuffer)
            ((CharBuffer) v).get(r);
        else
            TextUtils.getChars(v, 0, v.length(), r, 0);
        return r;
    }

    public static void erase(@NonNull final CharSequence v) {
        if (v instanceof Erasable) {
            ((Erasable) v).erase();
        } else if (v instanceof CharBuffer) {
            final CharBuffer bv = (CharBuffer) v;
            if (bv.hasArray()) {
                Arrays.fill(bv.array(), '\0');
            } else {
                bv.clear();
                bv.put(new char[bv.remaining()]);
                bv.clear();
            }
        } else if (BuildConfig.DEBUG) {
            Log.e(C.LOG_TAG_SECURITY, "Ouch! We can't erase some in-memory string!");
        }
    }

    @NonNull
    public static char[] toArrayAndErase(@NonNull final CharSequence v) {
        final char[] r = toArray(v);
        erase(v);
        return r;
    }

    public static boolean toBoolean(@Nullable final Object v) {
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        if (v instanceof Boolean) return (boolean) v;
        if (v instanceof Integer) return ((int) v) != 0;
        if (v instanceof Long) return ((long) v) != 0;
        if (v instanceof Float) return ((float) v) != 0;
        if (v instanceof Double) return ((Double) v) != 0;
        return false;
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

    public static void postOnMainThread(@NonNull final Runnable r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            new Handler(Looper.getMainLooper()).post(r);
        }
    }

    public interface LongUnaryOp {
        long apply(long v);
    }

    /**
     * A lightweight compat version of {@link AtomicLong#getAndUpdate(LongUnaryOperator)}.
     * <p>
     * <a href="https://developer.android.com/studio/write/java8-support#library-desugaring">Desugaring with additional lib / multidex</a>???
     * I implore!
     */
    public static long getAndUpdate(@NonNull final AtomicLong v,
                                    @NonNull final LongUnaryOp op) {
        long prev, next;
        do {
            prev = v.get();
            next = op.apply(prev);
            if (prev == next) {
                break;
            }
        } while (!v.compareAndSet(prev, next));
        return prev;
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
                        throw new AsyncError(e); // Yep, whack the main thread.
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

    /**
     * Rechecks against intent filters.
     *
     * @param ctx    context
     * @param intent to resolve against filters
     * @return see {@link android.content.pm.PackageManager#resolveActivity(Intent, int)}
     */
    @Nullable
    public static ResolveInfo resolveActivityAsImplicit(@NonNull final Context ctx,
                                                        @Nullable final Intent intent) {
        if (intent == null)
            return null;
        return ctx.getPackageManager()
                .resolveActivity(
                        intent.cloneFilter()
                                .setComponent(null).setPackage(ctx.getPackageName()),
                        PackageManager.MATCH_DEFAULT_ONLY
                );
    }

    @RequiresApi(23)
    @NonNull
    public static Set<String> checkSelfPermissions(@NonNull final Context ctx,
                                                   @NonNull final Iterable<String> perms) {
        final Set<String> r = new HashSet<>();
        for (final String perm : perms)
            if (ctx.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED)
                r.add(perm);
        return r;
    }

    @NonNull
    public static String[] getAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return Build.SUPPORTED_ABIS;
        else if (Build.CPU_ABI2 != null)
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        else
            return new String[]{Build.CPU_ABI};
    }
}
