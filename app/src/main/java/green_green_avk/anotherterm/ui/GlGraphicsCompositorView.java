package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import green_green_avk.anotherterm.GraphicsCompositor;
import green_green_avk.anotherterm.ui.gl.BaseRenderer;
import green_green_avk.anotherterm.ui.gl.GlInfo;
import green_green_avk.anotherterm.ui.gl.GlProgram;
import green_green_avk.anotherterm.ui.gl.GlShader;
import green_green_avk.anotherterm.ui.gl.GlTexture;
import green_green_avk.anotherterm.ui.gl.GlUtils;
import green_green_avk.wayland.protocol.wayland.wl_shm;

public final class GlGraphicsCompositorView extends TextureView implements GraphicsCompositorView,
        GraphicsCompositor.Sink {
    private final Renderer renderer = new Renderer(this);

    public GlGraphicsCompositorView(final Context context) {
        super(context);
        setSurfaceTextureListener(renderer);
    }

    public GlGraphicsCompositorView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setSurfaceTextureListener(renderer);
    }

    public GlGraphicsCompositorView(final Context context, final AttributeSet attrs,
                                    final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(renderer);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GlGraphicsCompositorView(final Context context, final AttributeSet attrs,
                                    final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSurfaceTextureListener(renderer);
    }

    private GraphicsCompositor compositor = null;

    @Override
    public void setCompositor(@NonNull final GraphicsCompositor compositor) {
        this.compositor = compositor;
        compositor.setSink(this);
    }

    @Override
    public void unsetCompositor() {
        if (compositor == null)
            return;
        compositor.setSink(null);
        this.compositor = null;
    }

    private final PointF pointerPos = new PointF();

    private void pointerPosSet(final float x, final float y) {
        pointerPos.set(x, y);
        renderer.refresh();
    }

    private int buttons = 0;

    private int getActionButton(final int v) {
        if ((v & (MotionEvent.BUTTON_PRIMARY | MotionEvent.BUTTON_STYLUS_PRIMARY)) != 0)
            return MotionEvent.BUTTON_PRIMARY;
        if ((v & (MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_STYLUS_PRIMARY)) != 0)
            return MotionEvent.BUTTON_SECONDARY;
        if ((v & (MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_STYLUS_SECONDARY)) != 0)
            return MotionEvent.BUTTON_TERTIARY;
        return 0;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (compositor == null)
            return true;
        final GraphicsCompositor.Surface root = compositor.root;
        if (root == null)
            return true;
        if (!ScreenMouseView.isMouseEvent(event)) {
            final int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    final int pt = event.getActionIndex();
                    root.onTouchEvent(event.getEventTime(), event.getPointerId(pt),
                            event.getX(pt), event.getY(pt), action);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    final int pts = event.getPointerCount();
                    for (int pt = 0; pt < pts; pt++) {
                        root.onTouchEvent(event.getEventTime(), event.getPointerId(pt),
                                event.getX(pt), event.getY(pt), action);
                    }
                    break;
                }
            }
            return true;
        }
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int bb = event.getButtonState() |
                        ((event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS)
                                ? MotionEvent.BUTTON_PRIMARY : 0);
                pointerPosSet(event.getX(), event.getY());
                root.onPointerButtonEvent(event.getEventTime(), pointerPos.x, pointerPos.y,
                        getActionButton((bb ^ buttons) & bb), action);
                buttons = bb;
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                final int bb = event.getButtonState();
                pointerPosSet(event.getX(), event.getY());
                root.onPointerButtonEvent(event.getEventTime(), pointerPos.x, pointerPos.y,
                        getActionButton((bb ^ buttons) & buttons), action);
                buttons = bb;
                break;
            }
            case MotionEvent.ACTION_MOVE:
                pointerPosSet(event.getX(), event.getY());
                root.onPointerButtonEvent(event.getEventTime(), pointerPos.x, pointerPos.y,
                        0, action);
                break;
        }
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_MOUSE) == 0)
            return true;
        if (compositor == null)
            return true;
        final GraphicsCompositor.Surface root = compositor.root;
        if (root == null)
            return true;
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_HOVER_MOVE:
                pointerPosSet(event.getX(), event.getY());
                root.onPointerButtonEvent(event.getEventTime(), pointerPos.x, pointerPos.y,
                        0, action);
                break;
            case MotionEvent.ACTION_SCROLL: {
                final float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                pointerPosSet(event.getX(), event.getY());
                if (vScroll != 0)
                    root.onPointerAxisEvent(event.getEventTime(), pointerPos.x, pointerPos.y,
                            MotionEvent.AXIS_VSCROLL, vScroll);
                break;
            }
        }
        return true;
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (compositor == null)
            return;
