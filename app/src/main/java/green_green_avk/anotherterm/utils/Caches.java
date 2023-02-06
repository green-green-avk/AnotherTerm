package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Caches {
    private Caches() {
    }

    @SuppressWarnings("rawtypes")
    private static final Cache NO_CACHE = new Cache() {
        @Override
        @Nullable
        public Object get(@NonNull final Object key) {
            return null;
        }

        @Override
        public void put(@NonNull final Object key, @NonNull final Object item) {
        }

        @Override
        public void remove(@NonNull final Object key) {
        }
    };

    @NonNull
    public static <T> Cache<T> noCache() {
        return NO_CACHE;
    }
}
