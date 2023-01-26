package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.CharBuffer;
import java.util.Arrays;

public final class Password implements ErasableCharSequence {
    @NonNull
    private final char[] data;

    private Password(@NonNull final char[] v) {
        data = v;
    }

    /**
     * Move semantics factory
     */
    @NonNull
    public static Password adopt(@NonNull final char[] v) {
        return new Password(v);
    }

    /**
     * Move semantics factory
     */
    @NonNull
    public static Password adopt(@NonNull final CharSequence v) {
        return new Password(Misc.toArrayAndErase(v));
    }

    /**
     * Copy semantics factory
     */
    @NonNull
    public static Password from(@NonNull final Password v) {
        return new Password(v.data.clone());
    }

    /**
     * Copy semantics factory
     */
    @NonNull
    public static Password from(@NonNull final char[] v) {
        return new Password(v.clone());
    }

    /**
     * Copy semantics factory
     */
    @NonNull
    public static Password from(@NonNull final CharSequence v) {
        return new Password(Misc.toArray(v));
    }

    /**
     * Just to avoid breaking {@link #equals(Object) contract}.
     */
    public boolean matches(@Nullable final Object o) {
        if (equals(o))
            return true;
        if (o instanceof char[])
            return Arrays.equals(data, (char[]) o);
        if (o instanceof CharSequence)
            return Misc.equals((CharSequence) o, data);
        return false;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Password)
            return Arrays.equals(data, ((Password) obj).data);
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    protected void finalize() throws Throwable {
        erase();
        super.finalize();
    }

    /**
     * Exposes the wrapped internal array in order to work around a Dalvik and early ART bug
     * with garbage output of {@link java.nio.charset.Charset#encode(CharBuffer)}
     * if its argument is represented by {@link CharBuffer#wrap(CharSequence)}
     * around this object.
     * <p>
     * Android APIs < 24 are affected.
     */
    @NonNull
    public CharBuffer asCharBuffer() {
        return CharBuffer.wrap(data);
    }

    @NonNull
    public Password copy() {
        return new Password(data.clone());
    }

    @NonNull
    public char[] toArray() {
        return data.clone();
    }

    @Override
    public void erase() {
        Arrays.fill(data, '\0');
    }

    @Override
    public int length() {
        return data.length;
    }

    @Override
    public char charAt(final int index) {
        return data[index];
    }

    @Override
    @NonNull
    public CharSequence subSequence(final int start, final int end) {
        if (start < 0 || start > end || end > data.length)
            throw new IndexOutOfBoundsException();
        return new CharSequence() {
            @Override
            public int length() {
                return end - start;
            }

            @Override
            public char charAt(final int index) {
                if (index < 0 || index >= length())
                    throw new IndexOutOfBoundsException();
                return data[start + index];
            }

            @Override
            @NonNull
            public CharSequence subSequence(final int _start, final int _end) {
                return Password.this.subSequence(start + _start, start + _end);
            }
        };
    }
}
