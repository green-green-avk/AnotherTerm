package green_green_avk.anotherterm.ui.gl;

import static javax.microedition.khronos.egl.EGL10.EGL_ALPHA_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_BLUE_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_DEPTH_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_GREEN_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE;
import static javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_RENDERABLE_TYPE;
import static javax.microedition.khronos.egl.EGL10.EGL_STENCIL_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_SUCCESS;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public abstract class BaseRenderer implements TextureView.SurfaceTextureListener {
    public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    public static final int EGL_OPENGL_ES2_BIT = 0x0004;
    public static final int GL_UNPACK_ROW_LENGTH_EXT = 0x0CF2;
    public static final int GL_UNPACK_SKIP_ROWS_EXT = 0x0CF3;
    public static final int GL_UNPACK_SKIP_PIXELS_EXT = 0x0CF4;

    public static final int[] config0Depth = new int[]{
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 0,
            EGL_STENCIL_SIZE, 0,
            EGL_NONE
    };

    public static final int[] config24Depth = new int[]{
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 24,
            EGL_STENCIL_SIZE, 0,
            EGL_NONE
    };

    public final int[] config;
    public final int glesVersion;

    @NonNull
    public static EGLConfig chooseEglConfig(@NonNull final EGL10 egl,
                                            @NonNull final EGLDisplay eglDisplay,
                                            @NonNull final int[] config) {
        final int[] configsCount = new int[]{0};
        final EGLConfig[] configs = new EGLConfig[]{null};
        egl.eglChooseConfig(eglDisplay, config, configs, 1, configsCount);
        if (configs[0] == null)
            throw new RendererException("No good GL config found");
        return configs[0];
    }

    public BaseRenderer(@NonNull final int[] config, final int glesVersion) {
        this.config = config;
        this.glesVersion = glesVersion;
    }

    protected abstract void onRenderInit();

    protected abstract void onRenderFrame();

    protected abstract void onRenderExit();

    public void refresh() {
        synchronized (refreshLock) {
            refreshMark = true;
            refreshLock.notifyAll();
        }
    }

    private volatile boolean refreshMark = false;
    private final Object refreshLock = new Object();
    protected final Object renderLock = new Object();

    private final AtomicReference<Point> resize = new AtomicReference<>(null);

    private final class RenderThread extends Thread {
        private volatile boolean isStopped = false;
        @NonNull
        private final SurfaceTexture surface;

        public RenderThread(@NonNull final SurfaceTexture surface) {
            super();
            this.surface = surface;
            setDaemon(true);
        }

        @Override
        public void run() {
            final EGL10 egl = (EGL10) EGLContext.getEGL();
            final EGLDisplay eglDisplay = egl.eglGetDisplay(EGL_DEFAULT_DISPLAY);
            egl.eglInitialize(eglDisplay, new int[2]);
            final EGLConfig eglConfig = chooseEglConfig(egl, eglDisplay, config);
            final EGLContext eglContext = egl.eglCreateContext(eglDisplay, eglConfig,
                    EGL_NO_CONTEXT,
                    new int[]{EGL_CONTEXT_CLIENT_VERSION, glesVersion, EGL_NONE});
            final EGLSurface eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig,
                    surface,
                    null);
            try {
                if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext))
                    throw new RendererException("GLError: " + egl.eglGetError());
                synchronized (renderLock) {
                    onRenderInit();
                }
                int err = egl.eglGetError();
                if (err != EGL_SUCCESS)
                    throw new RendererException("GLError: " + err);
                while (!isStopped) {
                    final Point resize = BaseRenderer.this.resize.getAndSet(null);
                    if (resize != null)
                        GLES20.glViewport(0, 0, resize.x, resize.y);
                    synchronized (renderLock) {
                        onRenderFrame();
                    }
                    err = egl.eglGetError();
                    if (err != EGL_SUCCESS)
                        throw new RendererException("GLError: " + err);
                    GLES20.glFlush();
                    GLES20.glFinish();
                    if (!egl.eglSwapBuffers(eglDisplay, eglSurface))
                        throw new RendererException("GLError: " + egl.eglGetError());
                    try {
                        synchronized (refreshLock) {
                            if (!refreshMark)
                                refreshLock.wait();
                            refreshMark = false;
                        }
                    } catch (final InterruptedException e) {
                        throw new RendererException("GLError: interrupted");
                    }
                }
            } finally {
                synchronized (renderLock) {
                    onRenderExit();
                }
                surface.release();
                egl.eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                egl.eglDestroyContext(eglDisplay, eglContext);
                egl.eglDestroySurface(eglDisplay, eglSurface);
            }
        }
    }

    private RenderThread renderThread = null;

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface,
                                          final int width, final int height) {
        renderThread = new RenderThread(surface);
        resize.set(new Point(width, height));
        renderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface,
                                            final int width, final int height) {
        resize.set(new Point(width, height));
        refresh();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        if (renderThread != null)
            renderThread.isStopped = true;
        refresh();
        renderThread = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
    }
}
