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
 * touchscreen input device
 *
 * The wl_touch interface represents a touchscreen
 * associated with a seat.
 *
 * Touch interactions can consist of one or more contacts.
 * For each contact, a series of events is generated, starting
 * with a down event, followed by zero or more motion events,
 * and ending with an up event. Events relating to the same
 * contact point can be identified by the ID of the sequence.
 */
public class wl_touch extends WlInterface<wl_touch.Requests, wl_touch.Events> {
    public static final int version = 6;

    public interface Requests extends WlInterface.Requests {

        /**
         * release the touch object
         */
        @IMethod(0)
        @ISince(3)
        @IDtor
        void release();
    }

    public interface Events extends WlInterface.Events {

        /**
         * touch down event and beginning of a touch sequence
         * <p>
         * A new touch point has appeared on the surface. This touch point is
         * assigned a unique ID. Future events from this touch point reference
         * this ID. The ID ceases to be valid after a touch up event and may be
         * reused in the future.
         *
         * @param serial  serial number of the touch down event
         * @param time    timestamp with millisecond granularity
         * @param surface surface touched
         * @param id      the unique ID of this touch point
         * @param x       surface-local x coordinate
         * @param y       surface-local y coordinate
         */
        @IMethod(0)
        void down(long serial, long time, @NonNull wl_surface surface, int id, float x, float y);

        /**
         * end of a touch event sequence
         *
         * The touch point has disappeared. No further events will be sent for
         * this touch point and the touch point's ID is released and may be
         * reused in a future touch down event.
         *
         * @param serial serial number of the touch up event
         * @param time timestamp with millisecond granularity
         * @param id the unique ID of this touch point
         */
        @IMethod(1)
        void up(long serial, long time, int id);

        /**
         * update of touch point coordinates
         *
         * A touch point has changed coordinates.
         *
         * @param time timestamp with millisecond granularity
         * @param id the unique ID of this touch point
         * @param x surface-local x coordinate
         * @param y surface-local y coordinate
         */
        @IMethod(2)
        void motion(long time, int id, float x, float y);

        /**
         * end of touch frame event
         *
         * Indicates the end of a set of events that logically belong together.
         * A client is expected to accumulate the data in all events within the
         * frame before proceeding.
         *
         * A wl_touch.frame terminates at least one event but otherwise no
         * guarantee is provided about the set of events within a frame. A client
         * must assume that any state not updated in a frame is unchanged from the
         * previously known state.
         */
        @IMethod(3)
        void frame();

        /**
         * touch session cancelled
         *
         * Sent if the compositor decides the touch stream is a global
         * gesture. No further events are sent to the clients from that
         * particular gesture. Touch cancellation applies to all touch points
         * currently active on this client's surface. The client is
         * responsible for finalizing the touch points, future touch points on
         * this surface may reuse the touch point ID.
         */
        @IMethod(4)
        void cancel();

        /**
         * update shape of touch point
         *
         * Sent when a touchpoint has changed its shape.
         *
         * This event does not occur on its own. It is sent before a
         * wl_touch.frame event and carries the new shape information for
         * any previously reported, or new touch points of that frame.
         *
         * Other events describing the touch point such as wl_touch.down,
         * wl_touch.motion or wl_touch.orientation may be sent within the
         * same wl_touch.frame. A client should treat these events as a single
         * logical touch point update. The order of wl_touch.shape,
         * wl_touch.orientation and wl_touch.motion is not guaranteed.
         * A wl_touch.down event is guaranteed to occur before the first
         * wl_touch.shape event for this touch ID but both events may occur within
         * the same wl_touch.frame.
         *
         * A touchpoint shape is approximated by an ellipse through the major and
         * minor axis length. The major axis length describes the longer diameter
         * of the ellipse, while the minor axis length describes the shorter
         * diameter. Major and minor are orthogonal and both are specified in
         * surface-local coordinates. The center of the ellipse is always at the
         * touchpoint location as reported by wl_touch.down or wl_touch.move.
         *
         * This event is only sent by the compositor if the touch device supports
         * shape reports. The client has to make reasonable assumptions about the
         * shape if it did not receive this event.
         *
         * @param id the unique ID of this touch point
         * @param major length of the major axis in surface-local coordinates
         * @param minor length of the minor axis in surface-local coordinates
         */
        @IMethod(5)
        @ISince(6)
        void shape(int id, float major, float minor);

        /**
         * update orientation of touch point
         *
         * Sent when a touchpoint has changed its orientation.
         *
         * This event does not occur on its own. It is sent before a
         * wl_touch.frame event and carries the new shape information for
         * any previously reported, or new touch points of that frame.
         *
         * Other events describing the touch point such as wl_touch.down,
         * wl_touch.motion or wl_touch.shape may be sent within the
         * same wl_touch.frame. A client should treat these events as a single
         * logical touch point update. The order of wl_touch.shape,
         * wl_touch.orientation and wl_touch.motion is not guaranteed.
         * A wl_touch.down event is guaranteed to occur before the first
         * wl_touch.orientation event for this touch ID but both events may occur
         * within the same wl_touch.frame.
         *
         * The orientation describes the clockwise angle of a touchpoint's major
         * axis to the positive surface y-axis and is normalized to the -180 to
         * +180 degree range. The granularity of orientation depends on the touch
         * device, some devices only support binary rotation values between 0 and
         * 90 degrees.
         *
         * This event is only sent by the compositor if the touch device supports
         * orientation reports.
         *
         * @param id the unique ID of this touch point
         * @param orientation angle between major axis and positive surface y-axis in degrees
         */
        @IMethod(6)
        @ISince(6)
        void orientation(int id, float orientation);
    }

    public static final class Enums {
        private Enums() {
        }
    }
}
