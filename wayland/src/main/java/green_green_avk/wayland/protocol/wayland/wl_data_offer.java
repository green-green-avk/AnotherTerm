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
 * A wl_data_offer represents a piece of data offered for transfer
 * by another client (the source client).  It is used by the
 * copy-and-paste and drag-and-drop mechanisms.  The offer
 * describes the different mime types that the data can be
 * converted to and provides the mechanism for transferring the
 * data directly from the source client.
 */
public class wl_data_offer extends WlInterface<wl_data_offer.Requests, wl_data_offer.Events> {
    public static final int version = 3;

    public interface Requests extends WlInterface.Requests {

        /**
         * accept one of the offered mime types
         *
         * Indicate that the client can accept the given mime type, or
         * NULL for not accepted.
         *
         * For objects of version 2 or older, this request is used by the
         * client to give feedback whether the client can receive the given
         * mime type, or NULL if none is accepted; the feedback does not
         * determine whether the drag-and-drop operation succeeds or not.
         *
         * For objects of version 3 or newer, this request determines the
         * final result of the drag-and-drop operation. If the end result
         * is that no mime types were accepted, the drag-and-drop operation
         * will be cancelled and the corresponding drag source will receive
         * wl_data_source.cancelled. Clients may still use this event in
         * conjunction with wl_data_source.action for feedback.
         *
         * @param serial serial number of the accept request
         * @param mime_type mime type accepted by the client
         */
        @IMethod(0)
        void accept(long serial, @INullable @Nullable String mime_type);

        /**
         * request that the data is transferred
         *
         * To transfer the offered data, the client issues this request
         * and indicates the mime type it wants to receive.  The transfer
         * happens through the passed file descriptor (typically created
         * with the pipe system call).  The source client writes the data
         * in the mime type representation requested and then closes the
         * file descriptor.
         *
         * The receiving client reads from the read end of the pipe until
         * EOF and then closes its end, at which point the transfer is
         * complete.
         *
         * This request may happen multiple times for different mime types,
         * both before and after wl_data_device.drop. Drag-and-drop destination
         * clients may preemptively fetch data or examine it more closely to
         * determine acceptance.
         *
         * @param mime_type mime type desired by receiver
         * @param fd file descriptor for data transfer
         */
        @IMethod(1)
        void receive(@NonNull String mime_type, @NonNull FileDescriptor fd);

        /**
         * destroy data offer
         *
         * Destroy the data offer.
         */
        @IMethod(2)
        @IDtor
        void destroy();

        /**
         * the offer will no longer be used
         *
         * Notifies the compositor that the drag destination successfully
         * finished the drag-and-drop operation.
         *
         * Upon receiving this request, the compositor will emit
         * wl_data_source.dnd_finished on the drag source client.
         *
         * It is a client error to perform other requests than
         * wl_data_offer.destroy after this one. It is also an error to perform
         * this request after a NULL mime type has been set in
         * wl_data_offer.accept or no action was received through
         * wl_data_offer.action.
         */
        @IMethod(3)
        @ISince(3)
        void finish();

        /**
         * set the available/preferred drag-and-drop actions
         *
         * Sets the actions that the destination side client supports for
         * this operation. This request may trigger the emission of
         * wl_data_source.action and wl_data_offer.action events if the compositor
         * needs to change the selected action.
         *
         * This request can be called multiple times throughout the
         * drag-and-drop operation, typically in response to wl_data_device.enter
         * or wl_data_device.motion events.
         *
         * This request determines the final result of the drag-and-drop
         * operation. If the end result is that no action is accepted,
         * the drag source will receive wl_drag_source.cancelled.
         *
         * The dnd_actions argument must contain only values expressed in the
         * wl_data_device_manager.dnd_actions enum, and the preferred_action
         * argument must only contain one of those values set, otherwise it
         * will result in a protocol error.
         *
         * While managing an "ask" action, the destination drag-and-drop client
         * may perform further wl_data_offer.receive requests, and is expected
         * to perform one last wl_data_offer.set_actions request with a preferred
         * action other than "ask" (and optionally wl_data_offer.accept) before
         * requesting wl_data_offer.finish, in order to convey the action selected
         * by the user. If the preferred action is not in the
         * wl_data_offer.source_actions mask, an error will be raised.
         *
         * If the "ask" action is dismissed (e.g. user cancellation), the client
         * is expected to perform wl_data_offer.destroy right away.
         *
         * This request can only be made on drag-and-drop offers, a protocol error
         * will be raised otherwise.
         *
         * @param dnd_actions actions supported by the destination client
         * @param preferred_action action preferred by the destination client
         */
        @IMethod(4)
        @ISince(3)
        void set_actions(long dnd_actions, long preferred_action);
    }

    public interface Events extends WlInterface.Events {

        /**
         * advertise offered mime type
         *
         * Sent immediately after creating the wl_data_offer object.  One
         * event per offered mime type.
         *
         * @param mime_type offered mime type
         */
        @IMethod(0)
        void offer(@NonNull String mime_type);

        /**
         * notify the source-side available actions
         *
         * This event indicates the actions offered by the data source. It
         * will be sent right after wl_data_device.enter, or anytime the source
         * side changes its offered actions through wl_data_source.set_actions.
         *
         * @param source_actions actions offered by the data source
         */
        @IMethod(1)
        @ISince(3)
        void source_actions(long source_actions);

        /**
         * notify the selected action
         *
         * This event indicates the action selected by the compositor after
         * matching the source/destination side actions. Only one action (or
         * none) will be offered here.
         *
         * This event can be emitted multiple times during the drag-and-drop
         * operation in response to destination side action changes through
         * wl_data_offer.set_actions.
         *
         * This event will no longer be emitted after wl_data_device.drop
         * happened on the drag-and-drop destination, the client must
         * honor the last action received, or the last preferred one set
         * through wl_data_offer.set_actions when handling an "ask" action.
         *
         * Compositors may also change the selected action on the fly, mainly
         * in response to keyboard modifier changes during the drag-and-drop
         * operation.
         *
         * The most recent action received is always the valid one. Prior to
         * receiving wl_data_device.drop, the chosen action may change (e.g.
         * due to keyboard modifiers being pressed). At the time of receiving
         * wl_data_device.drop the drag-and-drop destination must honor the
         * last action received.
         *
         * Action changes may still happen after wl_data_device.drop,
         * especially on "ask" actions, where the drag-and-drop destination
         * may choose another action afterwards. Action changes happening
         * at this stage are always the result of inter-client negotiation, the
         * compositor shall no longer be able to induce a different action.
         *
         * Upon "ask" actions, it is expected that the drag-and-drop destination
         * may potentially choose a different action and/or mime type,
         * based on wl_data_offer.source_actions and finally chosen by the
         * user (e.g. popping up a menu with the available options). The
         * final wl_data_offer.set_actions and wl_data_offer.accept requests
         * must happen before the call to wl_data_offer.finish.
         *
         * @param dnd_action action selected by the compositor
         */
        @IMethod(2)
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
             * finish request was called untimely
             */
            public static final int invalid_finish = 0;

            /**
             * action mask contains invalid values
             */
            public static final int invalid_action_mask = 1;

            /**
             * action argument has an invalid value
             */
            public static final int invalid_action = 2;

            /**
             * offer doesn't accept this request
             */
            public static final int invalid_offer = 3;
        }
    }
}
