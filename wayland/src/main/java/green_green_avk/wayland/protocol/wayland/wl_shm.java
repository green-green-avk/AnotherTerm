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
 * shared memory support
 * <p>
 * A singleton global object that provides support for shared
 * memory.
 * <p>
 * Clients can create wl_shm_pool objects using the create_pool
 * request.
 * <p>
 * At connection setup time, the wl_shm object emits one or more
 * format events to inform clients about the valid pixel formats
 * that can be used for buffers.
 */
public class wl_shm extends WlInterface<wl_shm.Requests, wl_shm.Events> {
    public static final int version = 1;

    public interface Requests extends WlInterface.Requests {

        /**
         * create a shm pool
         * <p>
         * Create a new wl_shm_pool object.
         * <p>
         * The pool can be used to create shared memory based buffer
         * objects.  The server will mmap size bytes of the passed file
         * descriptor, to use as backing memory for the pool.
         *
         * @param id   pool to create
         * @param fd   file descriptor for the pool
         * @param size pool size, in bytes
         */
        @IMethod(0)
        void create_pool(@Iface(wl_shm_pool.class) @NonNull NewId id, @NonNull FileDescriptor fd, int size);
    }

    public interface Events extends WlInterface.Events {

        /**
         * pixel format description
         * <p>
         * Informs the client about a valid pixel format that
         * can be used for buffers. Known formats include
         * argb8888 and xrgb8888.
         *
         * @param format buffer pixel format
         */
        @IMethod(0)
        void format(long format);
    }

    public static final class Enums {
        private Enums() {
        }

        /**
         * wl_shm error values
         */
        public static final class Error {
            private Error() {
            }

            /**
             * buffer format is not known
             */
            public static final int invalid_format = 0;

            /**
             * invalid size or stride during pool or buffer creation
             */
            public static final int invalid_stride = 1;

            /**
             * mmapping the file descriptor failed
             */
            public static final int invalid_fd = 2;
        }

        /**
         * pixel formats
         */
        public static final class Format {
            private Format() {
            }

            /**
             * 32-bit ARGB format, [31:0] A:R:G:B 8:8:8:8 little endian
             */
            public static final int argb8888 = 0;

            /**
             * 32-bit RGB format, [31:0] x:R:G:B 8:8:8:8 little endian
             */
            public static final int xrgb8888 = 1;

            /**
             * 8-bit color index format, [7:0] C
             */
            public static final int c8 = 0x20203843;

            /**
             * 8-bit RGB format, [7:0] R:G:B 3:3:2
             */
            public static final int rgb332 = 0x38424752;

            /**
             * 8-bit BGR format, [7:0] B:G:R 2:3:3
             */
            public static final int bgr233 = 0x38524742;

            /**
             * 16-bit xRGB format, [15:0] x:R:G:B 4:4:4:4 little endian
             */
            public static final int xrgb4444 = 0x32315258;

            /**
             * 16-bit xBGR format, [15:0] x:B:G:R 4:4:4:4 little endian
             */
            public static final int xbgr4444 = 0x32314258;

            /**
             * 16-bit RGBx format, [15:0] R:G:B:x 4:4:4:4 little endian
             */
            public static final int rgbx4444 = 0x32315852;

            /**
             * 16-bit BGRx format, [15:0] B:G:R:x 4:4:4:4 little endian
             */
            public static final int bgrx4444 = 0x32315842;

            /**
             * 16-bit ARGB format, [15:0] A:R:G:B 4:4:4:4 little endian
             */
            public static final int argb4444 = 0x32315241;

            /**
             * 16-bit ABGR format, [15:0] A:B:G:R 4:4:4:4 little endian
             */
            public static final int abgr4444 = 0x32314241;

            /**
             * 16-bit RBGA format, [15:0] R:G:B:A 4:4:4:4 little endian
             */
            public static final int rgba4444 = 0x32314152;

            /**
             * 16-bit BGRA format, [15:0] B:G:R:A 4:4:4:4 little endian
             */
            public static final int bgra4444 = 0x32314142;

            /**
             * 16-bit xRGB format, [15:0] x:R:G:B 1:5:5:5 little endian
             */
            public static final int xrgb1555 = 0x35315258;

            /**
             * 16-bit xBGR 1555 format, [15:0] x:B:G:R 1:5:5:5 little endian
             */
            public static final int xbgr1555 = 0x35314258;

            /**
             * 16-bit RGBx 5551 format, [15:0] R:G:B:x 5:5:5:1 little endian
             */
            public static final int rgbx5551 = 0x35315852;

            /**
             * 16-bit BGRx 5551 format, [15:0] B:G:R:x 5:5:5:1 little endian
             */
            public static final int bgrx5551 = 0x35315842;

            /**
             * 16-bit ARGB 1555 format, [15:0] A:R:G:B 1:5:5:5 little endian
             */
            public static final int argb1555 = 0x35315241;

            /**
             * 16-bit ABGR 1555 format, [15:0] A:B:G:R 1:5:5:5 little endian
             */
            public static final int abgr1555 = 0x35314241;

            /**
             * 16-bit RGBA 5551 format, [15:0] R:G:B:A 5:5:5:1 little endian
             */
            public static final int rgba5551 = 0x35314152;

            /**
             * 16-bit BGRA 5551 format, [15:0] B:G:R:A 5:5:5:1 little endian
             */
            public static final int bgra5551 = 0x35314142;

            /**
             * 16-bit RGB 565 format, [15:0] R:G:B 5:6:5 little endian
             */
            public static final int rgb565 = 0x36314752;

            /**
             * 16-bit BGR 565 format, [15:0] B:G:R 5:6:5 little endian
             */
            public static final int bgr565 = 0x36314742;

