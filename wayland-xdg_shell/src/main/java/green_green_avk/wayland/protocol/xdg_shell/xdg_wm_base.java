package green_green_avk.wayland.protocol.xdg_shell;

/*
 * Copyright © 2008-2013 Kristian Høgsberg
 * Copyright © 2013      Rafael Antognolli
 * Copyright © 2013      Jasper St. Pierre
 * Copyright © 2010-2013 Intel Corporation
 * Copyright © 2015-2017 Samsung Electronics Co., Ltd
 * Copyright © 2015-2017 Red Hat Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the next
 * paragraph) shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import androidx.annotation.NonNull;

import green_green_avk.wayland.protocol.wayland.wl_surface;
import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * create desktop-style surfaces
 * <p>
 * The {@code xdg_wm_base} interface is exposed as a global object enabling clients
 * to turn their {@code wl_surfaces} into windows in a desktop environment. It
 * defines the basic functionality needed for clients and the compositor to
 * create windows that can be dragged, resized, maximized, etc, as well as
 * creating transient windows such as popup menus.
 */
public class xdg_wm_base extends WlInterface<xdg_wm_base.Requests, xdg_wm_base.Events> {
    public static final int version = 5;

    public interface Requests extends WlInterface.Requests {

        /**
         * destroy {@code xdg_wm_base}
         * <p>
         * Destroy this {@code xdg_wm_base} object.
         * <p>
         * Destroying a bound {@code xdg_wm_base} object while there are surfaces
         * still alive created by this {@code xdg_wm_base} object instance is illegal
         * and will result in a {@code defunct_surfaces} error.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * create a positioner object
         * <p>
         * Create a positioner object. A positioner object is used to position
         * surfaces relative to some parent surface. See the interface description
         * and {@code xdg_surface.get_popup} for details.
         *
         * @param id ...
         */
        @IMethod(1)
        void create_positioner(@Iface(xdg_positioner.class) @NonNull NewId id);

        /**
         * create a shell surface from a surface
         * <p>
         * This creates an {@code xdg_surface} for the given surface. While {@code xdg_surface}
         * itself is not a role, the corresponding surface may only be assigned
         * a role extending {@code xdg_surface}, such as {@code xdg_toplevel} or {@code xdg_popup}. It is
         * illegal to create an {@code xdg_surface} for a {@code wl_surface} which already has an
         * assigned role and this will result in a role error.
         * <p>
         * This creates an {@code xdg_surface} for the given surface. An {@code xdg_surface} is
         * used as basis to define a role to a given surface, such as {@code xdg_toplevel}
         * or {@code xdg_popup}. It also manages functionality shared between {@code xdg_surface}
         * based surface roles.
         * <p>
         * See the documentation of {@code xdg_surface} for more details about what an
         * {@code xdg_surface} is and how it is used.
         *
         * @param id      ...
         * @param surface ...
         */
        @IMethod(2)
        void get_xdg_surface(@Iface(xdg_surface.class) @NonNull NewId id, @NonNull wl_surface surface);

        /**
         * respond to a ping event
         * <p>
         * A client must respond to a ping event with a pong request or
         * the client may be deemed unresponsive. See {@code xdg_wm_base.ping}
         * and {@code xdg_wm_base.error.unresponsive}.
         *
         * @param serial serial of the ping event
         */
        @IMethod(3)
        void pong(long serial);
    }

    public interface Events extends WlInterface.Events {

        /**
         * check if the client is alive
         * <p>
         * The ping event asks the client if it's still alive. Pass the
         * serial specified in the event back to the compositor by sending
         * a "{@code pong}" request back with the specified serial. See {@code xdg_wm_base.pong}.
         * <p>
         * Compositors can use this to determine if the client is still
         * alive. It's unspecified what will happen if the client doesn't
         * respond to the ping request, or in what timeframe. Clients should
         * try to respond in a reasonable amount of time. The “unresponsive”
         * error is provided for compositors that wish to disconnect unresponsive
         * clients.
         * <p>
         * A compositor is free to ping in any way it wants, but a client must
         * always respond to any {@code xdg_wm_base} object it created.
         *
         * @param serial pass this to the pong request
         */
        @IMethod(0)
        void ping(long serial);
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Error {
            private Error() {
            }

            /**
             * given wl_surface has another role
             */
            public static final int role = 0;

            /**
             * xdg_wm_base was destroyed before children
             */
            public static final int defunct_surfaces = 1;

            /**
             * the client tried to map or destroy a non-topmost popup
             */
            public static final int not_the_topmost_popup = 2;

            /**
             * the client specified an invalid popup parent surface
             */
            public static final int invalid_popup_parent = 3;

            /**
             * the client provided an invalid surface state
             */
            public static final int invalid_surface_state = 4;

            /**
             * the client provided an invalid positioner
             */
            public static final int invalid_positioner = 5;

            /**
             * the client didn’t respond to a ping event in time
             */
            public static final int unresponsive = 6;
        }
    }
}
