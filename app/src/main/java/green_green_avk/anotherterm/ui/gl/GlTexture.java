package green_green_avk.anotherterm.ui.gl;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public final class GlTexture {
    private boolean _isInit = false;
    private boolean _isSet = false;
    private final int[] id = new int[1];

    public int magFilter = GLES20.GL_LINEAR;
    public int minFilter = GLES20.GL_LINEAR;
    public int wrapS = GLES20.GL_REPEAT;
    public int wrapT = GLES20.GL_REPEAT;
    public boolean makeMipmap = false;

    public void delete() {
        if (_isInit)
            GLES20.glDeleteTextures(1, id, 0);
        _isInit = false;
        _isSet = false;
    }

    private void init() {
        if (!_isInit) {
            GLES20.glGenTextures(1, id, 0);
            _isInit = true;
        }
    }

    public void bind(final int slot) {
        if (!_isInit) return;
        GLES20.glActiveTexture(slot);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
    }

    private void _set(@NonNull final ByteBuffer buffer, final int width, final int height,
                      final int format, final int type) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0,
                format, type, buffer);
        if (makeMipmap)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT);
    }

    private void _update(@NonNull final ByteBuffer buffer,
                         final int xOffset, final int yOffset, final int width, final int height,
                         final int format, final int type) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, xOffset, yOffset, width, height,
                format, type, buffer);
        if (makeMipmap)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    }

    private void _update(@NonNull final ByteBuffer buffer,
                         final int fromXOffset, final int fromYOffset, final int bufferWidth,
                         final int xOffset, final int yOffset, final int width, final int height,
                         final int format, final int type) {
        if (!GlInfo.hasUnpackRowLengthIn20 && GlInfo.getGlesMajorVersion() < 3) {
            _update(buffer, 0, 0, bufferWidth, fromYOffset + height,
                    format, type);
            return;
        }
        try {
            GLES20.glPixelStorei(BaseRenderer.GL_UNPACK_ROW_LENGTH_EXT, bufferWidth);
            GLES20.glPixelStorei(BaseRenderer.GL_UNPACK_SKIP_ROWS_EXT, fromYOffset);
            GLES20.glPixelStorei(BaseRenderer.GL_UNPACK_SKIP_PIXELS_EXT, fromXOffset);
            _update(buffer, xOffset, yOffset, width, height, format, type);
        } finally {
            GLES20.glPixelStorei(BaseRenderer.GL_UNPACK_ROW_LENGTH_EXT, 0);
            GLES20.glPixelStorei(BaseRenderer.GL_UNPACK_SKIP_ROWS_EXT, 0);
            GLES20.glPixelStorei(BaseRenderer.GL_UNPACK_SKIP_PIXELS_EXT, 0);
        }
    }

    private final int[] oldAlignment = new int[1];

    public void set(@NonNull final ByteBuffer buffer, final int width, final int height,
                    final int format, final int type, final int alignment) {
        init();
        if (alignment > 0) {
            GLES20.glGetIntegerv(GLES20.GL_UNPACK_ALIGNMENT, oldAlignment, 0);
            try {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, alignment);
                _set(buffer, width, height, format, type);
            } finally {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, oldAlignment[0]);
            }
        } else {
            _set(buffer, width, height, format, type);
        }
        _isSet = true;
    }

    public void update(@NonNull final ByteBuffer buffer,
                       final int fromXOffset, final int fromYOffset, final int bufferWidth,
                       final int xOffset, final int yOffset, final int width, final int height,
                       final int format, final int type, final int alignment) {
        if (!_isSet) return;
        if (alignment > 0) {
            GLES20.glGetIntegerv(GLES20.GL_UNPACK_ALIGNMENT, oldAlignment, 0);
            try {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, alignment);
                _update(buffer, fromXOffset, fromYOffset, bufferWidth,
                        xOffset, yOffset, width, height, format, type);
            } finally {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, oldAlignment[0]);
            }
        } else {
            _update(buffer, fromXOffset, fromYOffset, bufferWidth,
                    xOffset, yOffset, width, height, format, type);
        }
    }

    public boolean isSet() {
        return _isSet;
    }
}
