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

import green_green_avk.wayland.protocol.wayland.wl_output;
import green_green_avk.wayland.protocol.wayland.wl_seat;
import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * toplevel surface
 * <p>
 * This interface defines an {@code xdg_surface} role which allows a surface to,
 * among other things, set window-like properties such as maximize,
 * fullscreen, and minimize, set application-specific metadata like title and
 * id, and well as trigger user interactive operations such as interactive
 * resize and move.
 * <p>
 * Unmapping an {@code xdg_toplevel} means that the surface cannot be shown
 * by the compositor until it is explicitly mapped again.
 * All active operations (e.g., move, resize) are canceled and all
 * attributes (e.g. title, state, stacking, ...) are discarded for
 * an {@code xdg_toplevel} surface when it is unmapped. The {@code xdg_toplevel} returns to
 * the state it had right after {@code xdg_surface.get_toplevel}. The client
 * can re-map the toplevel by perfoming a commit without any buffer
 * attached, waiting for a configure event and handling it as usual (see
 * {@code xdg_surface} description).
 * <p>
 * Attaching a null buffer to a toplevel unmaps the surface.
 */
public class xdg_toplevel extends WlInterface<xdg_toplevel.Requests, xdg_toplevel.Events> {
    public static final int version = 5;

    public interface Requests extends WlInterface.Requests {

        /**
         * destroy the {@code xdg_toplevel}
         * <p>
         * This request destroys the role surface and unmaps the surface;
         * see "Unmapping" behavior in interface section for details.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * set the parent of this surface
         * <p>
         * Set the "parent" of this surface. This surface should be stacked
         * above the parent surface and all other ancestor surfaces.
         * <p>
         * Parent surfaces should be set on dialogs, toolboxes, or other
         * "auxiliary" surfaces, so that the parent is raised when the dialog
         * is raised.
         * <p>
         * Setting a null parent for a child surface unsets its parent. Setting
         * a null parent for a surface which currently has no parent is a no-op.
         * <p>
         * Only mapped surfaces can have child surfaces. Setting a parent which
         * is not mapped is equivalent to setting a null parent. If a surface
         * becomes unmapped, its children's parent is set to the parent of
         * the now-unmapped surface. If the now-unmapped surface has no parent,
         * its children's parent is unset. If the now-unmapped surface becomes
         * mapped again, its parent-child relationship is not restored.
         * <p>
         * The parent toplevel must not be one of the child toplevel's
         * descendants, and the parent must be different from the child toplevel,
         * otherwise the {@code invalid_parent} protocol error is raised.
         *
         * @param parent ...
         */
        @IMethod(1)
        void set_parent(@INullable @Nullable xdg_toplevel parent);

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
         * @param title ...
         */
        @IMethod(2)
        void set_title(@NonNull String title);

        /**
         * set application ID
         * <p>
         * Set an application identifier for the surface.
         * <p>
         * The app ID identifies the general class of applications to which
         * the surface belongs. The compositor can use this to group multiple
         * surfaces together, or to determine how to launch a new application.
         * <p>
         * For D-Bus activatable applications, the app ID is used as the D-Bus
         * service name.
         * <p>
         * The compositor shell will try to group application surfaces together
         * by their app ID. As a best practice, it is suggested to select app
         * ID's that match the basename of the application's .desktop file.
         * For example, "org.freedesktop.FooViewer" where the .desktop file is
         * "org.freedesktop.FooViewer.desktop".
         * <p>
         * Like other properties, a {@code set_app_id} request can be sent after the
         * {@code xdg_toplevel} has been mapped to update the property.
         * <p>
         * See the desktop-entry specification [0] for more details on
         * application identifiers and how they relate to well-known D-Bus
         * names and .desktop files.
         * <p>
         * [0] https://standards.freedesktop.org/desktop-entry-spec/
         *
         * @param app_id ...
         */
        @IMethod(3)
        void set_app_id(@NonNull String app_id);

