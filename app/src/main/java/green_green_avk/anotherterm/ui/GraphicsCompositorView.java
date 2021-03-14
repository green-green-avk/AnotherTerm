package green_green_avk.anotherterm.ui;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.GraphicsCompositor;

public interface GraphicsCompositorView {
    void setCompositor(@NonNull GraphicsCompositor compositor);

    void unsetCompositor();

    @Nullable
    Bitmap makeThumbnail(int w, int h);
}
