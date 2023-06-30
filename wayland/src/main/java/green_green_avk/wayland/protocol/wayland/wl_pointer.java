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
 * pointer input device
 * <p>
 * The {@code wl_pointer} interface represents one or more input devices,
 * such as mice, which control the pointer location and {@code pointer_focus}
 * of a seat.
 * <p>
 * The {@code wl_pointer} interface generates motion, enter and leave
 * events for the surfaces that the pointer is located over,
 * and button and axis events for button presses, button releases
 * and scrolling.
 */
public class wl_pointer extends WlInterface<wl_pointer.Requests, wl_pointer.Events> {
    public static final int version = 8;

    public interface Requests extends WlInterface.Requests {

        /**
         * set the pointer surface
         * <p>
         * Set the pointer surface, i.e., the surface that contains the
         * pointer image (cursor). This request gives the surface the role
         * of a cursor. If the surface already has another role, it raises
         * a protocol error.
         * <p>
         * The cursor actually changes only if the pointer
         * focus for this device is one of the requesting client's surfaces
         * or the surface parameter is the current pointer surface. If
         * there was a previous surface set with this request it is
         * replaced. If surface is NULL, the pointer image is hidden.
         * <p>
         * The parameters {@code hotspot_x} and {@code hotspot_y} define the position of
         * the pointer surface relative to the pointer location. Its
         * top-left corner is always at (x, y) - ({@code hotspot_x}, {@code hotspot_y}),
         * where (x, y) are the coordinates of the pointer location, in
         * surface-local coordinates.
         * <p>
         * On surface.attach requests to the pointer surface, {@code hotspot_x}
         * and {@code hotspot_y} are decremented by the x and y parameters
         * passed to the request. Attach must be confirmed by
         * {@code wl_surface.commit} as usual.
         * <p>
         * The hotspot can also be updated by passing the currently set
         * pointer surface to this request with new values for {@code hotspot_x}
         * and {@code hotspot_y}.
         * <p>
         * The current and pending input regions of the {@code wl_surface} are
         * cleared, and {@code wl_surface.set_input_region} is ignored until the
         * {@code wl_surface} is no longer used as the cursor. When the use as a
         * cursor ends, the current and pending input regions become
         * undefined, and the {@code wl_surface} is unmapped.
         * <p>
         * The serial parameter must match the latest {@code wl_pointer.enter}
         * serial number sent to the client. Otherwise the request will be
         * ignored.
         *
         * @param serial    serial number of the enter event
         * @param surface   pointer surface
         * @param hotspot_x surface-local x coordinate
         * @param hotspot_y surface-local y coordinate
         */
        @IMethod(0)
        void set_cursor(long serial, @INullable @Nullable wl_surface surface, int hotspot_x, int hotspot_y);

        /**
         * release the pointer object
         * <p>
         * Using this request a client can tell the server that it is not going to
         * use the pointer object anymore.
         * <p>
         * This request destroys the pointer proxy object, so clients must not call
         * {@code wl_pointer_destroy}() after using this request.
         */
        @IMethod(1)
        @ISince(3)
        @IDtor
        void release();
    }

    public interface Events extends WlInterface.Events {

        /**
         * enter event
         * <p>
         * Notification that this seat's pointer is focused on a certain
         * surface.
         * <p>
         * When a seat's focus enters a surface, the pointer image
         * is undefined and a client should respond to this event by setting
         * an appropriate pointer image with the {@code set_cursor} request.
         *
         * @param serial    serial number of the enter event
         * @param surface   surface entered by the pointer
         * @param surface_x surface-local x coordinate
         * @param surface_y surface-local y coordinate
         */
        @IMethod(0)
        void enter(long serial, @NonNull wl_surface surface, float surface_x, float surface_y);

        /**
         * leave event
         * <p>
         * Notification that this seat's pointer is no longer focused on
         * a certain surface.
         * <p>
         * The leave notification is sent before the enter notification
         * for the new focus.
         *
         * @param serial  serial number of the leave event
         * @param surface surface left by the pointer
         */
        @IMethod(1)
        void leave(long serial, @NonNull wl_surface surface);

        /**
         * pointer motion event
         * <p>
         * Notification of pointer location change. The arguments
         * {@code surface_x} and {@code surface_y} are the location relative to the
         * focused surface.
         *
         * @param time      timestamp with millisecond granularity
         * @param surface_x surface-local x coordinate
         * @param surface_y surface-local y coordinate
         */
        @IMethod(2)
        void motion(long time, float surface_x, float surface_y);

