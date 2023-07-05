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

import green_green_avk.wayland.protocol_core.WlInterface;

/**
 * child surface positioner
 * <p>
 * The {@code xdg_positioner} provides a collection of rules for the placement of a
 * child surface relative to a parent surface. Rules can be defined to ensure
 * the child surface remains within the visible area's borders, and to
 * specify how the child surface changes its position, such as sliding along
 * an axis, or flipping around a rectangle. These positioner-created rules are
 * constrained by the requirement that a child surface must intersect with or
 * be at least partially adjacent to its parent surface.
 * <p>
 * See the various requests for details about possible rules.
 * <p>
 * At the time of the request, the compositor makes a copy of the rules
 * specified by the {@code xdg_positioner}. Thus, after the request is complete the
 * {@code xdg_positioner} object can be destroyed or reused; further changes to the
 * object will have no effect on previous usages.
 * <p>
 * For an {@code xdg_positioner} object to be considered complete, it must have a
 * non-zero size set by {@code set_size}, and a non-zero anchor rectangle set by
 * {@code set_anchor_rect}. Passing an incomplete {@code xdg_positioner} object when
 * positioning a surface raises an {@code invalid_positioner} error.
 */
public class xdg_positioner extends WlInterface<xdg_positioner.Requests, xdg_positioner.Events> {
    public static final int version = 5;

    public interface Requests extends WlInterface.Requests {

        /**
         * destroy the {@code xdg_positioner} object
         * <p>
         * Notify the compositor that the {@code xdg_positioner} will no longer be used.
         */
        @IMethod(0)
        @IDtor
        void destroy();

        /**
         * set the size of the to-be positioned rectangle
         * <p>
         * Set the size of the surface that is to be positioned with the positioner
         * object. The size is in surface-local coordinates and corresponds to the
         * window geometry. See {@code xdg_surface.set_window_geometry}.
         * <p>
         * If a zero or negative size is set the {@code invalid_input} error is raised.
         *
         * @param width  width of positioned rectangle
         * @param height height of positioned rectangle
         */
        @IMethod(1)
        void set_size(int width, int height);

        /**
         * set the anchor rectangle within the parent surface
         * <p>
         * Specify the anchor rectangle within the parent surface that the child
         * surface will be placed relative to. The rectangle is relative to the
         * window geometry as defined by {@code xdg_surface.set_window_geometry} of the
         * parent surface.
         * <p>
         * When the {@code xdg_positioner} object is used to position a child surface, the
         * anchor rectangle may not extend outside the window geometry of the
         * positioned child's parent surface.
         * <p>
         * If a negative size is set the {@code invalid_input} error is raised.
         *
         * @param x      x position of anchor rectangle
         * @param y      y position of anchor rectangle
         * @param width  width of anchor rectangle
         * @param height height of anchor rectangle
         */
        @IMethod(2)
        void set_anchor_rect(int x, int y, int width, int height);

        /**
         * set anchor rectangle anchor
         * <p>
         * Defines the anchor point for the anchor rectangle. The specified anchor
         * is used derive an anchor point that the child surface will be
         * positioned relative to. If a corner anchor is set (e.g. '{@code top_left}' or
         * '{@code bottom_right}'), the anchor point will be at the specified corner;
         * otherwise, the derived anchor point will be centered on the specified
         * edge, or in the center of the anchor rectangle if no edge is specified.
         *
         * @param anchor anchor
         */
        @IMethod(3)
        void set_anchor(long anchor);

        /**
         * set child surface gravity
         * <p>
         * Defines in what direction a surface should be positioned, relative to
         * the anchor point of the parent surface. If a corner gravity is
         * specified (e.g. '{@code bottom_right}' or '{@code top_left}'), then the child surface
         * will be placed towards the specified gravity; otherwise, the child
         * surface will be centered over the anchor point on any axis that had no
         * gravity specified. If the gravity is not in the ‘gravity’ enum, an
         * {@code invalid_input} error is raised.
         *
         * @param gravity gravity direction
         */
        @IMethod(4)
        void set_gravity(long gravity);

