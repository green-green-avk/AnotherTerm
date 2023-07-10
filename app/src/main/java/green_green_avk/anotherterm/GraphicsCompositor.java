package green_green_avk.anotherterm;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import green_green_avk.wayland.protocol.wayland.wl_shm;

public final class GraphicsCompositor {
    private static final KeyCharacterMap keyCharacterMap =
            KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    public interface SurfaceSource {
        void onEnter();

        void onLeave();

        void onPointerButtonEvent(long time, float x, float y, int buttonId, int action);

        void onPointerAxisEvent(long time, float x, float y, int axisId, float value);

        void onKeyEvent(long time, int keyCode, boolean pressed);

        void onKeyEvent(boolean enter);

        void onTouchEvent(long time, int ptId, float x, float y, int action);
    }

    public interface Source {
        @UiThread
        void onStop();

        void onResize(int width, int height);
    }

    public interface Sink {
        void invalidateSurface(@NonNull Surface surface);

        void destroySurface(@NonNull Surface surface);
    }

    public interface ClipboardContentCb {
        void clipboardContent(@NonNull String mime, @NonNull byte[] data);
    }

    public interface AuxSource {
        void clipboardContent(@NonNull String mime, @NonNull byte[] data);

        void clipboardContentRequest(@NonNull String mime, @NonNull ClipboardContentCb cb);

        void imPutString(@NonNull String str);
    }

    public Source source = null;
    private volatile Sink sink = null;

    private AuxSource auxSource = null;

    public void setSink(@Nullable final Sink sink) {
        this.sink = sink;
        synchronized (treeLock) {
            final Surface root = this.root;
            if (root != null) {
                if (sink != null)
                    root.source.onEnter();
                else
                    root.source.onLeave();
            }
        }
    }

    public final int defaultWidth;
    public final int defaultHeight;

    {
        final DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        final int _size = Math.max(dm.widthPixels, dm.heightPixels);
        final int size;
        switch (Integer.bitCount(_size)) {
            case 1:
                size = _size;
                break;
            case 0:
                throw new Error("Ouch!");
            default:
                size = Integer.highestOneBit(_size) << 1;
        }
        defaultWidth = size;
        defaultHeight = size;
    }

    public int width = defaultWidth;
    public int height = defaultHeight;
    public final Object sizeLock = new Object();

    @Nullable
    public volatile Surface keyboardFocus = null;

    @NonNull
    public volatile CharSequence title = "\uD83D\uDDA5";