            /**
             * 24-bit RGB format, [23:0] R:G:B little endian
             */
            public static final int rgb888 = 0x34324752;

            /**
             * 24-bit BGR format, [23:0] B:G:R little endian
             */
            public static final int bgr888 = 0x34324742;

            /**
             * 32-bit xBGR format, [31:0] x:B:G:R 8:8:8:8 little endian
             */
            public static final int xbgr8888 = 0x34324258;

            /**
             * 32-bit RGBx format, [31:0] R:G:B:x 8:8:8:8 little endian
             */
            public static final int rgbx8888 = 0x34325852;

            /**
             * 32-bit BGRx format, [31:0] B:G:R:x 8:8:8:8 little endian
             */
            public static final int bgrx8888 = 0x34325842;

            /**
             * 32-bit ABGR format, [31:0] A:B:G:R 8:8:8:8 little endian
             */
            public static final int abgr8888 = 0x34324241;

            /**
             * 32-bit RGBA format, [31:0] R:G:B:A 8:8:8:8 little endian
             */
            public static final int rgba8888 = 0x34324152;

            /**
             * 32-bit BGRA format, [31:0] B:G:R:A 8:8:8:8 little endian
             */
            public static final int bgra8888 = 0x34324142;

            /**
             * 32-bit xRGB format, [31:0] x:R:G:B 2:10:10:10 little endian
             */
            public static final int xrgb2101010 = 0x30335258;

            /**
             * 32-bit xBGR format, [31:0] x:B:G:R 2:10:10:10 little endian
             */
            public static final int xbgr2101010 = 0x30334258;

            /**
             * 32-bit RGBx format, [31:0] R:G:B:x 10:10:10:2 little endian
             */
            public static final int rgbx1010102 = 0x30335852;

            /**
             * 32-bit BGRx format, [31:0] B:G:R:x 10:10:10:2 little endian
             */
            public static final int bgrx1010102 = 0x30335842;

            /**
             * 32-bit ARGB format, [31:0] A:R:G:B 2:10:10:10 little endian
             */
            public static final int argb2101010 = 0x30335241;

            /**
             * 32-bit ABGR format, [31:0] A:B:G:R 2:10:10:10 little endian
             */
            public static final int abgr2101010 = 0x30334241;

            /**
             * 32-bit RGBA format, [31:0] R:G:B:A 10:10:10:2 little endian
             */
            public static final int rgba1010102 = 0x30334152;

            /**
             * 32-bit BGRA format, [31:0] B:G:R:A 10:10:10:2 little endian
             */
            public static final int bgra1010102 = 0x30334142;

            /**
             * packed YCbCr format, [31:0] Cr0:Y1:Cb0:Y0 8:8:8:8 little endian
             */
            public static final int yuyv = 0x56595559;

            /**
             * packed YCbCr format, [31:0] Cb0:Y1:Cr0:Y0 8:8:8:8 little endian
             */
            public static final int yvyu = 0x55595659;

            /**
             * packed YCbCr format, [31:0] Y1:Cr0:Y0:Cb0 8:8:8:8 little endian
             */
            public static final int uyvy = 0x59565955;

            /**
             * packed YCbCr format, [31:0] Y1:Cb0:Y0:Cr0 8:8:8:8 little endian
             */
            public static final int vyuy = 0x59555956;

            /**
             * packed AYCbCr format, [31:0] A:Y:Cb:Cr 8:8:8:8 little endian
             */
            public static final int ayuv = 0x56555941;

            /**
             * 2 plane YCbCr Cr:Cb format, 2x2 subsampled Cr:Cb plane
             */
            public static final int nv12 = 0x3231564E;

            /**
             * 2 plane YCbCr Cb:Cr format, 2x2 subsampled Cb:Cr plane
             */
            public static final int nv21 = 0x3132564E;

            /**
             * 2 plane YCbCr Cr:Cb format, 2x1 subsampled Cr:Cb plane
             */
            public static final int nv16 = 0x3631564E;

            /**
             * 2 plane YCbCr Cb:Cr format, 2x1 subsampled Cb:Cr plane
             */
            public static final int nv61 = 0x3136564E;

            /**
             * 3 plane YCbCr format, 4x4 subsampled Cb (1) and Cr (2) planes
             */
            public static final int yuv410 = 0x39565559;

            /**
             * 3 plane YCbCr format, 4x4 subsampled Cr (1) and Cb (2) planes
             */
            public static final int yvu410 = 0x39555659;

            /**
             * 3 plane YCbCr format, 4x1 subsampled Cb (1) and Cr (2) planes
             */
            public static final int yuv411 = 0x31315559;

            /**
             * 3 plane YCbCr format, 4x1 subsampled Cr (1) and Cb (2) planes
             */
            public static final int yvu411 = 0x31315659;

            /**
             * 3 plane YCbCr format, 2x2 subsampled Cb (1) and Cr (2) planes
             */
            public static final int yuv420 = 0x32315559;

            /**
             * 3 plane YCbCr format, 2x2 subsampled Cr (1) and Cb (2) planes
             */
            public static final int yvu420 = 0x32315659;

            /**
             * 3 plane YCbCr format, 2x1 subsampled Cb (1) and Cr (2) planes
             */
            public static final int yuv422 = 0x36315559;

            /**
             * 3 plane YCbCr format, 2x1 subsampled Cr (1) and Cb (2) planes
             */
            public static final int yvu422 = 0x36315659;

            /**
             * 3 plane YCbCr format, non-subsampled Cb (1) and Cr (2) planes
             */
            public static final int yuv444 = 0x34325559;

            /**
             * 3 plane YCbCr format, non-subsampled Cr (1) and Cb (2) planes
             */
            public static final int yvu444 = 0x34325659;
        }
    }
}