        /**
         * show the window menu
         * <p>
         * Clients implementing client-side decorations might want to show
         * a context menu when right-clicking on the decorations, giving the
         * user a menu that they can use to maximize or minimize the window.
         * <p>
         * This request asks the compositor to pop up such a window menu at
         * the given position, relative to the local surface coordinates of
         * the parent surface. There are no guarantees as to what menu items
         * the window menu contains, or even if a window menu will be drawn
         * at all.
         * <p>
         * This request must be used in response to some sort of user action
         * like a button press, key press, or touch down event.
         *
         * @param seat   the {@code wl_seat} of the user event
         * @param serial the serial of the user event
         * @param x      the x position to pop up the window menu at
         * @param y      the y position to pop up the window menu at
         */
        @IMethod(4)
        void show_window_menu(@NonNull wl_seat seat, long serial, int x, int y);

        /**
         * start an interactive move
         * <p>
         * Start an interactive, user-driven move of the surface.
         * <p>
         * This request must be used in response to some sort of user action
         * like a button press, key press, or touch down event. The passed
         * serial is used to determine the type of interactive move (touch,
         * pointer, etc).
         * <p>
         * The server may ignore move requests depending on the state of
         * the surface (e.g. fullscreen or maximized), or if the passed serial
         * is no longer valid.
         * <p>
         * If triggered, the surface will lose the focus of the device
         * ({@code wl_pointer}, {@code wl_touch}, etc) used for the move. It is up to the
         * compositor to visually indicate that the move is taking place, such as
         * updating a pointer cursor, during the move. There is no guarantee
         * that the device focus will return when the move is completed.
         *
         * @param seat   the {@code wl_seat} of the user event
         * @param serial the serial of the user event
         */
        @IMethod(5)
        void move(@NonNull wl_seat seat, long serial);

        /**
         * start an interactive resize
         * <p>
         * Start a user-driven, interactive resize of the surface.
         * <p>
         * This request must be used in response to some sort of user action
         * like a button press, key press, or touch down event. The passed
         * serial is used to determine the type of interactive resize (touch,
         * pointer, etc).
         * <p>
         * The server may ignore resize requests depending on the state of
         * the surface (e.g. fullscreen or maximized).
         * <p>
         * If triggered, the client will receive configure events with the
         * "resize" state enum value and the expected sizes. See the "resize"
         * enum value for more details about what is required. The client
         * must also acknowledge configure events using "a{@code ck_configur}e". After
         * the resize is completed, the client will receive another "configure"
         * event without the resize state.
         * <p>
         * If triggered, the surface also will lose the focus of the device
         * ({@code wl_pointer}, {@code wl_touch}, etc) used for the resize. It is up to the
         * compositor to visually indicate that the resize is taking place,
         * such as updating a pointer cursor, during the resize. There is no
         * guarantee that the device focus will return when the resize is
         * completed.
         * <p>
         * The edges parameter specifies how the surface should be resized, and
         * is one of the values of the {@code resize_edge} enum. Values not matching
         * a variant of the enum will cause a protocol error. The compositor
         * may use this information to update the surface position for example
         * when dragging the top left corner. The compositor may also use
         * this information to adapt its behavior, e.g. choose an appropriate
         * cursor image.
         *
         * @param seat   the {@code wl_seat} of the user event
         * @param serial the serial of the user event
         * @param edges  which edge or corner is being dragged
         */
        @IMethod(6)
        void resize(@NonNull wl_seat seat, long serial, long edges);

