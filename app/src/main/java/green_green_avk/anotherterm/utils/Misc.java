package green_green_avk.anotherterm.utils;

import android.os.Build;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class Misc {
    public static final Charset UTF8 = Charset.forName("UTF8");

    private Misc() {
    }

    @NonNull
    public static String fromUTF8(@NonNull final byte[] buf) {
        return new String(buf, UTF8);
    }

    @NonNull
    public static byte[] toUTF8(@NonNull final String v) {
        return v.getBytes(UTF8);
    }

    public static void copy(@NonNull final OutputStream os,
                            @NonNull final InputStream is) throws IOException {
        final byte[] buf = new byte[8192];
        while (true) {
            final int r = is.read(buf);
            if (r < 0) break;
            os.write(buf, 0, r);
        }
    }

    // IOUtils.toByteArray() from the Apache Commons IO
    // looks like something very strange in term of efficiency...
    @NonNull
    public static byte[] toArray(@NonNull final InputStream is) throws IOException {
        final Queue<byte[]> q = new LinkedList<>();
        byte[] buf = new byte[4096];
        q.add(buf);
        int off = 0;
        int len = buf.length;
        int l;
        while ((l = is.read(buf, off, len)) >= 0) {
            off += l;
            len -= l;
            if (len == 0) {
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

    // Some sugar if boxing is affordable
    @NonNull
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseArray o) {
        return new Iterable<Integer>() {
            @NonNull
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
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
        };
    }

    @NonNull
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseBooleanArray o) {
        return new Iterable<Integer>() {
            @NonNull
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
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
        };
    }

    @NonNull
    public static Iterable<Integer> getTrueKeysIterable(@NonNull final SparseBooleanArray o) {
        return new Iterable<Integer>() {
            @NonNull
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
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
        };
    }

    @NonNull
    public static Iterable<Integer> getKeysIterable(@NonNull final SparseIntArray o) {
        return new Iterable<Integer>() {
            @NonNull
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
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
        };
    }

    public static <T extends Comparable<T>> T clamp(@NonNull T v, T min, T max) {
        if (v.compareTo(min) < 0) return min;
        if (v.compareTo(max) > 0) return max;
        return v;
    }

    public static int bitsAs(int v, int m, int r) {
        return ((v & m) != 0) ? r : 0;
    }

    public static boolean bitsAs(int v, int m) {
        return (v & m) != 0;
    }

    @NonNull
    public static String[] getAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return Build.SUPPORTED_ABIS;
        else if (Build.CPU_ABI2 != null) return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        else return new String[]{Build.CPU_ABI};
    }
}
