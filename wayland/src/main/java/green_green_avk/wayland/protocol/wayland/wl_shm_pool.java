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
 * a shared memory pool
 * <p>
 * The wl_shm_pool object encapsulates a piece of memory shared
 * between the compositor and client.  Through the wl_shm_pool
 * object, the client can allocate shared memory wl_buffer objects.
 * All objects created through the same pool share the same
 * underlying mapped memory. Reusing the mapped memory avoids the
 * setup/teardown overhead and is useful when interactively resizing
 * a surface or for many small buffers.
 */
public class wl_shm_pool extends WlInterface<wl_shm_pool.Requests, wl_shm_pool.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * create a buffer from the pool
         * <p>
         * Create a wl_buffer object from the pool.
         * <p>
         * The buffer is created offset bytes into the pool and has
         * width and height as specified.  The stride argument specifies
         * the number of bytes from the beginning of one row to the beginning
         * of the next.  The format is the pixel format of the buffer and
         * must be one of those advertised through the wl_shm.format event.
         * <p>
         * A buffer will keep a reference to the pool it was created from
         * so it is valid to destroy the pool immediately after creating
         * a buffer from it.
         *
         * @param id     buffer to create
         * @param offset buffer byte offset within the pool
         * @param width  buffer width, in pixels
         * @param height buffer height, in pixels
         * @param stride number of bytes from the beginning of one row to the beginning of the next row
         * @param format buffer pixel format
         */
        @IMethod(0)
        void create_buffer(@Iface(wl_buffer.class) @NonNull NewId id, int offset, int width, int height, int stride, long format);

        /**
         * destroy the pool
         * <p>
         * Destroy the shared memory pool.
         * <p>
         * The mmapped memory will be released when all
         * buffers that have been created from this pool
         * are gone.
         */
        @IMethod(1)
        @IDtor
        void destroy();

        /**
         * change the size of the pool mapping
         * <p>
         * This request will cause the server to remap the backing memory
         * for the pool from the file descriptor passed when the pool was
         * created, but using the new size.  This request can only be
         * used to make the pool bigger.
         *
         * @param size new size of the pool, in bytes
         */
        @IMethod(2)
        void resize(int size);
    }

    public interface Events extends WlInterface.Events {
    }

    public static final class Enums {
        private Enums() {
        }
    }
}
