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
 * sub-surface interface to a {@code wl_surface}
 * <p>
 * An additional interface to a {@code wl_surface} object, which has been
 * made a sub-surface. A sub-surface has one parent surface. A
 * sub-surface's size and position are not limited to that of the parent.
 * Particularly, a sub-surface is not automatically clipped to its
 * parent's area.
 * <p>
 * A sub-surface becomes mapped, when a non-NULL {@code wl_buffer} is applied
 * and the parent surface is mapped. The order of which one happens
 * first is irrelevant. A sub-surface is hidden if the parent becomes
 * hidden, or if a NULL {@code wl_buffer} is applied. These rules apply
 * recursively through the tree of surfaces.
 * <p>
 * The behaviour of a {@code wl_surface.commit} request on a sub-surface
 * depends on the sub-surface's mode. The possible modes are
 * synchronized and desynchronized, see methods
 * {@code wl_subsurface.set_sync} and {@code wl_subsurface.set_desync}. Synchronized
 * mode caches the {@code wl_surface} state to be applied when the parent's
 * state gets applied, and desynchronized mode applies the pending
 * {@code wl_surface} state directly. A sub-surface is initially in the
 * synchronized mode.
 * <p>
 * Sub-surfaces also have another kind of state, which is managed by
 * {@code wl_subsurface} requests, as opposed to {@code wl_surface} requests. This
 * state includes the sub-surface position relative to the parent
 * surface ({@code wl_subsurface.set_position}), and the stacking order of
 * the parent and its sub-surfaces ({@code wl_subsurface.place_above} and
 * .{@code place_below}). This state is applied when the parent surface's
 * {@code wl_surface} state is applied, regardless of the sub-surface's mode.
 * As the exception, {@code set_sync} and {@code set_desync} are effective immediately.
 * <p>
 * The main surface can be thought to be always in desynchronized mode,
 * since it does not have a parent in the sub-surfaces sense.
 * <p>
 * Even if a sub-surface is in desynchronized mode, it will behave as
 * in synchronized mode, if its parent surface behaves as in
 * synchronized mode. This rule is applied recursively throughout the
 * tree of surfaces. This means, that one can set a sub-surface into
 * synchronized mode, and then assume that all its child and grand-child
 * sub-surfaces are synchronized, too, without explicitly setting them.
 * <p>
 * If the {@code wl_surface} associated with the {@code wl_subsurface} is destroyed, the
 * {@code wl_subsurface} object becomes inert. Note, that destroying either object
 * takes effect immediately. If you need to synchronize the removal
 * of a sub-surface to the parent surface update, unmap the sub-surface
 * first by attaching a NULL {@code wl_buffer}, update parent, and then destroy
 * the sub-surface.
 * <p>
 * If the parent {@code wl_surface} object is destroyed, the sub-surface is
 * unmapped.
 */
public class wl_subsurface extends WlInterface<wl_subsurface.Requests, wl_subsurface.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * remove sub-surface interface
         * <p>
         * The sub-surface interface is removed from the {@code wl_surface} object
         * that was turned into a sub-surface with a
         * {@code wl_subcompositor.get_subsurface} request. The {@code wl_surface}'s association
         * to the parent is deleted, and the {@code wl_surface} loses its role as
         * a sub-surface. The {@code wl_surface} is unmapped immediately.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * reposition the sub-surface
         * <p>
         * This schedules a sub-surface position change.
         * The sub-surface will be moved so that its origin (top left
         * corner pixel) will be at the location x, y of the parent surface
         * coordinate system. The coordinates are not restricted to the parent
         * surface area. Negative values are allowed.
         * <p>
         * The scheduled coordinates will take effect whenever the state of the
         * parent surface is applied. When this happens depends on whether the
         * parent surface is in synchronized mode or not. See
         * {@code wl_subsurface.set_sync} and {@code wl_subsurface.set_desync} for details.
         * <p>
         * If more than one {@code set_position} request is invoked by the client before
         * the commit of the parent surface, the position of a new request always
         * replaces the scheduled position from any previous request.
         * <p>
         * The initial position is 0, 0.
         *
         * @param x x coordinate in the parent surface
         * @param y y coordinate in the parent surface
         */
        @IMethod(1)
        void set_position(int x, int y);

        /**
         * restack the sub-surface
         * <p>
         * This sub-surface is taken from the stack, and put back just
         * above the reference surface, changing the z-order of the sub-surfaces.
         * The reference surface must be one of the sibling surfaces, or the
         * parent surface. Using any other surface, including this sub-surface,
         * will cause a protocol error.
         * <p>
         * The z-order is double-buffered. Requests are handled in order and
         * applied immediately to a pending state. The final pending state is
         * copied to the active state the next time the state of the parent
         * surface is applied. When this happens depends on whether the parent
         * surface is in synchronized mode or not. See {@code wl_subsurface.set_sync} and
         * {@code wl_subsurface.set_desync} for details.
         * <p>
         * A new sub-surface is initially added as the top-most in the stack
         * of its siblings and parent.
         *
         * @param sibling the reference surface
         */
        @IMethod(2)
        void place_above(@NonNull wl_surface sibling);

        /**
         * restack the sub-surface
         * <p>
         * The sub-surface is placed just below the reference surface.
         * See {@code wl_subsurface.place_above}.
         *
         * @param sibling the reference surface
         */
        @IMethod(3)
        void place_below(@NonNull wl_surface sibling);

        /**
         * set sub-surface to synchronized mode
         * <p>
         * Change the commit behaviour of the sub-surface to synchronized
         * mode, also described as the parent dependent mode.
         * <p>
         * In synchronized mode, {@code wl_surface.commit} on a sub-surface will
         * accumulate the committed state in a cache, but the state will
         * not be applied and hence will not change the compositor output.
         * The cached state is applied to the sub-surface immediately after
         * the parent surface's state is applied. This ensures atomic
         * updates of the parent and all its synchronized sub-surfaces.
         * Applying the cached state will invalidate the cache, so further
         * parent surface commits do not (re-)apply old state.
         * <p>
         * See {@code wl_subsurface} for the recursive effect of this mode.
         */
        @IMethod(4)
        void set_sync();

        /**
         * set sub-surface to desynchronized mode
         * <p>
         * Change the commit behaviour of the sub-surface to desynchronized
         * mode, also described as independent or freely running mode.
         * <p>
         * In desynchronized mode, {@code wl_surface.commit} on a sub-surface will
         * apply the pending state directly, without caching, as happens
         * normally with a {@code wl_surface}. Calling {@code wl_surface.commit} on the
         * parent surface has no effect on the sub-surface's {@code wl_surface}
         * state. This mode allows a sub-surface to be updated on its own.
         * <p>
         * If cached state exists when {@code wl_surface.commit} is called in
         * desynchronized mode, the pending state is added to the cached
         * state, and applied as a whole. This invalidates the cache.
         * <p>
         * Note: even if a sub-surface is set to desynchronized, a parent
         * sub-surface may override it to behave as synchronized. For details,
         * see {@code wl_subsurface}.
         * <p>
         * If a surface's parent surface behaves as desynchronized, then
         * the cached state is applied on {@code set_desync}.
         */
        @IMethod(5)
        void set_desync();
    }

    public interface Events extends WlInterface.Events {
    }

    public static final class Enums {
        private Enums() {
        }

        public static final class Error {
            private Error() {
            }

            /**
             * wl_surface is not a sibling or the parent
             */
            public static final int bad_surface = 0;
        }
    }
}
