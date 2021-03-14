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
 * data transfer device
 *
 * There is one wl_data_device per seat which can be obtained
 * from the global wl_data_device_manager singleton.
 *
 * A wl_data_device provides access to inter-client data transfer
 * mechanisms such as copy-and-paste and drag-and-drop.
 */
public class wl_data_device extends WlInterface<wl_data_device.Requests, wl_data_device.Events> {
    public static final int version = 3;

    public interface Requests extends WlInterface.Requests {

        /**
         * start drag-and-drop operation
         *
         * This request asks the compositor to start a drag-and-drop
         * operation on behalf of the client.
         *
         * The source argument is the data source that provides the data
         * for the eventual data transfer. If source is NULL, enter, leave
         * and motion events are sent only to the client that initiated the
         * drag and the client is expected to handle the data passing
         * internally.
         *
         * The origin surface is the surface where the drag originates and
         * the client must have an active implicit grab that matches the
         * serial.
         *
         * The icon surface is an optional (can be NULL) surface that
         * provides an icon to be moved around with the cursor.  Initially,
         * the top-left corner of the icon surface is placed at the cursor
         * hotspot, but subsequent wl_surface.attach request can move the
         * relative position. Attach requests must be confirmed with
         * wl_surface.commit as usual. The icon surface is given the role of
         * a drag-and-drop icon. If the icon surface already has another role,
         * it raises a protocol error.
         *
         * The current and pending input regions of the icon wl_surface are
         * cleared, and wl_surface.set_input_region is ignored until the
         * wl_surface is no longer used as the icon surface. When the use
         * as an icon ends, the current and pending input regions become
         * undefined, and the wl_surface is unmapped.
         *
         * @param source data source for the eventual transfer
         * @param origin surface where the drag originates
         * @param icon drag-and-drop icon surface
         * @param serial serial number of the implicit grab on the origin
         */
        @IMethod(0)
        void start_drag(@INullable @Nullable wl_data_source source, @NonNull wl_surface origin, @INullable @Nullable wl_surface icon, long serial);

        /**
         * copy data to the selection
         *
         * This request asks the compositor to set the selection
         * to the data from the source on behalf of the client.
         *
         * To unset the selection, set the source to NULL.
         *
         * @param source data source for the selection
         * @param serial serial number of the event that triggered this request
         */
        @IMethod(1)
        void set_selection(@INullable @Nullable wl_data_source source, long serial);

        /**
         * destroy data device
         *
         * This request destroys the data device.
         */
        @IMethod(2)
        @ISince(2)
        @IDtor
        void release();
    }

    public interface Events extends WlInterface.Events {

        /**
         * introduce a new wl_data_offer
         *
         * The data_offer event introduces a new wl_data_offer object,
         * which will subsequently be used in either the
         * data_device.enter event (for drag-and-drop) or the
         * data_device.selection event (for selections).  Immediately
         * following the data_device_data_offer event, the new data_offer
         * object will send out data_offer.offer events to describe the
         * mime types it offers.
         *
         * @param id the new data_offer object
         */
        @IMethod(0)
        void data_offer(@Iface(wl_data_offer.class) @NonNull NewId id);

        /**
         * initiate drag-and-drop session
         * <p>
         * This event is sent when an active drag-and-drop pointer enters
         * a surface owned by the client.  The position of the pointer at
         * enter time is provided by the x and y arguments, in surface-local
         * coordinates.
         *
         * @param serial  serial number of the enter event
         * @param surface client surface entered
         * @param x       surface-local x coordinate
         * @param y       surface-local y coordinate
         * @param id      source data_offer object
         */
        @IMethod(1)
        void enter(long serial, @NonNull wl_surface surface, float x, float y, @INullable @Nullable wl_data_offer id);

        /**
         * end drag-and-drop session
         *
         * This event is sent when the drag-and-drop pointer leaves the
         * surface and the session ends.  The client must destroy the
         * wl_data_offer introduced at enter time at this point.
         */
        @IMethod(2)
        void leave();

        /**
         * drag-and-drop session motion
         *
         * This event is sent when the drag-and-drop pointer moves within
         * the currently focused surface. The new position of the pointer
         * is provided by the x and y arguments, in surface-local
         * coordinates.
         *
         * @param time timestamp with millisecond granularity
         * @param x surface-local x coordinate
         * @param y surface-local y coordinate
         */
        @IMethod(3)
        void motion(long time, float x, float y);

        /**
         * end drag-and-drop session successfully
         *
         * The event is sent when a drag-and-drop operation is ended
         * because the implicit grab is removed.
         *
         * The drag-and-drop destination is expected to honor the last action
         * received through wl_data_offer.action, if the resulting action is
         * "copy" or "move", the destination can still perform
         * wl_data_offer.receive requests, and is expected to end all
         * transfers with a wl_data_offer.finish request.
         *
         * If the resulting action is "ask", the action will not be considered
         * final. The drag-and-drop destination is expected to perform one last
         * wl_data_offer.set_actions request, or wl_data_offer.destroy in order
         * to cancel the operation.
         */
        @IMethod(4)
        void drop();

        /**
         * advertise new selection
         *
         * The selection event is sent out to notify the client of a new
         * wl_data_offer for the selection for this device.  The
         * data_device.data_offer and the data_offer.offer events are
         * sent out immediately before this event to introduce the data
         * offer object.  The selection event is sent to a client
         * immediately before receiving keyboard focus and when a new
         * selection is set while the client has keyboard focus.  The
         * data_offer is valid until a new data_offer or NULL is received
         * or until the client loses keyboard focus.  The client must
         * destroy the previous selection data_offer, if any, upon receiving
         * this event.
         *
         * @param id selection data_offer object
         */
        @IMethod(5)
        void selection(@INullable @Nullable wl_data_offer id);
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
    }
}
