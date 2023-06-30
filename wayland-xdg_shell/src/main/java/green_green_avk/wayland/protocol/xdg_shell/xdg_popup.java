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

import green_green_avk.wayland.protocol.wayland.wl_seat;
import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * short-lived, popup surfaces for menus
 * <p>
 * A popup surface is a short-lived, temporary surface. It can be used to
 * implement for example menus, popovers, tooltips and other similar user
 * interface concepts.
 * <p>
 * A popup can be made to take an explicit grab. See {@code xdg_popup.grab} for
 * details.
 * <p>
 * When the popup is dismissed, a {@code popup_done} event will be sent out, and at
 * the same time the surface will be unmapped. See the {@code xdg_popup.popup_done}
 * event for details.
 * <p>
 * Explicitly destroying the {@code xdg_popup} object will also dismiss the popup and
 * unmap the surface. Clients that want to dismiss the popup when another
 * surface of their own is clicked should dismiss the popup using the destroy
 * request.
 * <p>
 * A newly created {@code xdg_popup} will be stacked on top of all previously created
 * {@code xdg_popup} surfaces associated with the same {@code xdg_toplevel}.
 * <p>
 * The parent of an {@code xdg_popup} must be mapped (see the {@code xdg_surface}
 * description) before the {@code xdg_popup} itself.
 * <p>
 * The client must call {@code wl_surface.commit} on the corresponding {@code wl_surface}
 * for the {@code xdg_popup} state to take effect.
 */
public class xdg_popup extends WlInterface<xdg_popup.Requests, xdg_popup.Events> {
    public static final int version = 5;

    public interface Requests extends WlInterface.Requests {

        /**
         * remove {@code xdg_popup} interface
         * <p>
         * This destroys the popup. Explicitly destroying the {@code xdg_popup}
         * object will also dismiss the popup, and unmap the surface.
         * <p>
         * If this {@code xdg_popup} is not the "topmost" popup, a protocol error
         * will be sent.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * make the popup take an explicit grab
         * <p>
         * This request makes the created popup take an explicit grab. An explicit
         * grab will be dismissed when the user dismisses the popup, or when the
         * client destroys the {@code xdg_popup}. This can be done by the user clicking
         * outside the surface, using the keyboard, or even locking the screen
         * through closing the lid or a timeout.
         * <p>
         * If the compositor denies the grab, the popup will be immediately
         * dismissed.
         * <p>
         * This request must be used in response to some sort of user action like a
         * button press, key press, or touch down event. The serial number of the
         * event should be passed as 'serial'.
         * <p>
         * The parent of a grabbing popup must either be an {@code xdg_toplevel} surface or
         * another {@code xdg_popup} with an explicit grab. If the parent is another
         * {@code xdg_popup} it means that the popups are nested, with this popup now being
         * the topmost popup.
         * <p>
         * Nested popups must be destroyed in the reverse order they were created
         * in, e.g. the only popup you are allowed to destroy at all times is the
         * topmost one.
         * <p>
         * When compositors choose to dismiss a popup, they may dismiss every
         * nested grabbing popup as well. When a compositor dismisses popups, it
         * will follow the same dismissing order as required from the client.
         * <p>
         * If the topmost grabbing popup is destroyed, the grab will be returned to
         * the parent of the popup, if that parent previously had an explicit grab.
         * <p>
         * If the parent is a grabbing popup which has already been dismissed, this
         * popup will be immediately dismissed. If the parent is a popup that did
         * not take an explicit grab, an error will be raised.
         * <p>
         * During a popup grab, the client owning the grab will receive pointer
         * and touch events for all their surfaces as normal (similar to an
         * "owner-events" grab in X11 parlance), while the top most grabbing popup
         * will always have keyboard focus.
         *
         * @param seat   the {@code wl_seat} of the user event
         * @param serial the serial of the user event
         */
        @IMethod(1)
        void grab(@NonNull wl_seat seat, long serial);

        /**
         * recalculate the popup's location
         * <p>
         * Reposition an already-mapped popup. The popup will be placed given the
         * details in the passed {@code xdg_positioner} object, and a
         * {@code xdg_popup.repositioned} followed by {@code xdg_popup.configure} and
         * {@code xdg_surface.configure} will be emitted in response. Any parameters set
         * by the previous positioner will be discarded.
         * <p>
         * The passed token will be sent in the corresponding
         * {@code xdg_popup.repositioned} event. The new popup position will not take
         * effect until the corresponding configure event is acknowledged by the
         * client. See {@code xdg_popup.repositioned} for details. The token itself is
         * opaque, and has no other special meaning.
         * <p>
         * If multiple reposition requests are sent, the compositor may skip all
         * but the last one.
         * <p>
         * If the popup is repositioned in response to a configure event for its
         * parent, the client should send an {@code xdg_positioner.set_parent_configure}
         * and possibly an {@code xdg_positioner.set_parent_size} request to allow the
         * compositor to properly constrain the popup.
         * <p>
         * If the popup is repositioned together with a parent that is being
         * resized, but not in response to a configure event, the client should
         * send an {@code xdg_positioner.set_parent_size} request.
         *
         * @param positioner ...
         * @param token      reposition request token
         */
        @IMethod(2)
        @ISince(3)
        void reposition(@NonNull xdg_positioner positioner, long token);
    }

    public interface Events extends WlInterface.Events {

        /**
         * configure the popup surface
         * <p>
         * This event asks the popup surface to configure itself given the
         * configuration. The configured state should not be applied immediately.
         * See {@code xdg_surface.configure} for details.
         * <p>
         * The x and y arguments represent the position the popup was placed at
         * given the {@code xdg_positioner} rule, relative to the upper left corner of the
         * window geometry of the parent surface.
         * <p>
         * For version 2 or older, the configure event for an {@code xdg_popup} is only
         * ever sent once for the initial configuration. Starting with version 3,
         * it may be sent again if the popup is setup with an {@code xdg_positioner} with
         * {@code set_reactive} requested, or in response to {@code xdg_popup.reposition} requests.
         *
         * @param x      x position relative to parent surface window geometry
         * @param y      y position relative to parent surface window geometry
         * @param width  window geometry width
         * @param height window geometry height
         */
        @IMethod(0)
        void configure(int x, int y, int width, int height);

        /**
         * popup interaction is done
         * <p>
         * The {@code popup_done} event is sent out when a popup is dismissed by the
         * compositor. The client should destroy the {@code xdg_popup} object at this
         * point.
         */
        @IMethod(1)
        void popup_done();

        /**
         * signal the completion of a repositioned request
         * <p>
         * The repositioned event is sent as part of a popup configuration
         * sequence, together with {@code xdg_popup.configure} and lastly
         * {@code xdg_surface.configure} to notify the completion of a reposition request.
         * <p>
         * The repositioned event is to notify about the completion of a
         * {@code xdg_popup.reposition} request. The token argument is the token passed
         * in the {@code xdg_popup.reposition} request.
         * <p>
         * Immediately after this event is emitted, {@code xdg_popup.configure} and
         * {@code xdg_surface.configure} will be sent with the updated size and position,
         * as well as a new configure serial.
         * <p>
         * The client should optionally update the content of the popup, but must
         * acknowledge the new popup configuration for the new position to take
         * effect. See {@code xdg_surface.ack_configure} for details.
         *
         * @param token reposition request token
         */
        @IMethod(2)
        @ISince(3)
        void repositioned(long token);
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Error {
            private Error() {
            }

            /**
             * tried to grab after being mapped
             */
            public static final int invalid_grab = 0;
        }
    }
}
