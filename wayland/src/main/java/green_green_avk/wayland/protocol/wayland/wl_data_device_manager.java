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
 * data transfer interface
 * <p>
 * The {@code wl_data_device_manager} is a singleton global object that
 * provides access to inter-client data transfer mechanisms such as
 * copy-and-paste and drag-and-drop. These mechanisms are tied to
 * a {@code wl_seat} and this interface lets a client get a {@code wl_data_device}
 * corresponding to a {@code wl_seat}.
 * <p>
 * Depending on the version bound, the objects created from the bound
 * {@code wl_data_device_manager} object will have different requirements for
 * functioning properly. See {@code wl_data_source.set_actions},
 * {@code wl_data_offer.accept} and {@code wl_data_offer.finish} for details.
 */
public class wl_data_device_manager extends WlInterface<wl_data_device_manager.Requests, wl_data_device_manager.Events> {
    public static final int version = 3;

    public interface Requests extends WlInterface.Requests {

        /**
         * create a new data source
         * <p>
         * Create a new data source.
         *
         * @param id data source to create
         */
        @IMethod(0)
        void create_data_source(@Iface(wl_data_source.class) @NonNull NewId id);

        /**
         * create a new data device
         * <p>
         * Create a new data device for a given seat.
         *
         * @param id   data device to create
         * @param seat seat associated with the data device
         */
        @IMethod(1)
        void get_data_device(@Iface(wl_data_device.class) @NonNull NewId id, @NonNull wl_seat seat);
    }

    public interface Events extends WlInterface.Events {
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * drag and drop actions
         */
        public static final class Dnd_action {
            private Dnd_action() {
            }

            /**
             * no action
             */
            public static final int none = 0;

            /**
             * copy action
             */
            public static final int copy = 1;

            /**
             * move action
             */
            public static final int move = 2;

            /**
             * ask action
             */
            public static final int ask = 4;
        }
    }
}