        /**
         * set the maximum size
         * <p>
         * Set a maximum size for the window.
         * <p>
         * The client can specify a maximum size so that the compositor does
         * not try to configure the window beyond this size.
         * <p>
         * The width and height arguments are in window geometry coordinates.
         * See {@code xdg_surface.set_window_geometry}.
         * <p>
         * Values set in this way are double-buffered. They will get applied
         * on the next commit.
         * <p>
         * The compositor can use this information to allow or disallow
         * different states like maximize or fullscreen and draw accurate
         * animations.
         * <p>
         * Similarly, a tiling window manager may use this information to
         * place and resize client windows in a more effective way.
         * <p>
         * The client should not rely on the compositor to obey the maximum
         * size. The compositor may decide to ignore the values set by the
         * client and request a larger size.
         * <p>
         * If never set, or a value of zero in the request, means that the
         * client has no expected maximum size in the given dimension.
         * As a result, a client wishing to reset the maximum size
         * to an unspecified state can use zero for width and height in the
         * request.
         * <p>
         * Requesting a maximum size to be smaller than the minimum size of
         * a surface is illegal and will result in an {@code invalid_size} error.
         * <p>
         * The width and height must be greater than or equal to zero. Using
         * strictly negative values for width or height will result in a
         * {@code invalid_size} error.
         *
         * @param width  ...
         * @param height ...
         */
        @IMethod(7)
        void set_max_size(int width, int height);

        /**
         * set the minimum size
         * <p>
         * Set a minimum size for the window.
         * <p>
         * The client can specify a minimum size so that the compositor does
         * not try to configure the window below this size.
         * <p>
         * The width and height arguments are in window geometry coordinates.
         * See {@code xdg_surface.set_window_geometry}.
         * <p>
         * Values set in this way are double-buffered. They will get applied
         * on the next commit.
         * <p>
         * The compositor can use this information to allow or disallow
         * different states like maximize or fullscreen and draw accurate
         * animations.
         * <p>
         * Similarly, a tiling window manager may use this information to
         * place and resize client windows in a more effective way.
         * <p>
         * The client should not rely on the compositor to obey the minimum
         * size. The compositor may decide to ignore the values set by the
         * client and request a smaller size.
         * <p>
         * If never set, or a value of zero in the request, means that the
         * client has no expected minimum size in the given dimension.
         * As a result, a client wishing to reset the minimum size
         * to an unspecified state can use zero for width and height in the
         * request.
         * <p>
         * Requesting a minimum size to be larger than the maximum size of
         * a surface is illegal and will result in an {@code invalid_size} error.
         * <p>
         * The width and height must be greater than or equal to zero. Using
         * strictly negative values for width and height will result in a
         * {@code invalid_size} error.
         *
         * @param width  ...
         * @param height ...
         */
        @IMethod(8)
        void set_min_size(int width, int height);

        /**
         * maximize the window
         * <p>
         * Maximize the surface.
         * <p>
         * After requesting that the surface should be maximized, the compositor
         * will respond by emitting a configure event. Whether this configure
         * actually sets the window maximized is subject to compositor policies.
         * The client must then update its content, drawing in the configured
         * state. The client must also acknowledge the configure when committing
         * the new content (see {@code ack_configure}).
         * <p>
         * It is up to the compositor to decide how and where to maximize the
         * surface, for example which output and what region of the screen should
         * be used.
         * <p>
         * If the surface was already maximized, the compositor will still emit
         * a configure event with the "maximized" state.
         * <p>
         * If the surface is in a fullscreen state, this request has no direct
         * effect. It may alter the state the surface is returned to when
         * unmaximized unless overridden by the compositor.
         */
        @IMethod(9)
        void set_maximized();

        /**
         * unmaximize the window
         * <p>
         * Unmaximize the surface.
         * <p>
         * After requesting that the surface should be unmaximized, the compositor
         * will respond by emitting a configure event. Whether this actually
         * un-maximizes the window is subject to compositor policies.
         * If available and applicable, the compositor will include the window
         * geometry dimensions the window had prior to being maximized in the
         * configure event. The client must then update its content, drawing it in
         * the configured state. The client must also acknowledge the configure
         * when committing the new content (see {@code ack_configure}).
         * <p>
         * It is up to the compositor to position the surface after it was
         * unmaximized; usually the position the surface had before maximizing, if
         * applicable.
         * <p>
         * If the surface was already not maximized, the compositor will still
         * emit a configure event without the "maximized" state.
         * <p>
         * If the surface is in a fullscreen state, this request has no direct
         * effect. It may alter the state the surface is returned to when
         * unmaximized unless overridden by the compositor.
         */
        @IMethod(10)
        void unset_maximized();

