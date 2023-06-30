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
import androidx.annotation.Nullable;

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * desktop user interface surface base interface
 * <p>
 * An interface that may be implemented by a {@code wl_surface}, for
 * implementations that provide a desktop-style user interface.
 * <p>
 * It provides a base set of functionality required to construct user
 * interface elements requiring management by the compositor, such as
 * toplevel windows, menus, etc. The types of functionality are split into
 * {@code xdg_surface} roles.
 * <p>
 * Creating an {@code xdg_surface} does not set the role for a {@code wl_surface}. In order
 * to map an {@code xdg_surface}, the client must create a role-specific object
 * using, e.g., {@code get_toplevel}, {@code get_popup}. The {@code wl_surface} for any given
 * {@code xdg_surface} can have at most one role, and may not be assigned any role
 * not based on {@code xdg_surface}.
 * <p>
 * A role must be assigned before any other requests are made to the
 * {@code xdg_surface} object.
 * <p>
 * The client must call {@code wl_surface.commit} on the corresponding {@code wl_surface}
 * for the {@code xdg_surface} state to take effect.
 * <p>
 * Creating an {@code xdg_surface} from a {@code wl_surface} which has a buffer attached or
 * committed is a client error, and any attempts by a client to attach or
 * manipulate a buffer prior to the first {@code xdg_surface.configure} call must
 * also be treated as errors.
 * <p>
 * After creating a role-specific object and setting it up, the client must
 * perform an initial commit without any buffer attached. The compositor
 * will reply with an {@code xdg_surface.configure} event. The client must
 * acknowledge it and is then allowed to attach a buffer to map the surface.
 * <p>
 * Mapping an {@code xdg_surface}-based role surface is defined as making it
 * possible for the surface to be shown by the compositor. Note that
 * a mapped surface is not guaranteed to be visible once it is mapped.
 * <p>
 * For an {@code xdg_surface} to be mapped by the compositor, the following
 * conditions must be met:
 * (1) the client has assigned an {@code xdg_surface}-based role to the surface
 * (2) the client has set and committed the {@code xdg_surface} state and the
 * role-dependent state to the surface
 * (3) the client has committed a buffer to the surface
 * <p>
 * A newly-unmapped surface is considered to have met condition (1) out
 * of the 3 required conditions for mapping a surface if its role surface
 * has not been destroyed, i.e. the client must perform the initial commit
 * again before attaching a buffer.
 */
public class xdg_surface extends WlInterface<xdg_surface.Requests, xdg_surface.Events> {
    public static final int version = 5;

    public interface Requests extends WlInterface.Requests {

        /**
         * destroy the {@code xdg_surface}
         * <p>
         * Destroy the {@code xdg_surface} object. An {@code xdg_surface} must only be destroyed
         * after its role object has been destroyed, otherwise
         * a {@code defunct_role_object} error is raised.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * assign the {@code xdg_toplevel} surface role
         * <p>
         * This creates an {@code xdg_toplevel} object for the given {@code xdg_surface} and gives
         * the associated {@code wl_surface} the {@code xdg_toplevel} role.
         * <p>
         * See the documentation of {@code xdg_toplevel} for more details about what an
         * {@code xdg_toplevel} is and how it is used.
         *
         * @param id ...
         */
        @IMethod(1)
        void get_toplevel(@Iface(xdg_toplevel.class) @NonNull NewId id);

        /**
         * assign the {@code xdg_popup} surface role
         * <p>
         * This creates an {@code xdg_popup} object for the given {@code xdg_surface} and gives
         * the associated {@code wl_surface} the {@code xdg_popup} role.
         * <p>
         * If null is passed as a parent, a parent surface must be specified using
         * some other protocol, before committing the initial state.
         * <p>
         * See the documentation of {@code xdg_popup} for more details about what an
         * {@code xdg_popup} is and how it is used.
         *
         * @param id         ...
         * @param parent     ...
         * @param positioner ...
         */
        @IMethod(2)
        void get_popup(@Iface(xdg_popup.class) @NonNull NewId id, @INullable @Nullable xdg_surface parent, @NonNull xdg_positioner positioner);

