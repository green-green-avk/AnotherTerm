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
import androidx.annotation.Nullable;

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * desktop-style metadata interface
 * <p>
 * An interface that may be implemented by a {@code wl_surface}, for
 * implementations that provide a desktop-style user interface.
 * <p>
 * It provides requests to treat surfaces like toplevel, fullscreen
 * or popup windows, move, resize or maximize them, associate
 * metadata like title and class, etc.
 * <p>
 * On the server side the object is automatically destroyed when
 * the related {@code wl_surface} is destroyed. On the client side,
 * {@code wl_shell_surface_destroy}() must be called before destroying
 * the {@code wl_surface} object.
 */
public class wl_shell_surface extends WlInterface<wl_shell_surface.Requests, wl_shell_surface.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * respond to a ping event
         * <p>
         * A client must respond to a ping event with a pong request or
         * the client may be deemed unresponsive.
         *
         * @param serial serial number of the ping event
         */
        @IMethod(0)
        void pong(long serial);

        /**
         * start an interactive move
         * <p>
         * Start a pointer-driven move of the surface.
         * <p>
         * This request must be used in response to a button press event.
         * The server may ignore move requests depending on the state of
         * the surface (e.g. fullscreen or maximized).
         *
         * @param seat   seat whose pointer is used
         * @param serial serial number of the implicit grab on the pointer
         */
        @IMethod(1)
        void move(@NonNull wl_seat seat, long serial);

        /**
         * start an interactive resize
         * <p>
         * Start a pointer-driven resizing of the surface.
         * <p>
         * This request must be used in response to a button press event.
         * The server may ignore resize requests depending on the state of
         * the surface (e.g. fullscreen or maximized).
         *
         * @param seat   seat whose pointer is used
         * @param serial serial number of the implicit grab on the pointer
         * @param edges  which edge or corner is being dragged
         */
        @IMethod(2)
        void resize(@NonNull wl_seat seat, long serial, long edges);

        /**
         * make the surface a toplevel surface
         * <p>
         * Map the surface as a toplevel surface.
         * <p>
         * A toplevel surface is not fullscreen, maximized or transient.
         */
        @IMethod(3)
        void set_toplevel();

        /**
         * make the surface a transient surface
         * <p>
         * Map the surface relative to an existing surface.
         * <p>
         * The x and y arguments specify the location of the upper left
         * corner of the surface relative to the upper left corner of the
         * parent surface, in surface-local coordinates.
         * <p>
         * The flags argument controls details of the transient behaviour.
         *
         * @param parent parent surface
         * @param x      surface-local x coordinate
         * @param y      surface-local y coordinate
         * @param flags  transient surface behavior
         */
        @IMethod(4)
        void set_transient(@NonNull wl_surface parent, int x, int y, long flags);

        /**
         * make the surface a fullscreen surface
         * <p>
         * Map the surface as a fullscreen surface.
         * <p>
         * If an output parameter is given then the surface will be made
         * fullscreen on that output. If the client does not specify the
         * output then the compositor will apply its policy - usually
         * choosing the output on which the surface has the biggest surface
         * area.
         * <p>
         * The client may specify a method to resolve a size conflict
         * between the output size and the surface size - this is provided
         * through the method parameter.
         * <p>
         * The framerate parameter is used only when the method is set
         * to "driver", to indicate the preferred framerate. A value of 0
         * indicates that the client does not care about framerate. The
         * framerate is specified in mHz, that is framerate of 60000 is 60Hz.
         * <p>
         * A method of "scale" or "driver" implies a scaling operation of
         * the surface, either via a direct scaling operation or a change of
         * the output mode. This will override any kind of output scaling, so
         * that mapping a surface with a buffer size equal to the mode can
         * fill the screen independent of {@code buffer_scale}.
         * <p>
         * A method of "fill" means we don't scale up the buffer, however
         * any output scale is applied. This means that you may run into
         * an edge case where the application maps a buffer with the same
         * size of the output mode but {@code buffer_scale} 1 (thus making a
         * surface larger than the output). In this case it is allowed to
         * downscale the results to fit the screen.
         * <p>
         * The compositor must reply to this request with a configure event
         * with the dimensions for the output on which the surface will
         * be made fullscreen.
         *
         * @param method    method for resolving size conflict
         * @param framerate framerate in mHz
         * @param output    output on which the surface is to be fullscreen
         */
        @IMethod(5)
        void set_fullscreen(long method, long framerate, @INullable @Nullable wl_output output);

        /**
         * make the surface a popup surface
         * <p>
         * Map the surface as a popup.
         * <p>
         * A popup surface is a transient surface with an added pointer
         * grab.
         * <p>
         * An existing implicit grab will be changed to owner-events mode,
         * and the popup grab will continue after the implicit grab ends
         * (i.e. releasing the mouse button does not cause the popup to
         * be unmapped).
         * <p>
         * The popup grab continues until the window is destroyed or a
         * mouse button is pressed in any other client's window. A click
         * in any of the client's surfaces is reported as normal, however,
         * clicks in other clients' surfaces will be discarded and trigger
         * the callback.
         * <p>
         * The x and y arguments specify the location of the upper left
         * corner of the surface relative to the upper left corner of the
         * parent surface, in surface-local coordinates.
         *
         * @param seat   seat whose pointer is used
         * @param serial serial number of the implicit grab on the pointer
         * @param parent parent surface
         * @param x      surface-local x coordinate
         * @param y      surface-local y coordinate
         * @param flags  transient surface behavior
         */
        @IMethod(6)
        void set_popup(@NonNull wl_seat seat, long serial, @NonNull wl_surface parent, int x, int y, long flags);

        /**
         * make the surface a maximized surface
         * <p>
         * Map the surface as a maximized surface.
         * <p>
         * If an output parameter is given then the surface will be
         * maximized on that output. If the client does not specify the
         * output then the compositor will apply its policy - usually
         * choosing the output on which the surface has the biggest surface
         * area.
         * <p>
         * The compositor will reply with a configure event telling
         * the expected new surface size. The operation is completed
         * on the next buffer attach to this surface.
         * <p>
         * A maximized surface typically fills the entire output it is
         * bound to, except for desktop elements such as panels. This is
         * the main difference between a maximized shell surface and a
         * fullscreen shell surface.
         * <p>
         * The details depend on the compositor implementation.
         *
         * @param output output on which the surface is to be maximized
         */
        @IMethod(7)
        void set_maximized(@INullable @Nullable wl_output output);

        /**
         * set surface title
         * <p>
         * Set a short title for the surface.
         * <p>
         * This string may be used to identify the surface in a task bar,
         * window list, or other user interface elements provided by the
         * compositor.
         * <p>
         * The string must be encoded in UTF-8.
         *
         * @param title surface title
         */
        @IMethod(8)
        void set_title(@NonNull String title);

        /**
         * set surface class
         * <p>
         * Set a class for the surface.
         * <p>
         * The surface class identifies the general class of applications
         * to which the surface belongs. A common convention is to use the
         * file name (or the full path if it is a non-standard location) of
         * the application's .desktop file as the class.
         *
         * @param class_Name surface class
         */
        @IMethod(9)
        void set_class(@NonNull String class_Name);
    }

    public interface Events extends WlInterface.Events {

        /**
         * ping client
         * <p>
         * Ping a client to check if it is receiving events and sending
         * requests. A client is expected to reply with a pong request.
         *
         * @param serial serial number of the ping
         */
        @IMethod(0)
        void ping(long serial);

        /**
         * suggest resize
         * <p>
         * The configure event asks the client to resize its surface.
         * <p>
         * The size is a hint, in the sense that the client is free to
         * ignore it if it doesn't resize, pick a smaller size (to
         * satisfy aspect ratio or resize in steps of NxM pixels).
         * <p>
         * The edges parameter provides a hint about how the surface
         * was resized. The client may use this information to decide
         * how to adjust its content to the new size (e.g. a scrolling
         * area might adjust its content position to leave the viewable
         * content unmoved).
         * <p>
         * The client is free to dismiss all but the last configure
         * event it received.
         * <p>
         * The width and height arguments specify the size of the window
         * in surface-local coordinates.
         *
         * @param edges  how the surface was resized
         * @param width  new width of the surface
         * @param height new height of the surface
         */
        @IMethod(1)
        void configure(long edges, int width, int height);

        /**
         * popup interaction is done
         * <p>
         * The {@code popup_done} event is sent out when a popup grab is broken,
         * that is, when the user clicks a surface that doesn't belong
         * to the client owning the popup surface.
         */
        @IMethod(2)
        void popup_done();
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * edge values for resizing
         */
        public static final class Resize {
            private Resize() {
            }

            /**
             * no edge
             */
            public static final int none = 0;

            /**
             * top edge
             */
            public static final int top = 1;

            /**
             * bottom edge
             */
            public static final int bottom = 2;

            /**
             * left edge
             */
            public static final int left = 4;

            /**
             * top and left edges
             */
            public static final int top_left = 5;

            /**
             * bottom and left edges
             */
            public static final int bottom_left = 6;

            /**
             * right edge
             */
            public static final int right = 8;

            /**
             * top and right edges
             */
            public static final int top_right = 9;

            /**
             * bottom and right edges
             */
            public static final int bottom_right = 10;
        }

        /**
         * details of transient behaviour
         */
        public static final class Transient {
            private Transient() {
            }

            /**
             * do not set keyboard focus
             */
            public static final int inactive = 0x1;
        }

        /**
         * different method to set the surface fullscreen
         */
        public static final class Fullscreen_method {
            private Fullscreen_method() {
            }

            /**
             * no preference, apply default policy
             */
            public static final int _default = 0;

            /**
             * scale, preserve the surface's aspect ratio and center on output
             */
            public static final int scale = 1;

            /**
             * switch output mode to the smallest mode that can fit the surface, add black borders to compensate size mismatch
             */
            public static final int driver = 2;

            /**
             * no upscaling, center on output and add black borders to compensate size mismatch
             */
            public static final int fill = 3;
        }
    }
}
