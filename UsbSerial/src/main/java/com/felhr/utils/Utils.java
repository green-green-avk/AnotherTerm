package com.felhr.utils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import java.util.Collection;
import java.util.List;

public final class Utils {
    private Utils() {
    }

    public static <T> List<T> removeIf(final Collection<? extends T> c,
                                       final Predicate<? super T> predicate) {
        return Stream.of(c.iterator())
                .filterNot(predicate)
                .collect(Collectors.toList());
    }
}
