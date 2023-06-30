package green_green_avk.wayland.protocol.wayland;

/*
 * Copyright © 2008-2011 Kristian Høgsberg
 * Copyright © 2010-2011 Intel Corporation
 * Copyright © 2012-2013 Collabora, Ltd.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the
 * next paragraph) shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import androidx.annotation.NonNull;

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * core global object
 * <p>
 * The core global object. This is a special singleton object. It
 * is used for internal Wayland protocol features.
 */
public class wl_display extends WlInterface<wl_display.Requests, wl_display.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * asynchronous roundtrip
         * <p>
         * The sync request asks the server to emit the 'done' event
         * on the returned {@code wl_callback} object. Since requests are
         * handled in-order and events are delivered in-order, this can
         * be used as a barrier to ensure all previous requests and the
         * resulting events have been handled.
         * <p>
         * The object returned by this request will be destroyed by the
         * compositor after the callback is fired and as such the client must not
         * attempt to use it after that point.
         * <p>
         * The {@code callback_data} passed in the callback is the event serial.
         *
         * @param callback callback object for the sync request
         */
        @IMethod(0)
        void sync(@Iface(wl_callback.class) @NonNull NewId callback);

        /**
         * get global registry object
         * <p>
         * This request creates a registry object that allows the client
         * to list and bind the global objects available from the
         * compositor.
         * <p>
         * It should be noted that the server side resources consumed in
         * response to a {@code get_registry} request can only be released when the
         * client disconnects, not when the client side proxy is destroyed.
         * Therefore, clients should invoke {@code get_registry} as infrequently as
         * possible to avoid wasting memory.
         *
         * @param registry global registry object
         */
        @IMethod(1)
        void get_registry(@Iface(wl_registry.class) @NonNull NewId registry);
    }

    public interface Events extends WlInterface.Events {

        /**
         * fatal error event
         * <p>
         * The error event is sent out when a fatal (non-recoverable)
         * error has occurred. The {@code object_id} argument is the object
         * where the error occurred, most often in response to a request
         * to that object. The code identifies the error and is defined
         * by the object interface. As such, each interface defines its
         * own set of error codes. The message is a brief description
         * of the error, for (debugging) convenience.
         *
         * @param object_id object where the error occurred
         * @param code      error code
         * @param message   error description
         */
        @IMethod(0)
        void error(@NonNull WlInterface object_id, long code, @NonNull String message);

        /**
         * acknowledge object ID deletion
         * <p>
         * This event is used internally by the object ID management
         * logic. When a client deletes an object that it had created,
         * the server will send this event to acknowledge that it has
         * seen the delete request. When the client receives this event,
         * it will know that it can safely reuse the object ID.
         *
         * @param id deleted object ID
         */
        @IMethod(1)
        void delete_id(long id);
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * global error values
         */
        public static final class Error {
            private Error() {
            }

            /**
             * server couldn't find object
             */
            public static final int invalid_object = 0;

            /**
             * method doesn't exist on the specified interface or malformed request
             */
            public static final int invalid_method = 1;

            /**
             * server is out of memory
             */
            public static final int no_memory = 2;

            /**
             * implementation error in compositor
             */
            public static final int implementation = 3;
        }
    }
}