    // TODO: The Wayland / X screen keyboard must be a pretty different beast. Split!
    public final GraphicsConsoleOutput consoleOutput = new GraphicsConsoleOutput() {
        @Override
        public boolean getKeyAutorepeat() {
            return true; // Text input mode only
        }

        @Override
        @AnyRes
        public int getLayoutRes() {
            return R.array.graphics_keyboard;
        }

        @Override
        public void feed(final int code, final boolean shift,
                         final boolean alt, final boolean ctrl) {
            if (code == 0)
                return;
            if (ctrl)
                feed(KeyEvent.KEYCODE_CTRL_LEFT, true);
            if (alt)
                feed(KeyEvent.KEYCODE_ALT_LEFT, true);
            if (code < 0)
                feed(String.valueOf((char) -code));
            else {
                if (shift)
                    feed(KeyEvent.KEYCODE_SHIFT_LEFT, true);
                feed(code, true);
                feed(code, false);
                if (shift)
                    feed(KeyEvent.KEYCODE_SHIFT_LEFT, false);
            }
            if (alt)
                feed(KeyEvent.KEYCODE_ALT_LEFT, false);
            if (ctrl)
                feed(KeyEvent.KEYCODE_CTRL_LEFT, false);
        }

        @Override
        public void feed(final int code, final boolean pressed) {
            if (code < 0)
                return;
            final Surface s = keyboardFocus;
            if (s == null)
                return;
            final SurfaceSource ss = s.source;
            if (ss == null)
                return;
            ss.onKeyEvent(SystemClock.uptimeMillis(), code, pressed);
        }

        @Override
        public void feed(@NonNull final String v) {
            if (hasImSupport()) {
                putImString(v);
                return;
            }
            final Surface s = keyboardFocus;
            if (s == null)
                return;
            final SurfaceSource ss = s.source;
            if (ss == null)
                return;
            final KeyEvent[] events = keyCharacterMap.getEvents(v.toCharArray());
            if (events == null)
                return;
            for (final KeyEvent event : events) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ss.onKeyEvent(event.getEventTime(),
                                event.getKeyCode(), true);
                        break;
                    case MotionEvent.ACTION_UP:
                        ss.onKeyEvent(event.getEventTime(),
                                event.getKeyCode(), false);
                        break;
                }
            }
        }

        @Override
        public void paste(@NonNull final String v) {
            feed(v);
        }
    };

    public static final class Surface {
        @NonNull
        public final GraphicsCompositor compositor;
        public volatile SurfaceSource source;
        @Nullable
        public GraphicsCompositor.Surface parent = null;
        public GraphicsCompositor.Surface prevSibling = null;
        public GraphicsCompositor.Surface nextSibling = null;
        public GraphicsCompositor.Surface firstChild = null;

        public ByteBuffer buffer = null;
        public int offset = 0;
        public int width = 0;
        public int height = 0;
        public int stride = 0;
        public int format = wl_shm.Enums.Format.argb8888;
        @NonNull
        public final Matrix transform = new Matrix();
        @NonNull
        public final Rect damage = new Rect();

        private final AtomicReference<Runnable> _onReleaseBuffer = new AtomicReference<>();
        private final AtomicReference<Runnable> _onNextFrame = new AtomicReference<>();
        private final AtomicBoolean _isNextFrameRequested = new AtomicBoolean(false);

        @NonNull
        private final Point pendingHotspot = new Point();

        public void setHotspot(final int x, final int y) {
            synchronized (this) {
                pendingHotspot.set(x, y);
            }
        }

        public void clean() {
            buffer = null;
            onReleaseBuffer();
            _onNextFrame.set(null);
            final Sink sink = compositor.sink;
            if (sink != null)
                sink.destroySurface(this);
        }

        /**
         * Commits a new buffer.
         *
         * @param buffer
         * @param offset
         * @param width
         * @param height
         * @param stride
         * @param format          {@link wl_shm.Enums.Format} is used at the moment.
         * @param transform
         * @param damage
         * @param onBufferRelease
         * @param onNextFrame
         */
        public void commit(@NonNull final ByteBuffer buffer, final int offset,
                           final int width, final int height, final int stride,
                           final int format, @NonNull final Matrix transform,
                           @NonNull final Region damage,
                           @NonNull final Runnable onBufferRelease,
                           @Nullable final Runnable onNextFrame) {
            final Runnable __onReleaseBuffer;
            final Runnable __onNextFrame;
            synchronized (this) {
                this.buffer = buffer;
                this.offset = offset;
                this.width = width;
                this.height = height;
                this.stride = stride;
                this.format = format;
                this.transform.set(transform);
                this.transform.postTranslate(-pendingHotspot.x, -pendingHotspot.y);
                this.damage.set(damage.getBounds());
                _isNextFrameRequested.set(true);
                __onReleaseBuffer = _onReleaseBuffer.getAndSet(onBufferRelease);
                __onNextFrame = _onNextFrame.getAndSet(onNextFrame);
            }
            if (__onReleaseBuffer != null)
                __onReleaseBuffer.run();
            if (__onNextFrame != null)
                __onNextFrame.run();
            final Sink sink = compositor.sink;
            if (sink != null)
                sink.invalidateSurface(this);
        }

        /**
         * Commits an empty buffer.
         *
         * @param onNextFrame
         */
        public void commit(@Nullable final Runnable onNextFrame) {
            final Runnable __onReleaseBuffer;
            final Runnable __onNextFrame;
            synchronized (this) {
                this.buffer = null;
                this.offset = 0;
                this.width = 0;
                this.height = 0;
                this.stride = 0;
                this.format = wl_shm.Enums.Format.argb8888;
                this.transform.reset();
                this.damage.setEmpty();
                _isNextFrameRequested.set(true);
                __onReleaseBuffer = _onReleaseBuffer.getAndSet(null);
                __onNextFrame = _onNextFrame.getAndSet(onNextFrame);
            }
            if (__onReleaseBuffer != null)
                __onReleaseBuffer.run();
            if (__onNextFrame != null)
                __onNextFrame.run();
            final Sink sink = compositor.sink;
            if (sink != null) {
                sink.destroySurface(this);
                sink.invalidateSurface(this);
            }
        }

        public void onReleaseBuffer() {
            final Runnable r;
            synchronized (this) {
                r = _onReleaseBuffer.getAndSet(null);
            }
            if (r != null)
                r.run();
        }

        public void onNextFrame() {
            final Runnable r;
            synchronized (this) {
                r = _onNextFrame.getAndSet(null);
            }
            if (r != null)
                r.run();
        }

        public boolean pullNextFrameRequested() {
            synchronized (this) {
                return _isNextFrameRequested.getAndSet(false);
            }
        }

        public void onPointerButtonEvent(final long time, final float x, final float y,
                                         final int buttonId, final int action) {
            final SurfaceSource ss = source;
            if (ss != null)
                ss.onPointerButtonEvent(time, x, y, buttonId, action);
        }

        public void onPointerAxisEvent(final long time, final float x, final float y,
                                       final int axisId, final float value) {
            final SurfaceSource ss = source;
            if (ss != null)
                ss.onPointerAxisEvent(time, x, y, axisId, value);
        }

        public void onTouchEvent(final long time, final int ptId,
                                 final float x, final float y,
                                 final int action) {
            final SurfaceSource ss = source;
            if (ss != null)
                ss.onTouchEvent(time, ptId, x, y, action);
        }

        private Surface(@NonNull final GraphicsCompositor compositor) {
            this.compositor = compositor;
        }

        @Override
        protected void finalize() throws Throwable {
            clean();
            super.finalize();
        }
    }

    @NonNull
    public Surface createSurface() {
        return new Surface(this);
    }

    @Nullable
    public volatile Surface pointer = null;

    @Nullable
    public volatile Surface root = null;
    public final Object treeLock = new Object();

    public void setRoot(@Nullable final Surface root) {
        synchronized (treeLock) {
            final Surface oldRoot = this.root;
            final boolean refocus = keyboardFocus == null || keyboardFocus == oldRoot;
            if (oldRoot != null)
                oldRoot.source.onLeave();
            this.root = root;
            if (root != null)
                root.source.onEnter();
            if (refocus)
                onKeyboardFocusChange(root);
        }
    }

    public void onResize(final int width, final int height) {
        final boolean r;
        synchronized (sizeLock) {
            r = this.width != width || this.height != height;
            this.width = width;
            this.height = height;
        }
        if (r)
            source.onResize(width, height);
    }

    public void onKeyboardFocusChange(@Nullable final Surface focus) {
        synchronized (treeLock) {
            final Surface oldFocus = keyboardFocus;
            if (oldFocus == focus)
                return;
            if (oldFocus != null)
                oldFocus.source.onKeyEvent(false);
            keyboardFocus = focus;
            if (focus != null)
                focus.source.onKeyEvent(true);
        }
    }

    public interface OnClipboardSupportState {
        void onClipboardSupportState(boolean v);
    }

    private OnClipboardSupportState onClipboardSupportState = null;

    public void setAuxSource(@Nullable final AuxSource v) {
        auxSource = v;
        if (onClipboardSupportState != null)
            onClipboardSupportState.onClipboardSupportState(auxSource != null);
    }

    public boolean hasClipboardSupport() {
        return auxSource != null;
    }

    public boolean hasImSupport() {
        return auxSource != null;
    }

    public void setOnClipboardSupportState(@Nullable final OnClipboardSupportState v) {
        onClipboardSupportState = v;
        if (v != null)
            v.onClipboardSupportState(hasClipboardSupport());
    }

    public void postClipboardContent(@NonNull final String mime, @NonNull final byte[] data) {
        if (auxSource != null)
            auxSource.clipboardContent(mime, data);
    }

    public void requestClipboardContent(@NonNull final String mime,
                                        @NonNull final ClipboardContentCb cb) {
        if (auxSource != null)
            auxSource.clipboardContentRequest(mime, cb);
    }

    public void putImString(@NonNull final String str) {
        if (auxSource != null)
            auxSource.imPutString(str);
    }

    @UiThread
    public void stop() {
        source.onStop();
    }
}
