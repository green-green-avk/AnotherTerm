package green_green_avk.anotherterm.ui.gl;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

public final class GlShader {
    private int id = 0;

    int getId() {
        return id;
    }

    public GlShader(final int type, @NonNull final String src) {
        set(type, src);
    }

    public void set(final int type, @NonNull final String src) {
        if (id == 0)
            id = GLES20.glCreateShader(type);
        if (id == 0)
            throw new RendererException("Error allocating shader of type " + type);
        GLES20.glShaderSource(id, src);
        GLES20.glCompileShader(id);
        final int[] r = new int[]{0};
        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, r, 0);
        if (r[0] == 0) {
            final String msg = GLES20.glGetShaderInfoLog(id);
            GLES20.glDeleteShader(id);
            id = 0;
            throw new RendererException("Shader compile error:\n" + msg);
        }
    }

    public void delete() {
        if (id != 0)
            GLES20.glDeleteShader(id);
        id = 0;
    }
}
