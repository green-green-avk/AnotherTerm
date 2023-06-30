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
 * the compositor singleton
 * <p>
 * A compositor. This object is a singleton global. The
 * compositor is in charge of combining the contents of multiple
 * surfaces into one displayable output.
 */
public class wl_compositor extends WlInterface<wl_compositor.Requests, wl_compositor.Events> {
    public static final int version = 5;

    public interface Requests extends WlInterface.Requests {

        /**
         * create new surface
         * <p>
         * Ask the compositor to create a new surface.
         *
         * @param id the new surface
         */
        @IMethod(0)
        void create_surface(@Iface(wl_surface.class) @NonNull NewId id);

        /**
         * create new region
         * <p>
         * Ask the compositor to create a new region.
         *
         * @param id the new region
         */
        @IMethod(1)
        void create_region(@Iface(wl_region.class) @NonNull NewId id);
    }

    public interface Events extends WlInterface.Events {
    }

    public static final class Enums {
        private Enums() {
        }
    }
}
