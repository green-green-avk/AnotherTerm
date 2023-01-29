package com.jcraft.jsch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

public final class Fingerprints {
    private Fingerprints() {
    }

    @NotNull
    public static byte[] get(@NotNull final byte[] data, @NotNull final String hashName,
                             @Nullable Configuration from) {
        if (from == null) {
            from = JSch.getConfiguration();
        }
        try {
            final HASH hash = Util.getAlgorithm(hashName, HASH.class, from);
            hash.init();
            hash.update(data, 0, data.length);
            return hash.digest();
        } catch (final Exception e) {
            throw new JSchErrorException(e);
        }
    }

    @NotNull
    public static String prettyPrintSemicolon(@NotNull final byte[] data) {
        return String.join(":", () -> new Iterator<CharSequence>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < data.length;
            }

            @Override
            public CharSequence next() {
                if (i == data.length)
                    throw new NoSuchElementException();
                return String.format(Locale.ROOT, "%02X", data[i++]);
            }
        });
    }
}
