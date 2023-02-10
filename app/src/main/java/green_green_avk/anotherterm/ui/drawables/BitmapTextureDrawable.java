package green_green_avk.anotherterm.ui.drawables;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.ui.UiDimens;

public class BitmapTextureDrawable extends Drawable {
    @NonNull
    private final Bitmap bitmap;
    private final Paint paint = new Paint();
    @NonNull
    private Shader.TileMode tileModeX = Shader.TileMode.CLAMP;
    @NonNull
    private Shader.TileMode tileModeY = Shader.TileMode.CLAMP;
    public final UiDimens.Length tileSizeX =
            new UiDimens.Length(1f, UiDimens.Length.Units.PARENT_WIDTH,
                    this::reInvalidate);
    public final UiDimens.Length tileSizeY =
            new UiDimens.Length(1f, UiDimens.Length.Units.PARENT_HEIGHT,
                    this::reInvalidate);
    public final UiDimens.Length tileOffsetX =
            new UiDimens.Length(0f, UiDimens.Length.Units.PARENT_WIDTH,
                    this::reInvalidate);
    public final UiDimens.Length tileOffsetY =
            new UiDimens.Length(0f, UiDimens.Length.Units.PARENT_HEIGHT,
                    this::reInvalidate);

    private float density = 1f;
    private float scaledDensity = density;

    private void reInvalidate() {
        isDirty = true;
        invalidateSelf();
    }

    public BitmapTextureDrawable(@NonNull final Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setMetrics(@NonNull final DisplayMetrics displayMetrics) {
        density = displayMetrics.density;
        scaledDensity = displayMetrics.scaledDensity;
        reInvalidate();
    }

    @NonNull
    public Bitmap getBitmap() {
        return bitmap;
    }

    @NonNull
    public Shader.TileMode getTileModeX() {
        return tileModeX;
    }

    @NonNull
    public Shader.TileMode getTileModeY() {
        return tileModeY;
    }

    public void setTileModeXY(@NonNull final Shader.TileMode tileModeX,
                              @NonNull final Shader.TileMode tileModeY) {
        this.tileModeX = tileModeX;
        this.tileModeY = tileModeY;
        reInvalidate();
    }

    @Nullable
    public Xfermode getXfermode() {
        return paint.getXfermode();
    }

    public void setXfermode(@Nullable final Xfermode v) {
        paint.setXfermode(v);
    }

    public boolean isFilterBitmap() {
        return paint.isFilterBitmap();
    }

    public void setFilterBitmap(final boolean v) {
        paint.setFilterBitmap(v);
    }

    private boolean isDirty = true;

    private void update() {
        if (!isDirty)
            return;
        isDirty = false;
        final BitmapShader shader = new BitmapShader(bitmap, tileModeX, tileModeY);
        final Matrix matrix = new Matrix();
        final Rect bounds = getBounds();
        matrix.setScale(
                tileSizeX.measure(bounds.width(), bounds.height(),
                        density, scaledDensity) / bitmap.getWidth(),
                tileSizeY.measure(bounds.width(), bounds.height(),
                        density, scaledDensity) / bitmap.getHeight()
        );
        matrix.postTranslate(
                tileOffsetX.measure(bounds.width(), bounds.height(),
                        density, scaledDensity) + bounds.left,
                tileOffsetY.measure(bounds.width(), bounds.height(),
                        density, scaledDensity) + bounds.top
        );
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);
    }

    private final Rect _bitmapRect = new Rect();

    @Override
    public void draw(@NonNull final Canvas canvas) {
        update();
        if (paint.getShader() != null) {
            canvas.drawRect(getBounds(), paint);
        } else {
            _bitmapRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(bitmap, _bitmapRect, getBounds(), paint);
        }
    }

    @Override
    public void setAlpha(final int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable final ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return bitmap.hasAlpha() ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }
}
