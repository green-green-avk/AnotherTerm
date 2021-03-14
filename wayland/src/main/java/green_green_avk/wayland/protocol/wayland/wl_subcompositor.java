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
 * sub-surface compositing
 *
 * The global interface exposing sub-surface compositing capabilities.
 * A wl_surface, that has sub-surfaces associated, is called the
 * parent surface. Sub-surfaces can be arbitrarily nested and create
 * a tree of sub-surfaces.
 *
 * The root surface in a tree of sub-surfaces is the main
 * surface. The main surface cannot be a sub-surface, because
 * sub-surfaces must always have a parent.
 *
 * A main surface with its sub-surfaces forms a (compound) window.
 * For window management purposes, this set of wl_surface objects is
 * to be considered as a single window, and it should also behave as
 * such.
 *
 * The aim of sub-surfaces is to offload some of the compositing work
 * within a window from clients to the compositor. A prime example is
 * a video player with decorations and video in separate wl_surface
 * objects. This should allow the compositor to pass YUV video buffer
 * processing to dedicated overlay hardware when possible.
 */
public class wl_subcompositor extends WlInterface<wl_subcompositor.Requests, wl_subcompositor.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * unbind from the subcompositor interface
         *
         * Informs the server that the client will not be using this
         * protocol object anymore. This does not affect any other
         * objects, wl_subsurface objects included.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * give a surface the role sub-surface
         *
         * Create a sub-surface interface for the given surface, and
         * associate it with the given parent surface. This turns a
         * plain wl_surface into a sub-surface.
         *
         * The to-be sub-surface must not already have another role, and it
         * must not have an existing wl_subsurface object. Otherwise a protocol
         * error is raised.
         *
         * Adding sub-surfaces to a parent is a double-buffered operation on the
         * parent (see wl_surface.commit). The effect of adding a sub-surface
         * becomes visible on the next time the state of the parent surface is
         * applied.
         *
         * This request modifies the behaviour of wl_surface.commit request on
         * the sub-surface, see the documentation on wl_subsurface interface.
         *
         * @param id the new sub-surface object ID
         * @param surface the surface to be turned into a sub-surface
         * @param parent the parent surface
         */
        @IMethod(1)
        void get_subsurface(@Iface(wl_subsurface.class) @NonNull NewId id, @NonNull wl_surface surface, @NonNull wl_surface parent);
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
             * the to-be sub-surface is invalid
             */
            public static final int bad_surface = 0;
        }
    }
}
