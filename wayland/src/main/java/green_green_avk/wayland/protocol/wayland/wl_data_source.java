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

import java.io.FileDescriptor;

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * offer to transfer data
 *
 * The wl_data_source object is the source side of a wl_data_offer.
 * It is created by the source client in a data transfer and
 * provides a way to describe the offered data and a way to respond
 * to requests to transfer the data.
 */
public class wl_data_source extends WlInterface<wl_data_source.Requests, wl_data_source.Events> {
    public static final int version = 3;

    public interface Requests extends WlInterface.Requests {

        /**
         * add an offered mime type
         *
         * This request adds a mime type to the set of mime types
         * advertised to targets.  Can be called several times to offer
         * multiple types.
         *
         * @param mime_type mime type offered by the data source
         */
        @IMethod(0)
        void offer(@NonNull String mime_type);

        /**
         * destroy the data source
         *
         * Destroy the data source.
         */
        @IMethod(1)
        @IDtor
        void destroy();

        /**
         * set the available drag-and-drop actions
         *
         * Sets the actions that the source side client supports for this
         * operation. This request may trigger wl_data_source.action and
         * wl_data_offer.action events if the compositor needs to change the
         * selected action.
         *
         * The dnd_actions argument must contain only values expressed in the
         * wl_data_device_manager.dnd_actions enum, otherwise it will result
         * in a protocol error.
         *
         * This request must be made once only, and can only be made on sources
         * used in drag-and-drop, so it must be performed before
         * wl_data_device.start_drag. Attempting to use the source other than
         * for drag-and-drop will raise a protocol error.
         *
         * @param dnd_actions actions supported by the data source
         */
        @IMethod(2)
        @ISince(3)
        void set_actions(long dnd_actions);
    }

    public interface Events extends WlInterface.Events {

        /**
         * a target accepts an offered mime type
         *
         * Sent when a target accepts pointer_focus or motion events.  If
         * a target does not accept any of the offered types, type is NULL.
         *
         * Used for feedback during drag-and-drop.
         *
         * @param mime_type mime type accepted by the target
         */
        @IMethod(0)
        void target(@INullable @Nullable String mime_type);

        /**
         * send the data
         * <p>
         * Request for data from the client.  Send the data as the
         * specified mime type over the passed file descriptor, then
         * close it.
         *
         * @param mime_type mime type for the data
         * @param fd        file descriptor for the data
         */
        @IMethod(1)
        void send(@NonNull String mime_type, @NonNull FileDescriptor fd);

        /**
         * selection was cancelled
         *
         * This data source is no longer valid. There are several reasons why
         * this could happen:
         *
         * - The data source has been replaced by another data source.
         * - The drag-and-drop operation was performed, but the drop destination
         *   did not accept any of the mime types offered through
         *   wl_data_source.target.
         * - The drag-and-drop operation was performed, but the drop destination
         *   did not select any of the actions present in the mask offered through
         *   wl_data_source.action.
         * - The drag-and-drop operation was performed but didn't happen over a
         *   surface.
         * - The compositor cancelled the drag-and-drop operation (e.g. compositor
         *   dependent timeouts to avoid stale drag-and-drop transfers).
         *
         * The client should clean up and destroy this data source.
         *
         * For objects of version 2 or older, wl_data_source.cancelled will
         * only be emitted if the data source was replaced by another data
         * source.
         */
        @IMethod(2)
        void cancelled();

        /**
         * the drag-and-drop operation physically finished
         *
         * The user performed the drop action. This event does not indicate
         * acceptance, wl_data_source.cancelled may still be emitted afterwards
         * if the drop destination does not accept any mime type.
         *
         * However, this event might however not be received if the compositor
         * cancelled the drag-and-drop operation before this event could happen.
         *
         * Note that the data_source may still be used in the future and should
         * not be destroyed here.
         */
        @IMethod(3)
        @ISince(3)
        void dnd_drop_performed();

        /**
         * the drag-and-drop operation concluded
         *
         * The drop destination finished interoperating with this data
         * source, so the client is now free to destroy this data source and
         * free all associated data.
         *
         * If the action used to perform the operation was "move", the
         * source can now delete the transferred data.
         */
        @IMethod(4)
        @ISince(3)
        void dnd_finished();

        /**
         * notify the selected action
         *
         * This event indicates the action selected by the compositor after
         * matching the source/destination side actions. Only one action (or
         * none) will be offered here.
         *
         * This event can be emitted multiple times during the drag-and-drop
         * operation, mainly in response to destination side changes through
         * wl_data_offer.set_actions, and as the data device enters/leaves
         * surfaces.
         *
         * It is only possible to receive this event after
         * wl_data_source.dnd_drop_performed if the drag-and-drop operation
         * ended in an "ask" action, in which case the final wl_data_source.action
         * event will happen immediately before wl_data_source.dnd_finished.
         *
         * Compositors may also change the selected action on the fly, mainly
         * in response to keyboard modifier changes during the drag-and-drop
         * operation.
         *
         * The most recent action received is always the valid one. The chosen
         * action may change alongside negotiation (e.g. an "ask" action can turn
         * into a "move" operation), so the effects of the final action must
         * always be applied in wl_data_offer.dnd_finished.
         *
         * Clients can trigger cursor surface changes from this point, so
         * they reflect the current action.
         *
         * @param dnd_action action selected by the compositor
         */
        @IMethod(5)
        @ISince(3)
        void action(long dnd_action);
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Error {
            private Error() {
            }

            /**
             * action mask contains invalid values
             */
            public static final int invalid_action_mask = 0;

            /**
             * source doesn't accept this request
             */
            public static final int invalid_source = 1;
        }
    }
}
