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
 * compositor output region
 * <p>
 * An output describes part of the compositor geometry. The
 * compositor works in the 'compositor coordinate system' and an
 * output corresponds to a rectangular area in that space that is
 * actually visible. This typically corresponds to a monitor that
 * displays part of the compositor space. This object is published
 * as global during start up, or when a monitor is hotplugged.
 */
public class wl_output extends WlInterface<wl_output.Requests, wl_output.Events> {
    public static final int version = 4;

    public interface Requests extends WlInterface.Requests {

        /**
         * release the output object
         * <p>
         * Using this request a client can tell the server that it is not going to
         * use the output object anymore.
         */
        @IMethod(0)
        @ISince(3)
        @IDtor
        void release();
    }

    public interface Events extends WlInterface.Events {

        /**
         * properties of the output
         * <p>
         * The geometry event describes geometric properties of the output.
         * The event is sent when binding to the output object and whenever
         * any of the properties change.
         * <p>
         * The physical size can be set to zero if it doesn't make sense for this
         * output (e.g. for projectors or virtual outputs).
         * <p>
         * The geometry event will be followed by a done event (starting from
         * version 2).
         * <p>
         * Note: {@code wl_output} only advertises partial information about the output
         * position and identification. Some compositors, for instance those not
         * implementing a desktop-style output layout or those exposing virtual
         * outputs, might fake this information. Instead of using x and y, clients
         * should use {@code xdg_output.logical_position}. Instead of using make and model,
         * clients should use name and description.
         *
         * @param x               x position within the global compositor space
         * @param y               y position within the global compositor space
         * @param physical_width  width in millimeters of the output
         * @param physical_height height in millimeters of the output
         * @param subpixel        subpixel orientation of the output
         * @param make            textual description of the manufacturer
         * @param model           textual description of the model
         * @param transform       transform that maps framebuffer to output
         */
        @IMethod(0)
        void geometry(int x, int y, int physical_width, int physical_height, int subpixel, @NonNull String make, @NonNull String model, int transform);

        /**
         * advertise available modes for the output
         * <p>
         * The mode event describes an available mode for the output.
         * <p>
         * The event is sent when binding to the output object and there
         * will always be one mode, the current mode. The event is sent
         * again if an output changes mode, for the mode that is now
         * current. In other words, the current mode is always the last
         * mode that was received with the current flag set.
         * <p>
         * Non-current modes are deprecated. A compositor can decide to only
         * advertise the current mode and never send other modes. Clients
         * should not rely on non-current modes.
         * <p>
         * The size of a mode is given in physical hardware units of
         * the output device. This is not necessarily the same as
         * the output size in the global compositor space. For instance,
         * the output may be scaled, as described in {@code wl_output.scale},
         * or transformed, as described in {@code wl_output.transform}. Clients
         * willing to retrieve the output size in the global compositor
         * space should use {@code xdg_output.logical_size} instead.
         * <p>
         * The vertical refresh rate can be set to zero if it doesn't make
         * sense for this output (e.g. for virtual outputs).
         * <p>
         * The mode event will be followed by a done event (starting from
         * version 2).
         * <p>
         * Clients should not use the refresh rate to schedule frames. Instead,
         * they should use the {@code wl_surface.frame} event or the presentation-time
         * protocol.
         * <p>
         * Note: this information is not always meaningful for all outputs. Some
         * compositors, such as those exposing virtual outputs, might fake the
         * refresh rate or the size.
         *
         * @param flags   bitfield of mode flags
         * @param width   width of the mode in hardware units
         * @param height  height of the mode in hardware units
         * @param refresh vertical refresh rate in mHz
         */
        @IMethod(1)
        void mode(long flags, int width, int height, int refresh);

        /**
         * sent all information about output
         * <p>
         * This event is sent after all other properties have been
         * sent after binding to the output object and after any
         * other property changes done after that. This allows
         * changes to the output properties to be seen as
         * atomic, even if they happen via multiple events.
         */
        @IMethod(2)
        @ISince(2)
        void done();

