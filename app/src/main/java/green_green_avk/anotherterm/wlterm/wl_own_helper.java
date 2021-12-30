package green_green_avk.anotherterm.wlterm;

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * core global own extensions helper
 * <p>
 * To be used to return a protocol extension socket fd.
 * Supposed to be initially bound at object id 2
 * and removed silently by the server on first call to
 * the wl_display sync() or get_registry() methods.
 * <p>
 * It is added purely to avoid Xwayland recompilation to support additional features
 * like input method or clipboard.
 */
public abstract class wl_own_helper
        extends WlInterface<wl_own_helper.Requests, wl_own_helper.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * Mark this client session to be able to connect helper servers in future
         *
         * @param uuid a secret key of the session
         */
        @IMethod(0)
        void mark(long uuid);

        /**
         * Connect to a previously marked client session
         * <p>
         * The connection protocol will be switched after this call.
         * <p>
         * If the uuid is invalid, any packets will be consumed silently
         * for a security reason.
         *
         * @param uuid     a secret key of the target session
         * @param protocol see {@link Enums.Protocol}
         */
        @IMethod(1)
        void connect(long uuid, long protocol) throws WlOwnCustomException;
    }

    public interface Events extends WlInterface.Events {
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Protocol {
            private Protocol() {
            }

            public static final int simple = 0;
        }
    }
}