        /**
         * pointer button event
         * <p>
         * Mouse button click and release notifications.
         * <p>
         * The location of the click is given by the last motion or
         * enter event.
         * The time argument is a timestamp with millisecond
         * granularity, with an undefined base.
         * <p>
         * The button is a button code as defined in the Linux kernel's
         * linux/input-event-codes.h header file, e.g. BTN{@code _LEFT}.
         * <p>
         * Any 16-bit button code value is reserved for future additions to the
         * kernel's event code list. All other button codes above 0xFFFF are
         * currently undefined but may be used in future versions of this
         * protocol.
         *
         * @param serial serial number of the button event
         * @param time   timestamp with millisecond granularity
         * @param button button that produced the event
         * @param state  physical state of the button
         */
        @IMethod(3)
        void button(long serial, long time, long button, long state);

        /**
         * axis event
         * <p>
         * Scroll and other axis notifications.
         * <p>
         * For scroll events (vertical and horizontal scroll axes), the
         * value parameter is the length of a vector along the specified
         * axis in a coordinate space identical to those of motion events,
         * representing a relative movement along the specified axis.
         * <p>
         * For devices that support movements non-parallel to axes multiple
         * axis events will be emitted.
         * <p>
         * When applicable, for example for touch pads, the server can
         * choose to emit scroll events where the motion vector is
         * equivalent to a motion event vector.
         * <p>
         * When applicable, a client can transform its content relative to the
         * scroll distance.
         *
         * @param time  timestamp with millisecond granularity
         * @param axis  axis type
         * @param value length of vector in surface-local coordinate space
         */
        @IMethod(4)
        void axis(long time, long axis, float value);

        /**
         * end of a pointer event sequence
         * <p>
         * Indicates the end of a set of events that logically belong together.
         * A client is expected to accumulate the data in all events within the
         * frame before proceeding.
         * <p>
         * All {@code wl_pointer} events before a {@code wl_pointer.frame} event belong
         * logically together. For example, in a diagonal scroll motion the
         * compositor will send an optional {@code wl_pointer.axis_source} event, two
         * {@code wl_pointer.axis} events (horizontal and vertical) and finally a
         * {@code wl_pointer.frame} event. The client may use this information to
         * calculate a diagonal vector for scrolling.
         * <p>
         * When multiple {@code wl_pointer.axis} events occur within the same frame,
         * the motion vector is the combined motion of all events.
         * When a {@code wl_pointer.axis} and a {@code wl_pointer.axis_stop} event occur within
         * the same frame, this indicates that axis movement in one axis has
         * stopped but continues in the other axis.
         * When multiple {@code wl_pointer.axis_stop} events occur within the same
         * frame, this indicates that these axes stopped in the same instance.
         * <p>
         * A {@code wl_pointer.frame} event is sent for every logical event group,
         * even if the group only contains a single {@code wl_pointer} event.
         * Specifically, a client may get a sequence: motion, frame, button,
         * frame, axis, frame, {@code axis_stop}, frame.
         * <p>
         * The {@code wl_pointer.enter} and {@code wl_pointer.leave} events are logical events
         * generated by the compositor and not the hardware. These events are
         * also grouped by a {@code wl_pointer.frame}. When a pointer moves from one
         * surface to another, a compositor should group the
         * {@code wl_pointer.leave} event within the same {@code wl_pointer.frame}.
         * However, a client must not rely on {@code wl_pointer.leave} and
         * {@code wl_pointer.enter} being in the same {@code wl_pointer.frame}.
         * Compositor-specific policies may require the {@code wl_pointer.leave} and
         * {@code wl_pointer.enter} event being split across multiple {@code wl_pointer.frame}
         * groups.
         */
        @IMethod(5)
        @ISince(5)
        void frame();

        /**
         * axis source event
         * <p>
         * Source information for scroll and other axes.
         * <p>
         * This event does not occur on its own. It is sent before a
         * {@code wl_pointer.frame} event and carries the source information for
         * all events within that frame.
         * <p>
         * The source specifies how this event was generated. If the source is
         * {@code wl_pointer.axis_source.finger}, a {@code wl_pointer.axis_stop} event will be
         * sent when the user lifts the finger off the device.
         * <p>
         * If the source is {@code wl_pointer.axis_source.wheel},
         * {@code wl_pointer.axis_source.wheel_tilt} or
         * {@code wl_pointer.axis_source.continuous}, a {@code wl_pointer.axis_stop} event may
         * or may not be sent. Whether a compositor sends an {@code axis_stop} event
         * for these sources is hardware-specific and implementation-dependent;
         * clients must not rely on receiving an {@code axis_stop} event for these
         * scroll sources and should treat scroll sequences from these scroll
         * sources as unterminated by default.
         * <p>
         * This event is optional. If the source is unknown for a particular
         * axis event sequence, no event is sent.
         * Only one {@code wl_pointer.axis_source} event is permitted per frame.
         * <p>
         * The order of {@code wl_pointer.axis_discrete} and {@code wl_pointer.axis_source} is
         * not guaranteed.
         *
         * @param axis_source source of the axis event
         */
        @IMethod(6)
        @ISince(5)
        void axis_source(long axis_source);

