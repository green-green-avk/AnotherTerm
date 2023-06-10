package green_green_avk.anotherterm.ui.gl;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

public final class GlProgram {
    private int id = 0;

    public GlProgram() {
    }

    public GlProgram(@NonNull final GlShader vertexShader,
                     @NonNull final GlShader fragmentShader) {
        attach(vertexShader);
        attach(fragmentShader);
        link();
    }

    public void attach(@NonNull final GlShader shader) {
        if (id == 0)
            id = GLES20.glCreateProgram();
        if (id == 0)
            throw new RendererException("Error allocating program");
        GLES20.glAttachShader(id, shader.getId());
    }

    public void link() {
        if (id == 0)
            throw new RendererException("Error linking empty program");
        GLES20.glLinkProgram(id);
        final int[] r = new int[]{0};
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, r, 0);
        if (r[0] == 0) {
            final String msg = GLES20.glGetProgramInfoLog(id);
            GLES20.glDeleteProgram(id);
            id = 0;
            throw new RendererException("Program linking error:\n" + msg);
        }
    }

    public void use() {
        GLES20.glUseProgram(id);
    }

    public int getAttribLocation(@NonNull final String name) {
        return GLES20.glGetAttribLocation(id, name);
    }

    public int getUniformLocation(@NonNull final String name) {
        return GLES20.glGetUniformLocation(id, name);
    }

    public void delete() {
        if (id != 0)
            GLES20.glDeleteProgram(id);
        id = 0;
    }
}
