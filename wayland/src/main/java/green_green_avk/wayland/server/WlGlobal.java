package green_green_avk.wayland.server;

import androidx.annotation.NonNull;

import green_green_avk.wayland.protocol_core.WlInterface;

public interface WlGlobal {
    @NonNull
    Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
    getInterface();

    @NonNull
    WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
    bind(@NonNull WlClient client, @NonNull WlInterface.NewId newId) throws BindException;

    class BindException extends Exception {
        public BindException() {
        }

        public BindException(final String message) {
            super(message);
        }

        public BindException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public BindException(final Throwable cause) {
            super(cause);
        }
    }
}
