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

import java.io.FileDescriptor;

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * keyboard input device
 * <p>
 * The {@code wl_keyboard} interface represents one or more keyboards
 * associated with a seat.
 */
public class wl_keyboard extends WlInterface<wl_keyboard.Requests, wl_keyboard.Events> {
    public static final int version = 8;

    public interface Requests extends WlInterface.Requests {

        /**
         * release the keyboard object
         */
        @IMethod(0)
        @ISince(3)
        @IDtor
        void release();
    }

    public interface Events extends WlInterface.Events {

        /**
         * keyboard mapping
         * <p>
         * This event provides a file descriptor to the client which can be
         * memory-mapped in read-only mode to provide a keyboard mapping
         * description.
         * <p>
         * From version 7 onwards, the fd must be mapped with MAP{@code _PRIVATE} by
         * the recipient, as MAP{@code _SHARED} may fail.
         *
         * @param format keymap format
         * @param fd     keymap file descriptor
         * @param size   keymap size, in bytes
         */
        @IMethod(0)
        void keymap(long format, @NonNull FileDescriptor fd, long size);

        /**
         * enter event
         * <p>
         * Notification that this seat's keyboard focus is on a certain
         * surface.
         * <p>
         * The compositor must send the {@code wl_keyboard.modifiers} event after this
         * event.
         *
         * @param serial  serial number of the enter event
         * @param surface surface gaining keyboard focus
         * @param keys    the currently pressed keys
         */
        @IMethod(1)
        void enter(long serial, @NonNull wl_surface surface, @NonNull int[] keys);

        /**
         * leave event
         * <p>
         * Notification that this seat's keyboard focus is no longer on
         * a certain surface.
         * <p>
         * The leave notification is sent before the enter notification
         * for the new focus.
         * <p>
         * After this event client must assume that all keys, including modifiers,
         * are lifted and also it must stop key repeating if there's some going on.
         *
         * @param serial  serial number of the leave event
         * @param surface surface that lost keyboard focus
         */
        @IMethod(2)
        void leave(long serial, @NonNull wl_surface surface);

        /**
         * key event
         * <p>
         * A key was pressed or released.
         * The time argument is a timestamp with millisecond
         * granularity, with an undefined base.
         * <p>
         * The key is a platform-specific key code that can be interpreted
         * by feeding it to the keyboard mapping (see the keymap event).
         * <p>
         * If this event produces a change in modifiers, then the resulting
         * {@code wl_keyboard.modifiers} event must be sent after this event.
         *
         * @param serial serial number of the key event
         * @param time   timestamp with millisecond granularity
         * @param key    key that produced the event
         * @param state  physical state of the key
         */
        @IMethod(3)
        void key(long serial, long time, long key, long state);

        /**
         * modifier and group state
         * <p>
         * Notifies clients that the modifier and/or group state has
         * changed, and it should update its local state.
         *
         * @param serial         serial number of the modifiers event
         * @param mods_depressed depressed modifiers
         * @param mods_latched   latched modifiers
         * @param mods_locked    locked modifiers
         * @param group          keyboard layout
         */
        @IMethod(4)
        void modifiers(long serial, long mods_depressed, long mods_latched, long mods_locked, long group);

        /**
         * repeat rate and delay
         * <p>
         * Informs the client about the keyboard's repeat rate and delay.
         * <p>
         * This event is sent as soon as the {@code wl_keyboard} object has been created,
         * and is guaranteed to be received by the client before any key press
         * event.
         * <p>
         * Negative values for either rate or delay are illegal. A rate of zero
         * will disable any repeating (regardless of the value of delay).
         * <p>
         * This event can be sent later on as well with a new value if necessary,
         * so clients should continue listening for the event past the creation
         * of {@code wl_keyboard}.
         *
         * @param rate  the rate of repeating keys in characters per second
         * @param delay delay in milliseconds since key down until repeating starts
         */
        @IMethod(5)
        @ISince(4)
        void repeat_info(int rate, int delay);
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * keyboard mapping format
         */
        public static final class Keymap_format {
            private Keymap_format() {
            }

            /**
             * no keymap; client must understand how to interpret the raw keycode
             */
            public static final int no_keymap = 0;

            /**
             * libxkbcommon compatible, null-terminated string; to determine the xkb keycode, clients must add 8 to the key event keycode
             */
            public static final int xkb_v1 = 1;
        }

        /**
         * physical key state
         */
        public static final class Key_state {
            private Key_state() {
            }

            /**
             * key is not pressed
             */
            public static final int released = 0;

            /**
             * key is pressed
             */
            public static final int pressed = 1;
        }
    }
}