        /**
         * set the window as fullscreen on an output
         * <p>
         * Make the surface fullscreen.
         * <p>
         * After requesting that the surface should be fullscreened, the
         * compositor will respond by emitting a configure event. Whether the
         * client is actually put into a fullscreen state is subject to compositor
         * policies. The client must also acknowledge the configure when
         * committing the new content (see {@code ack_configure}).
         * <p>
         * The output passed by the request indicates the client's preference as
         * to which display it should be set fullscreen on. If this value is NULL,
         * it's up to the compositor to choose which display will be used to map
         * this surface.
         * <p>
         * If the surface doesn't cover the whole output, the compositor will
         * position the surface in the center of the output and compensate with
         * with border fill covering the rest of the output. The content of the
         * border fill is undefined, but should be assumed to be in some way that
         * attempts to blend into the surrounding area (e.g. solid black).
         * <p>
         * If the fullscreened surface is not opaque, the compositor must make
         * sure that other screen content not part of the same surface tree (made
         * up of subsurfaces, popups or similarly coupled surfaces) are not
         * visible below the fullscreened surface.
         *
         * @param output ...
         */
        @IMethod(11)
        void set_fullscreen(@INullable @Nullable wl_output output);

        /**
         * unset the window as fullscreen
         * <p>
         * Make the surface no longer fullscreen.
         * <p>
         * After requesting that the surface should be unfullscreened, the
         * compositor will respond by emitting a configure event.
         * Whether this actually removes the fullscreen state of the client is
         * subject to compositor policies.
         * <p>
         * Making a surface unfullscreen sets states for the surface based on the following:
         * * the state(s) it may have had before becoming fullscreen
         * * any state(s) decided by the compositor
         * * any state(s) requested by the client while the surface was fullscreen
         * <p>
         * The compositor may include the previous window geometry dimensions in
         * the configure event, if applicable.
         * <p>
         * The client must also acknowledge the configure when committing the new
         * content (see {@code ack_configure}).
         */
        @IMethod(12)
        void unset_fullscreen();

        /**
         * set the window as minimized
         * <p>
         * Request that the compositor minimize your surface. There is no
         * way to know if the surface is currently minimized, nor is there
         * any way to unset minimization on this surface.
         * <p>
         * If you are looking to throttle redrawing when minimized, please
         * instead use the {@code wl_surface.frame} event for this, as this will
         * also work with live previews on windows in Alt-Tab, Expose or
         * similar compositor features.
         */
        @IMethod(13)
        void set_minimized();
    }

    public interface Events extends WlInterface.Events {

        /**
         * suggest a surface change
         * <p>
         * This configure event asks the client to resize its toplevel surface or
         * to change its state. The configured state should not be applied
         * immediately. See {@code xdg_surface.configure} for details.
         * <p>
         * The width and height arguments specify a hint to the window
         * about how its surface should be resized in window geometry
         * coordinates. See {@code set_window_geometry}.
         * <p>
         * If the width or height arguments are zero, it means the client
         * should decide its own window dimension. This may happen when the
         * compositor needs to configure the state of the surface but doesn't
         * have any information about any previous or expected dimension.
         * <p>
         * The states listed in the event specify how the width/height
         * arguments should be interpreted, and possibly how it should be
         * drawn.
         * <p>
         * Clients must send an {@code ack_configure} in response to this event. See
         * {@code xdg_surface.configure} and {@code xdg_surface.ack_configure} for details.
         *
         * @param width  ...
         * @param height ...
         * @param states ...
         */
        @IMethod(0)
        void configure(int width, int height, @NonNull int[] states);

        /**
         * surface wants to be closed
         * <p>
         * The close event is sent by the compositor when the user
         * wants the surface to be closed. This should be equivalent to
         * the user clicking the close button in client-side decorations,
         * if your application has any.
         * <p>
         * This is only a request that the user intends to close the
         * window. The client may choose to ignore this request, or show
         * a dialog to ask the user to save their data, etc.
         */
        @IMethod(1)
        void close();