        /**
         * set the adjustment to be done when constrained
         * <p>
         * Specify how the window should be positioned if the originally intended
         * position caused the surface to be constrained, meaning at least
         * partially outside positioning boundaries set by the compositor. The
         * adjustment is set by constructing a bitmask describing the adjustment to
         * be made when the surface is constrained on that axis.
         * <p>
         * If no bit for one axis is set, the compositor will assume that the child
         * surface should not change its position on that axis when constrained.
         * <p>
         * If more than one bit for one axis is set, the order of how adjustments
         * are applied is specified in the corresponding adjustment descriptions.
         * <p>
         * The default adjustment is none.
         *
         * @param constraint_adjustment bit mask of constraint adjustments
         */
        @IMethod(5)
        void set_constraint_adjustment(long constraint_adjustment);

        /**
         * set surface position offset
         * <p>
         * Specify the surface position offset relative to the position of the
         * anchor on the anchor rectangle and the anchor on the surface. For
         * example if the anchor of the anchor rectangle is at (x, y), the surface
         * has the gravity bottom|right, and the offset is (ox, oy), the calculated
         * surface position will be (x + ox, y + oy). The offset position of the
         * surface is the one used for constraint testing. See
         * {@code set_constraint_adjustment}.
         * <p>
         * An example use case is placing a popup menu on top of a user interface
         * element, while aligning the user interface element of the parent surface
         * with some user interface element placed somewhere in the popup surface.
         *
         * @param x surface position x offset
         * @param y surface position y offset
         */
        @IMethod(6)
        void set_offset(int x, int y);

        /**
         * continuously reconstrain the surface
         * <p>
         * When set reactive, the surface is reconstrained if the conditions used
         * for constraining changed, e.g. the parent window moved.
         * <p>
         * If the conditions changed and the popup was reconstrained, an
         * {@code xdg_popup.configure} event is sent with updated geometry, followed by an
         * {@code xdg_surface.configure} event.
         */
        @IMethod(7)
        @ISince(3)
        void set_reactive();

        /**
         * Set the parent window geometry the compositor should use when
         * positioning the popup. The compositor may use this information to
         * determine the future state the popup should be constrained using. If
         * this doesn't match the dimension of the parent the popup is eventually
         * positioned against, the behavior is undefined.
         * <p>
         * The arguments are given in the surface-local coordinate space.
         *
         * @param parent_width  future window geometry width of parent
         * @param parent_height future window geometry height of parent
         */
        @IMethod(8)
        @ISince(3)
        void set_parent_size(int parent_width, int parent_height);

        /**
         * set parent configure this is a response to
         * <p>
         * Set the serial of an {@code xdg_surface.configure} event this positioner will be
         * used in response to. The compositor may use this information together
         * with {@code set_parent_size} to determine what future state the popup should be
         * constrained using.
         *
         * @param serial serial of parent configure event
         */
        @IMethod(9)
        @ISince(3)
        void set_parent_configure(long serial);
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
             * invalid input provided
             */
            public static final int invalid_input = 0;
        }

        public static final class Anchor {
            private Anchor() {
            }

            public static final int none = 0;

            public static final int top = 1;

            public static final int bottom = 2;

            public static final int left = 3;

            public static final int right = 4;

            public static final int top_left = 5;

            public static final int bottom_left = 6;

            public static final int top_right = 7;

            public static final int bottom_right = 8;
        }

        public static final class Gravity {
            private Gravity() {
            }

            public static final int none = 0;

            public static final int top = 1;

            public static final int bottom = 2;

            public static final int left = 3;

            public static final int right = 4;

            public static final int top_left = 5;

            public static final int bottom_left = 6;

            public static final int top_right = 7;

            public static final int bottom_right = 8;
        }

        /**
         * constraint adjustments
         */
        public static final class Constraint_adjustment {
            private Constraint_adjustment() {
            }

            /**
             * don't move the child surface when constrained
             */
            public static final int none = 0;

            /**
             * move along the x axis until unconstrained
             */
            public static final int slide_x = 1;

            /**
             * move along the y axis until unconstrained
             */
            public static final int slide_y = 2;

            /**
             * invert the anchor and gravity on the x axis
             */
            public static final int flip_x = 4;

            /**
             * invert the anchor and gravity on the y axis
             */
            public static final int flip_y = 8;

            /**
             * horizontally resize the surface
             */
            public static final int resize_x = 16;

            /**
             * vertically resize the surface
             */
            public static final int resize_y = 32;
        }
    }
}
