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
 * global registry object
 * <p>
 * The singleton global registry object.  The server has a number of
 * global objects that are available to all clients.  These objects
 * typically represent an actual object in the server (for example,
 * an input device) or they are singleton objects that provide
 * extension functionality.
 * <p>
 * When a client creates a registry object, the registry object
 * will emit a global event for each global currently in the
 * registry.  Globals come and go as a result of device or
 * monitor hotplugs, reconfiguration or other events, and the
 * registry will send out global and global_remove events to
 * keep the client up to date with the changes.  To mark the end
 * of the initial burst of events, the client can use the
 * wl_display.sync request immediately after calling
 * wl_display.get_registry.
 * <p>
 * A client can bind to a global object by using the bind
 * request.  This creates a client-side handle that lets the object
 * emit events to the client and lets the client invoke requests on
 * the object.
 */
public class wl_registry extends WlInterface<wl_registry.Requests, wl_registry.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * bind an object to the display
         * <p>
         * Binds a new, client-created object to the server using the
         * specified name as the identifier.
         *
         * @param name unique numeric name of the object
         * @param id   bounded object
         */
        @IMethod(0)
        void bind(long name, @NonNull NewId id);
    }

    public interface Events extends WlInterface.Events {

        /**
         * announce global object
         * <p>
         * Notify the client of global objects.
         * <p>
         * The event notifies the client that a global object with
         * the given name is now available, and it implements the
         * given version of the given interface.
         *
         * @param name          numeric name of the global object
         * @param interfaceName interface implemented by the object
         * @param version       interface version
         */
        @IMethod(0)
        void global(long name, @NonNull String interfaceName, long version);

        /**
         * announce removal of global object
         * <p>
         * Notify the client of removed global objects.
         * <p>
         * This event notifies the client that the global identified
         * by name is no longer available.  If the client bound to
         * the global using the bind request, the client should now
         * destroy that object.
         * <p>
         * The object remains valid and requests to the object will be
         * ignored until the client destroys it, to avoid races between
         * the global going away and a client sending a request to it.
         *
         * @param name numeric name of the global object
         */
        @IMethod(1)
        void global_remove(long name);
    }

    public static final class Enums {
        private Enums() {
        }
    }
}