        /**
         * recommended window geometry bounds
         * <p>
         * The {@code configure_bounds} event may be sent prior to a {@code xdg_toplevel.configure}
         * event to communicate the bounds a window geometry size is recommended
         * to constrain to.
         * <p>
         * The passed width and height are in surface coordinate space. If width
         * and height are 0, it means bounds is unknown and equivalent to as if no
         * {@code configure_bounds} event was ever sent for this surface.
         * <p>
         * The bounds can for example correspond to the size of a monitor excluding
         * any panels or other shell components, so that a surface isn't created in
         * a way that it cannot fit.
         * <p>
         * The bounds may change at any point, and in such a case, a new
         * {@code xdg_toplevel.configure_bounds} will be sent, followed by
         * {@code xdg_toplevel.configure} and {@code xdg_surface.configure}.
         *
         * @param width  ...
         * @param height ...
         */
        @IMethod(2)
        @ISince(4)
        void configure_bounds(int width, int height);

        /**
         * compositor capabilities
         * <p>
         * This event advertises the capabilities supported by the compositor. If
         * a capability isn't supported, clients should hide or disable the UI
         * elements that expose this functionality. For instance, if the
         * compositor doesn't advertise support for minimized toplevels, a button
         * triggering the {@code set_minimized} request should not be displayed.
         * <p>
         * The compositor will ignore requests it doesn't support. For instance,
         * a compositor which doesn't advertise support for minimized will ignore
         * {@code set_minimized} requests.
         * <p>
         * Compositors must send this event once before the first
         * {@code xdg_surface.configure} event. When the capabilities change, compositors
         * must send this event again and then send an {@code xdg_surface.configure}
         * event.
         * <p>
         * The configured state should not be applied immediately. See
         * {@code xdg_surface.configure} for details.
         * <p>
         * The capabilities are sent as an array of 32-bit unsigned integers in
         * native endianness.
         *
         * @param capabilities array of 32-bit capabilities
         */
        @IMethod(3)
        @ISince(5)
        void wm_capabilities(@NonNull int[] capabilities);
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Error {
            private Error() {
            }

            /**
             * provided value is         not a valid variant of the resize_edge enum
             */
            public static final int invalid_resize_edge = 0;

            /**
             * invalid parent toplevel
             */
            public static final int invalid_parent = 1;

            /**
             * client provided an invalid min or max size
             */
            public static final int invalid_size = 2;
        }

        /**
         * edge values for resizing
         */
        public static final class Resize_edge {
            private Resize_edge() {
            }

            public static final int none = 0;

            public static final int top = 1;

            public static final int bottom = 2;

            public static final int left = 4;

            public static final int top_left = 5;

            public static final int bottom_left = 6;

            public static final int right = 8;

            public static final int top_right = 9;

            public static final int bottom_right = 10;
        }

        /**
         * types of state on the surface
         */
        public static final class State {
            private State() {
            }

            /**
             * the surface is maximized
             */
            public static final int maximized = 1;

            /**
             * the surface is fullscreen
             */
            public static final int fullscreen = 2;

            /**
             * the surface is being resized
             */
            public static final int resizing = 3;

            /**
             * the surface is now activated
             */
            public static final int activated = 4;

            /**
             * the surface’s left edge is tiled
             */
            public static final int tiled_left = 5;

            /**
             * the surface’s right edge is tiled
             */
            public static final int tiled_right = 6;

            /**
             * the surface’s top edge is tiled
             */
            public static final int tiled_top = 7;

            /**
             * the surface’s bottom edge is tiled
             */
            public static final int tiled_bottom = 8;
        }

        public static final class Wm_capabilities {
            private Wm_capabilities() {
            }

            /**
             * show_window_menu is available
             */
            public static final int window_menu = 1;

            /**
             * set_maximized and unset_maximized are available
             */
            public static final int maximize = 2;

            /**
             * set_fullscreen and unset_fullscreen are available
             */
            public static final int fullscreen = 3;

            /**
             * set_minimized is available
             */
            public static final int minimize = 4;
        }
    }
}
