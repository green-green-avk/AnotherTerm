package green_green_avk.anotherterm.ui.gl;

import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE;

import android.graphics.Point;
import android.opengl.GLES20;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import green_green_avk.anotherterm.BuildConfig;

/**
 * First access to this class fields (after the VM started)
 * should not be made from a GL context or it will be terminated.
 */
public final class GlInfo {
    private GlInfo() {
    }

    public static final int GL_MAJOR_VERSION = 0x821B;

    public static final String eglExtensions;
    public static final int glesVersion;
    public static final String gles20Extensions;
    public static final Point maxSize;
    public static final boolean hasUnpackRowLengthIn20;

    static {
        final EGL10 egl = (EGL10) EGLContext.getEGL();
        final EGLDisplay eglDisplay = egl.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        final int[] eglVer = new int[2];
        egl.eglInitialize(eglDisplay, eglVer);
        eglExtensions = egl.eglQueryString(eglDisplay, EGL10.EGL_EXTENSIONS);
        if (BuildConfig.DEBUG)
            Log.i("GL_CAPS", "EGL_EXTENSIONS: " + eglExtensions);
        final EGLConfig eglConfig =
                BaseRenderer.chooseEglConfig(egl, eglDisplay, BaseRenderer.config0Depth);
        // Check for GL ES 3.0
        final EGLContext eglContext30 = egl.eglCreateContext(eglDisplay, eglConfig,
                EGL_NO_CONTEXT,
                new int[]{BaseRenderer.EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE});
        if (eglContext30 == null) {
            glesVersion = 0x20000;
            if (BuildConfig.DEBUG)
                Log.i("GL_CAPS", "GL ES 2.0 supported");
        } else {
            glesVersion = 0x30000;
            if (BuildConfig.DEBUG)
                Log.i("GL_CAPS", "GL ES 3.0 supported");
            egl.eglDestroyContext(eglDisplay, eglContext30);
        }
        // ===
        final EGLContext eglContext = egl.eglCreateContext(eglDisplay, eglConfig,
                EGL_NO_CONTEXT,
                new int[]{BaseRenderer.EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE});
        if (eglContext == null)
            throw new RendererException("EGL error: " + egl.eglGetError());
        final EGLSurface eglSurface =
                egl.eglCreatePbufferSurface(eglDisplay, eglConfig, null);
        try {
            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext))
                throw new RendererException("EGL error: " + egl.eglGetError());
            gles20Extensions = GLES20.glGetString(GL10.GL_EXTENSIONS);
            if (BuildConfig.DEBUG)
                Log.i("GL_CAPS", "GL ES 2.0 GL_EXTENSIONS: " + gles20Extensions);
            final int[] tmp = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, tmp, 0);
            maxSize = new Point(tmp[0], tmp[0]);
            if (BuildConfig.DEBUG)
                Log.i("GL_CAPS", "GL ES 2.0 GL_MAX_TEXTURE_SIZE: " + maxSize.x);
            // It can be available in GL ES 2.0 even when it is not reported in GL_EXTENSIONS...
            GLES20.glGetIntegerv(BaseRenderer.GL_UNPACK_ROW_LENGTH_EXT, tmp, 0);
            hasUnpackRowLengthIn20 = GLES20.glGetError() == GLES20.GL_NO_ERROR;
            if (BuildConfig.DEBUG)
                Log.i("GL_CAPS", "GL ES 2.0 GL_UNPACK_ROW_LENGTH_EXT support: " +
                        hasUnpackRowLengthIn20);
            // ===
        } finally {
            egl.eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            egl.eglDestroyContext(eglDisplay, eglContext);
            egl.eglDestroySurface(eglDisplay, eglSurface);
        }
    }

    private static final ThreadLocal<int[]> _glesMajorVersion = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[1];
        }
    };

    public static int getGlesMajorVersion() {
        final int[] v = _glesMajorVersion.get();
        GLES20.glGetIntegerv(GL_MAJOR_VERSION, v, 0);
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR)
            v[0] = 2;
        return v[0];
    }
}