        /**
         * axis stop event
         * <p>
         * Stop notification for scroll and other axes.
         * <p>
         * For some {@code wl_pointer.axis_source} types, a {@code wl_pointer.axis_stop} event
         * is sent to notify a client that the axis sequence has terminated.
         * This enables the client to implement kinetic scrolling.
         * See the {@code wl_pointer.axis_source} documentation for information on when
         * this event may be generated.
         * <p>
         * Any {@code wl_pointer.axis} events with the same {@code axis_source} after this
         * event should be considered as the start of a new axis motion.
         * <p>
         * The timestamp is to be interpreted identical to the timestamp in the
         * {@code wl_pointer.axis} event. The timestamp value may be the same as a
         * preceding {@code wl_pointer.axis} event.
         *
         * @param time timestamp with millisecond granularity
         * @param axis the axis stopped with this event
         */
        @IMethod(7)
        @ISince(5)
        void axis_stop(long time, long axis);

        /**
         * axis click event
         * <p>
         * Discrete step information for scroll and other axes.
         * <p>
         * This event carries the axis value of the {@code wl_pointer.axis} event in
         * discrete steps (e.g. mouse wheel clicks).
         * <p>
         * This event is deprecated with {@code wl_pointer} version 8 - this event is not
         * sent to clients supporting version 8 or later.
         * <p>
         * This event does not occur on its own, it is coupled with a
         * {@code wl_pointer.axis} event that represents this axis value on a
         * continuous scale. The protocol guarantees that each {@code axis_discrete}
         * event is always followed by exactly one axis event with the same
         * axis number within the same {@code wl_pointer.frame}. Note that the protocol
         * allows for other events to occur between the {@code axis_discrete} and
         * its coupled axis event, including other {@code axis_discrete} or axis
         * events. A {@code wl_pointer.frame} must not contain more than one {@code axis_discrete}
         * event per axis type.
         * <p>
         * This event is optional; continuous scrolling devices
         * like two-finger scrolling on touchpads do not have discrete
         * steps and do not generate this event.
         * <p>
         * The discrete value carries the directional information. e.g. a value
         * of -2 is two steps towards the negative direction of this axis.
         * <p>
         * The axis number is identical to the axis number in the associated
         * axis event.
         * <p>
         * The order of {@code wl_pointer.axis_discrete} and {@code wl_pointer.axis_source} is
         * not guaranteed.
         *
         * @param axis     axis type
         * @param discrete number of steps
         */
        @IMethod(8)
        @ISince(5)
        void axis_discrete(long axis, int discrete);

        /**
         * axis high-resolution scroll event
         * <p>
         * Discrete high-resolution scroll information.
         * <p>
         * This event carries high-resolution wheel scroll information,
         * with each multiple of 120 representing one logical scroll step
         * (a wheel detent). For example, an {@code axis_value120} of 30 is one quarter of
         * a logical scroll step in the positive direction, a {@code value120} of
         * -240 are two logical scroll steps in the negative direction within the
         * same hardware event.
         * Clients that rely on discrete scrolling should accumulate the
         * {@code value120} to multiples of 120 before processing the event.
         * <p>
         * The {@code value120} must not be zero.
         * <p>
         * This event replaces the {@code wl_pointer.axis_discrete} event in clients
         * supporting {@code wl_pointer} version 8 or later.
         * <p>
         * Where a {@code wl_pointer.axis_source} event occurs in the same
         * {@code wl_pointer.frame}, the axis source applies to this event.
         * <p>
         * The order of {@code wl_pointer.axis_value120} and {@code wl_pointer.axis_source} is
         * not guaranteed.
         *
         * @param axis     axis type
         * @param value120 scroll distance as fraction of 120
         */
        @IMethod(9)
        @ISince(8)
        void axis_value120(long axis, int value120);
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
        }

        /**
         * physical button state
         */
        public static final class Button_state {
            private Button_state() {
            }

            /**
             * the button is not pressed
             */
            public static final int released = 0;

            /**
             * the button is pressed
             */
            public static final int pressed = 1;
        }

        /**
         * axis types
         */
        public static final class Axis {
            private Axis() {
            }

            /**
             * vertical axis
             */
            public static final int vertical_scroll = 0;

            /**
             * horizontal axis
             */
            public static final int horizontal_scroll = 1;
        }

        /**
         * axis source types
         */
        public static final class Axis_source {
            private Axis_source() {
            }

            /**
             * a physical wheel rotation
             */
            public static final int wheel = 0;

            /**
             * finger on a touch surface
             */
            public static final int finger = 1;

            /**
             * continuous coordinate space
             */
            public static final int continuous = 2;

            /**
             * a physical wheel tilt
             */
            public static final int wheel_tilt = 3;
        }
    }
}
