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

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * region interface
 *
 * A region object describes an area.
 *
 * Region objects are used to describe the opaque and input
 * regions of a surface.
 */
public class wl_region extends WlInterface<wl_region.Requests, wl_region.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * destroy region
         *
         * Destroy the region.  This will invalidate the object ID.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * add rectangle to region
         * <p>
         * Add the specified rectangle to the region.
         *
         * @param x      region-local x coordinate
         * @param y      region-local y coordinate
         * @param width  rectangle width
         * @param height rectangle height
         */
        @IMethod(1)
        void add(int x, int y, int width, int height);

        /**
         * subtract rectangle from region
         * <p>
         * Subtract the specified rectangle from the region.
         *
         * @param x      region-local x coordinate
         * @param y      region-local y coordinate
         * @param width  rectangle width
         * @param height rectangle height
         */
        @IMethod(2)
        void subtract(int x, int y, int width, int height);
    }

    public interface Events extends WlInterface.Events {
    }

    public static final class Enums {
        private Enums() {
        }
    }
}
