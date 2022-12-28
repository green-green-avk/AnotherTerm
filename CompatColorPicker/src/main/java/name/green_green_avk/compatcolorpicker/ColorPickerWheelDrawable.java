package name.green_green_avk.compatcolorpicker;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class ColorPickerWheelDrawable extends Drawable {
    private final SweepGradient hg = new SweepGradient(100f, 100f,
            new int[]{Color.RED, Color.MAGENTA, Color.BLUE,
                    Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED},
            null);
    private final RadialGradient sg = new RadialGradient(100f, 100f, 100f,
            Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP);
    private final Paint hgp = new Paint();
    private final Paint sgp = new Paint();

    {
        hgp.setStyle(Paint.Style.FILL);
        sgp.setStyle(Paint.Style.FILL);
        hgp.setShader(hg);
        sgp.setShader(sg);
    }

    private int width = -1;
    private int height = -1;

    public ColorPickerWheelDrawable() {
    }

    public ColorPickerWheelDrawable(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    public void setIntrinsicWidth(final int v) {
        width = v;
    }

    public void setIntrinsicHeight(final int v) {
        height = v;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        final int sl = canvas.save();
        try {
            canvas.scale((float) getBounds().width() / 200,
                    (float) getBounds().height() / 200);
            canvas.drawCircle(100f, 100f, 100f, hgp);
            canvas.drawCircle(100f, 100f, 100f, sgp);
        } finally {
            canvas.restoreToCount(sl);
        }
    }

    @Override
    public void setAlpha(final int i) {
    }

    @Override
    public void setColorFilter(@Nullable final ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