        /**
         * set the new window geometry
         * <p>
         * The window geometry of a surface is its "visible bounds" from the
         * user's perspective. Client-side decorations often have invisible
         * portions like drop-shadows which should be ignored for the
         * purposes of aligning, placing and constraining windows.
         * <p>
         * The window geometry is double buffered, and will be applied at the
         * time {@code wl_surface.commit} of the corresponding {@code wl_surface} is called.
         * <p>
         * When maintaining a position, the compositor should treat the (x, y)
         * coordinate of the window geometry as the top left corner of the window.
         * A client changing the (x, y) window geometry coordinate should in
         * general not alter the position of the window.
         * <p>
         * Once the window geometry of the surface is set, it is not possible to
         * unset it, and it will remain the same until {@code set_window_geometry} is
         * called again, even if a new subsurface or buffer is attached.
         * <p>
         * If never set, the value is the full bounds of the surface,
         * including any subsurfaces. This updates dynamically on every
         * commit. This unset is meant for extremely simple clients.
         * <p>
         * The arguments are given in the surface-local coordinate space of
         * the {@code wl_surface} associated with this {@code xdg_surface}.
         * <p>
         * The width and height must be greater than zero. Setting an invalid size
         * will raise an {@code invalid_size} error. When applied, the effective window
         * geometry will be the set window geometry clamped to the bounding
         * rectangle of the combined geometry of the surface of the {@code xdg_surface} and
         * the associated subsurfaces.
         *
         * @param x      ...
         * @param y      ...
         * @param width  ...
         * @param height ...
         */
        @IMethod(3)
        void set_window_geometry(int x, int y, int width, int height);

        /**
         * ack a configure event
         * <p>
         * When a configure event is received, if a client commits the
         * surface in response to the configure event, then the client
         * must make an {@code ack_configure} request sometime before the commit
         * request, passing along the serial of the configure event.
         * <p>
         * For instance, for toplevel surfaces the compositor might use this
         * information to move a surface to the top left only when the client has
         * drawn itself for the maximized or fullscreen state.
         * <p>
         * If the client receives multiple configure events before it
         * can respond to one, it only has to ack the last configure event.
         * Acking a configure event that was never sent raises an {@code invalid_serial}
         * error.
         * <p>
         * A client is not required to commit immediately after sending
         * an {@code ack_configure} request - it may even {@code ack_configure} several times
         * before its next surface commit.
         * <p>
         * A client may send multiple {@code ack_configure} requests before committing, but
         * only the last request sent before a commit indicates which configure
         * event the client really is responding to.
         * <p>
         * Sending an {@code ack_configure} request consumes the serial number sent with
         * the request, as well as serial numbers sent by all configure events
         * sent on this {@code xdg_surface} prior to the configure event referenced by
         * the committed serial.
         * <p>
         * It is an error to issue multiple {@code ack_configure} requests referencing a
         * serial from the same configure event, or to issue an {@code ack_configure}
         * request referencing a serial from a configure event issued before the
         * event identified by the last {@code ack_configure} request for the same
         * {@code xdg_surface}. Doing so will raise an {@code invalid_serial} error.
         *
         * @param serial the serial from the configure event
         */
        @IMethod(4)
        void ack_configure(long serial);
    }

    public interface Events extends WlInterface.Events {

        /**
         * suggest a surface change
         * <p>
         * The configure event marks the end of a configure sequence. A configure
         * sequence is a set of one or more events configuring the state of the
         * {@code xdg_surface}, including the final {@code xdg_surface.configure} event.
         * <p>
         * Where applicable, {@code xdg_surface} surface roles will during a configure
         * sequence extend this event as a latched state sent as events before the
         * {@code xdg_surface.configure} event. Such events should be considered to make up
         * a set of atomically applied configuration states, where the
         * {@code xdg_surface.configure} commits the accumulated state.
         * <p>
         * Clients should arrange their surface for the new states, and then send
         * an {@code ack_configure} request with the serial sent in this configure event at
         * some point before committing the new surface.
         * <p>
         * If the client receives multiple configure events before it can respond
         * to one, it is free to discard all but the last event it received.
         *
         * @param serial serial of the configure event
         */
        @IMethod(0)
        void configure(long serial);
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Error {
            private Error() {
            }

            /**
             * Surface was not fully constructed
             */
            public static final int not_constructed = 1;

            /**
             * Surface was already constructed
             */
            public static final int already_constructed = 2;

            /**
             * Attaching a buffer to an unconfigured surface
             */
            public static final int unconfigured_buffer = 3;

            /**
             * Invalid serial number when acking a configure event
             */
            public static final int invalid_serial = 4;

            /**
             * Width or height was zero or negative
             */
            public static final int invalid_size = 5;

            /**
             * Surface was destroyed before its role object
             */
            public static final int defunct_role_object = 6;
        }
    }
}
