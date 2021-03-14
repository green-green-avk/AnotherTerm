package green_green_avk.wayland.protocol_core;

import androidx.annotation.NonNull;

import java.util.Locale;

public class WlErrorException extends WlException {
    @NonNull
    public final WlInterface source;
    public final int id;
    @NonNull
    public final String message;

    public WlErrorException(final WlInterface source, final int id,
                            @NonNull final String message) {
        super(String.format(Locale.getDefault(),
                "From %d: error id %d: %s",
                source.id, id, message));
        this.source = source;
        this.id = id;
        this.message = message;
    }
}