        /**
         * output scaling properties
         * <p>
         * This event contains scaling geometry information
         * that is not in the geometry event. It may be sent after
         * binding the output object or if the output scale changes
         * later. If it is not sent, the client should assume a
         * scale of 1.
         * <p>
         * A scale larger than 1 means that the compositor will
         * automatically scale surface buffers by this amount
         * when rendering. This is used for very high resolution
         * displays where applications rendering at the native
         * resolution would be too small to be legible.
         * <p>
         * It is intended that scaling aware clients track the
         * current output of a surface, and if it is on a scaled
         * output it should use {@code wl_surface.set_buffer_scale} with
         * the scale of the output. That way the compositor can
         * avoid scaling the surface, and the client can supply
         * a higher detail image.
         * <p>
         * The scale event will be followed by a done event.
         *
         * @param factor scaling factor of output
         */
        @IMethod(3)
        @ISince(2)
        void scale(int factor);

        /**
         * name of this output
         * <p>
         * Many compositors will assign user-friendly names to their outputs, show
         * them to the user, allow the user to refer to an output, etc. The client
         * may wish to know this name as well to offer the user similar behaviors.
         * <p>
         * The name is a UTF-8 string with no convention defined for its contents.
         * Each name is unique among all {@code wl_output} globals. The name is only
         * guaranteed to be unique for the compositor instance.
         * <p>
         * The same output name is used for all clients for a given {@code wl_output}
         * global. Thus, the name can be shared across processes to refer to a
         * specific {@code wl_output} global.
         * <p>
         * The name is not guaranteed to be persistent across sessions, thus cannot
         * be used to reliably identify an output in e.g. configuration files.
         * <p>
         * Examples of names include 'HDMI-A-1', 'WL-1', 'X11-1', etc. However, do
         * not assume that the name is a reflection of an underlying DRM connector,
         * X11 connection, etc.
         * <p>
         * The name event is sent after binding the output object. This event is
         * only sent once per output object, and the name does not change over the
         * lifetime of the {@code wl_output} global.
         * <p>
         * Compositors may re-use the same output name if the {@code wl_output} global is
         * destroyed and re-created later. Compositors should avoid re-using the
         * same name if possible.
         * <p>
         * The name event will be followed by a done event.
         *
         * @param name output name
         */
        @IMethod(4)
        @ISince(4)
        void name(@NonNull String name);

        /**
         * human-readable description of this output
         * <p>
         * Many compositors can produce human-readable descriptions of their
         * outputs. The client may wish to know this description as well, e.g. for
         * output selection purposes.
         * <p>
         * The description is a UTF-8 string with no convention defined for its
         * contents. The description is not guaranteed to be unique among all
         * {@code wl_output} globals. Examples might include 'Foocorp 11" Display' or
         * 'Virtual X11 output via :1'.
         * <p>
         * The description event is sent after binding the output object and
         * whenever the description changes. The description is optional, and may
         * not be sent at all.
         * <p>
         * The description event will be followed by a done event.
         *
         * @param description output description
         */
        @IMethod(5)
        @ISince(4)
        void description(@NonNull String description);
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * subpixel geometry information
         */
        public static final class Subpixel {
            private Subpixel() {
            }

            /**
             * unknown geometry
             */
            public static final int unknown = 0;

            /**
             * no geometry
             */
            public static final int none = 1;

            /**
             * horizontal RGB
             */
            public static final int horizontal_rgb = 2;

            /**
             * horizontal BGR
             */
            public static final int horizontal_bgr = 3;

            /**
             * vertical RGB
             */
            public static final int vertical_rgb = 4;

            /**
             * vertical BGR
             */
            public static final int vertical_bgr = 5;
        }

        /**
         * transform from framebuffer to output
         */
        public static final class Transform {
            private Transform() {
            }

            /**
             * no transform
             */
            public static final int normal = 0;

            /**
             * 90 degrees counter-clockwise
             */
            public static final int _90 = 1;

            /**
             * 180 degrees counter-clockwise
             */
            public static final int _180 = 2;

            /**
             * 270 degrees counter-clockwise
             */
            public static final int _270 = 3;

            /**
             * 180 degree flip around a vertical axis
             */
            public static final int flipped = 4;

            /**
             * flip and rotate 90 degrees counter-clockwise
             */
            public static final int flipped_90 = 5;

            /**
             * flip and rotate 180 degrees counter-clockwise
             */
            public static final int flipped_180 = 6;

            /**
             * flip and rotate 270 degrees counter-clockwise
             */
            public static final int flipped_270 = 7;
        }

        /**
         * mode information
         */
        public static final class Mode {
            private Mode() {
            }

            /**
             * indicates this is the current mode
             */
            public static final int current = 0x1;

            /**
             * indicates this is the preferred mode
             */
            public static final int preferred = 0x2;
        }
    }
}
