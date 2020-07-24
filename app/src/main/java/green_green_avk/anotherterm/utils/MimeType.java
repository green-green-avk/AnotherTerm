package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class MimeType {
    public boolean isSet = false;
    @NonNull
    public String major = "*";
    @NonNull
    public String minor = "*";

    @NonNull
    public String get() {
        return major + "/" + minor;
    }

    @NonNull
    public MimeType set(@NonNull final String v) {
        final int delim = v.indexOf('/');
        if (delim < 0) throw new IllegalArgumentException("Not a mime");
        major = v.substring(0, delim).trim().toLowerCase(Locale.ROOT);
        minor = v.substring(delim + 1).trim().toLowerCase(Locale.ROOT);
        isSet = true;
        return this;
    }

    @NonNull
    public MimeType quietSet(@Nullable final String v) {
        if (v != null)
            try {
                return set(v);
            } catch (final IllegalArgumentException ignored) {
            }
        major = "*";
        minor = "*";
        isSet = true;
        return this;
    }

    @NonNull
    public MimeType set(@NonNull final MimeType m) {
        major = m.major;
        minor = m.minor;
        isSet = true;
        return this;
    }

    @NonNull
    public MimeType merge(@NonNull final String v) {
        if (isSet) return merge(new MimeType().set(v));
        return set(v);
    }

    @NonNull
    public MimeType quietMerge(@Nullable final String v) {
        if (v != null)
            try {
                return merge(v);
            } catch (final IllegalArgumentException ignored) {
            }
        major = "*";
        minor = "*";
        isSet = true;
        return this;
    }

    @NonNull
    public MimeType merge(@NonNull final MimeType m) {
        if (!isSet) {
            set(m);
        } else if (!major.equals(m.major)) {
            major = "*";
            minor = "*";
        } else if (!minor.equals(m.minor)) {
            minor = "*";
        }
        return this;
    }
}