//        if (BuildConfig.DEBUG)
//            Log.i("WlDraw", "* Resize: " + w + "x" + h);
        compositor.onResize(w, h);
        // Not in the listener's onSurfaceTextureSizeChanged() for a reason...
    }

    @Override
    public void invalidateSurface(@NonNull final GraphicsCompositor.Surface surface) {
        synchronized (surface) {
            final Renderer.SurfaceTreeRenderer.Cache texCache =
                    renderer.surfaceTreeRenderer.texCache.get(surface);
            if (texCache != null)
                texCache.dirty = true;
            renderer.refresh();
        }
    }

    @Override
    public void destroySurface(@NonNull final GraphicsCompositor.Surface surface) {
        synchronized (surface) {
            final Renderer.SurfaceTreeRenderer.Cache texCache =
                    renderer.surfaceTreeRenderer.texCache.get(surface);
            if (texCache != null) {
                texCache.texture.delete();
                renderer.surfaceTreeRenderer.texCache.remove(surface);
            }
        }
    }

    @Override
    @Nullable
    public Bitmap makeThumbnail(int w, int h) {
        if (getWidth() <= 0 || getHeight() <= 0)
            return null;
        final float s = Math.min((float) w / getWidth(), (float) h / getHeight());
        w = Math.max((int) (getWidth() * s), 1);
        h = Math.max((int) (getHeight() * s), 1);
        return getBitmap(w, h);
    }

    private static final class Renderer extends BaseRenderer {
        @NonNull
        private final GlGraphicsCompositorView view;

        public Renderer(@NonNull final GlGraphicsCompositorView view) {
            super(config0Depth, GlInfo.glesVersion < 0x30000 ? 2 : 3);
            this.view = view;
        }

        @Override
        protected void onRenderInit() {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glClearColor(0f, 0.5f, 0f, 1f);
            surfaceTreeRenderer.onInit();
        }

        @Override
        protected void onRenderFrame() {
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            surfaceTreeRenderer.onDraw();
        }

        @Override
        protected void onRenderExit() {
            surfaceTreeRenderer.onExit();
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surface,
                                                final int width, final int height) {
            super.onSurfaceTextureSizeChanged(surface, width, height);
            // The surface size in pixel seems based on the actual screen resolution,
            // not the virtual...
        }

        private final SurfaceTreeRenderer surfaceTreeRenderer = new SurfaceTreeRenderer(this);

        private static class SurfaceTreeRenderer {
            @NonNull
            private final Renderer renderer;

            private SurfaceTreeRenderer(@NonNull final Renderer renderer) {
                this.renderer = renderer;
            }

            private static final class Locations {
                private final int vertLoc;
                private final int texLoc;
                private final int scaleLoc;
                private final int offsetLoc;

                private Locations(@NonNull final GlProgram v) {
                    vertLoc = v.getAttribLocation("a_position");
                    texLoc = v.getUniformLocation("u_tex");
                    scaleLoc = v.getUniformLocation("u_scale");
                    offsetLoc = v.getUniformLocation("u_offset");
                }
            }

            private static final class Format {
                @NonNull
                private final GlProgram program;
                @NonNull
                private final Locations locations;

                private Format(@NonNull final GlProgram program) {
                    this.program = program;
                    this.locations = new Locations(program);
                }
            }

            private final Map<Integer, Format> formats = new HashMap<>();

            private static final String vertexShader =
                    "#version 100\n" +
                            "precision highp float;\n" +
                            "attribute vec2 a_position;\n" +
                            "varying vec2 v_position;\n" +
                            "uniform vec2 u_scale;\n" +
                            "uniform vec2 u_offset;\n" +
                            "void main() {\n" +
                            "   v_position = a_position.xy * vec2(0.5,-0.5) + 0.5;\n" +
                            "   gl_Position = vec4((a_position.xy + vec2(1.0, -1.0)) / u_scale + vec2(-1.0, 1.0) + u_offset, 0.0, 1.0);\n" +
                            "}\n";
            private static final String fragmentShaderTemplate =
                    "#version 100\n" +
                            "precision mediump float;\n" +
                            "varying vec2 v_position;\n" +
                            "uniform sampler2D u_tex;\n" +
                            "void main() {\n" +
                            "   vec4 c = texture2D(u_tex, v_position.xy);\n" +
                            "   gl_FragColor = vec4(%s);\n" +
                            "}\n";
            private static final String fragmentShader_argb8888 =
                    String.format(Locale.ROOT, fragmentShaderTemplate, "c.b, c.g, c.r, c.a");
            private static final String fragmentShader_xrgb8888 =
                    String.format(Locale.ROOT, fragmentShaderTemplate, "c.b, c.g, c.r, 1.0");
            private static final String fragmentShader_abgr8888 =
                    String.format(Locale.ROOT, fragmentShaderTemplate, "c");
            private static final String fragmentShader_xbgr8888 =
                    String.format(Locale.ROOT, fragmentShaderTemplate, "c.rgb, 1.0");
            private final FloatBuffer verts = GlUtils.makeDirectFloatBuffer(new float[]{
                    -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f
            });

            private static final class Cache {
                private GlTexture texture = null;
                private int width = 0;
                private int height = 0;
                private boolean dirty = false;
            }

            private final Map<GraphicsCompositor.Surface, Cache> texCache = new HashMap<>();

            private void onInit() {
                final GlShader vs = new GlShader(GLES20.GL_VERTEX_SHADER, vertexShader);
                formats.put(wl_shm.Enums.Format.argb8888, new Format(new GlProgram(vs,
                        new GlShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader_argb8888))));
                formats.put(wl_shm.Enums.Format.xrgb8888, new Format(new GlProgram(vs,
                        new GlShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader_xrgb8888))));
                formats.put(wl_shm.Enums.Format.abgr8888, new Format(new GlProgram(vs,
                        new GlShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader_abgr8888))));
                formats.put(wl_shm.Enums.Format.xbgr8888, new Format(new GlProgram(vs,
                        new GlShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader_xbgr8888))));
            }

            private void onExit() {
                for (final Map.Entry<GraphicsCompositor.Surface, Cache> entry : texCache.entrySet())
                    synchronized (entry.getKey()) {
                        entry.getValue().texture.delete();
                    }
                texCache.clear();
                formats.clear();
            }

            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            private void onDraw() {
                final GraphicsCompositor compositor = renderer.view.compositor;
                if (compositor == null)
                    return;
                final GraphicsCompositor.Surface surface = compositor.root;
                if (surface == null)
                    return;
                onDrawSurface(surface, 0, 0);
                final GraphicsCompositor.Surface pointer = compositor.pointer;
                if (pointer != null)
                    onDrawSurface(pointer, renderer.view.pointerPos.x, renderer.view.pointerPos.y);
            }

            private void onDrawSurface(@NonNull final GraphicsCompositor.Surface surface,
                                       final float x, final float y) {
                synchronized (surface) {
                    try {
                        if (surface.buffer == null)
                            return;
                        if (surface.stride != surface.width * 4)
                            throw new IllegalArgumentException("We can't this stride: " +
                                    surface.stride + " / " + surface.width);
//                        if (BuildConfig.DEBUG)
//                            Log.i("WlDraw", "* Frame: " +
//                                    surface.width + "x" + surface.height);
                        Cache texCache = this.texCache.get(surface);
                        if (texCache == null) {
                            texCache = new Cache();
                            texCache.texture = new GlTexture();
                            texCache.dirty = true;
                            this.texCache.put(surface, texCache);
                        }
                        final Locations ll;
                        final Format format;
                        format = formats.get(surface.format);
                        if (format == null)
                            throw new IllegalArgumentException(String.format(Locale.getDefault(),
                                    "Unknown format: 0x%08X", surface.format));
                        format.program.use();
                        ll = format.locations;
                        if (texCache.dirty) {
                            if (texCache.texture.isSet()
                                    && texCache.width == surface.width
                                    && texCache.height == surface.height) {
                                final Region damage = surface.damage;
                                final Rect damageRect = damage.getBounds();
                                if (damage.isEmpty()) {
                                    damageRect.right = surface.width;
                                    damageRect.bottom = surface.height;
                                }
                                texCache.texture.update(surface.buffer,
                                        damageRect.left, damageRect.top, surface.width,
                                        damageRect.left, damageRect.top,
                                        damageRect.right - damageRect.left,
                                        damageRect.bottom - damageRect.top,
                                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
                            } else {
                                texCache.texture.set(surface.buffer, surface.width, surface.height,
                                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
                                texCache.width = surface.width;
                                texCache.height = surface.height;
                            }
                            texCache.dirty = false;
                        }
                        texCache.texture.bind(GLES20.GL_TEXTURE0);
                        final Point hotspot = surface.hotspot;
                        final GraphicsCompositor compositor = surface.compositor;
                        synchronized (compositor.sizeLock) {
                            GLES20.glUniform2f(ll.offsetLoc,
                                    (x - hotspot.x) / compositor.width * 2,
                                    -(y - hotspot.y) / compositor.height * 2);
                            GLES20.glUniform2f(ll.scaleLoc,
                                    (float) compositor.width / surface.width,
                                    (float) compositor.height / surface.height);
                        }
                        GLES20.glUniform1i(ll.texLoc, 0);
                        GLES20.glVertexAttribPointer(ll.vertLoc, 2, GLES20.GL_FLOAT,
                                false, 0, verts);
                        GLES20.glEnableVertexAttribArray(ll.vertLoc);
                        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
                        GLES20.glDisableVertexAttribArray(ll.vertLoc);
                    } finally {
                        if (surface.pullNextFrameRequested())
                            mainHandler.postDelayed(surface::onNextFrame, 1000 / 24);
                    }
                }
            }
        }
    }
}
