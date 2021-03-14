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
 * group of input devices
 *
 * A seat is a group of keyboards, pointer and touch devices. This
 * object is published as a global during start up, or when such a
 * device is hot plugged.  A seat typically has a pointer and
 * maintains a keyboard focus and a pointer focus.
 */
public class wl_seat extends WlInterface<wl_seat.Requests, wl_seat.Events> {
    public static final int version = 6;

    public interface Requests extends WlInterface.Requests {

        /**
         * return pointer object
         *
         * The ID provided will be initialized to the wl_pointer interface
         * for this seat.
         *
         * This request only takes effect if the seat has the pointer
         * capability, or has had the pointer capability in the past.
         * It is a protocol violation to issue this request on a seat that has
         * never had the pointer capability.
         *
         * @param id seat pointer
         */
        @IMethod(0)
        void get_pointer(@Iface(wl_pointer.class) @NonNull NewId id);

        /**
         * return keyboard object
         *
         * The ID provided will be initialized to the wl_keyboard interface
         * for this seat.
         *
         * This request only takes effect if the seat has the keyboard
         * capability, or has had the keyboard capability in the past.
         * It is a protocol violation to issue this request on a seat that has
         * never had the keyboard capability.
         *
         * @param id seat keyboard
         */
        @IMethod(1)
        void get_keyboard(@Iface(wl_keyboard.class) @NonNull NewId id);

        /**
         * return touch object
         *
         * The ID provided will be initialized to the wl_touch interface
         * for this seat.
         *
         * This request only takes effect if the seat has the touch
         * capability, or has had the touch capability in the past.
         * It is a protocol violation to issue this request on a seat that has
         * never had the touch capability.
         *
         * @param id seat touch interface
         */
        @IMethod(2)
        void get_touch(@Iface(wl_touch.class) @NonNull NewId id);

        /**
         * release the seat object
         *
         * Using this request a client can tell the server that it is not going to
         * use the seat object anymore.
         */
        @IMethod(3)
        @ISince(5)
        @IDtor
        void release();
    }

    public interface Events extends WlInterface.Events {

        /**
         * seat capabilities changed
         *
         * This is emitted whenever a seat gains or loses the pointer,
         * keyboard or touch capabilities.  The argument is a capability
         * enum containing the complete set of capabilities this seat has.
         *
         * When the pointer capability is added, a client may create a
         * wl_pointer object using the wl_seat.get_pointer request. This object
         * will receive pointer events until the capability is removed in the
         * future.
         *
         * When the pointer capability is removed, a client should destroy the
         * wl_pointer objects associated with the seat where the capability was
         * removed, using the wl_pointer.release request. No further pointer
         * events will be received on these objects.
         *
         * In some compositors, if a seat regains the pointer capability and a
         * client has a previously obtained wl_pointer object of version 4 or
         * less, that object may start sending pointer events again. This
         * behavior is considered a misinterpretation of the intended behavior
         * and must not be relied upon by the client. wl_pointer objects of
         * version 5 or later must not send events if created before the most
         * recent event notifying the client of an added pointer capability.
         *
         * The above behavior also applies to wl_keyboard and wl_touch with the
         * keyboard and touch capabilities, respectively.
         *
         * @param capabilities capabilities of the seat
         */
        @IMethod(0)
        void capabilities(long capabilities);

        /**
         * unique identifier for this seat
         *
         * In a multiseat configuration this can be used by the client to help
         * identify which physical devices the seat represents. Based on
         * the seat configuration used by the compositor.
         *
         * @param name seat identifier
         */
        @IMethod(1)
        @ISince(2)
        void name(@NonNull String name);
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * seat capability bitmask
         */
        public static final class Capability {
            private Capability() {
            }

            /**
             * the seat has pointer devices
             */
            public static final int pointer = 1;

            /**
             * the seat has one or more keyboards
             */
            public static final int keyboard = 2;

            /**
             * the seat has touch devices
             */
            public static final int touch = 4;
        }
    }
}
