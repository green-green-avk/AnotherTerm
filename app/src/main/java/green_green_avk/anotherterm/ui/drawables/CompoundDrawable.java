package green_green_avk.anotherterm.ui.drawables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.NinePatch;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import green_green_avk.anotherterm.ui.UiDimens;
import green_green_avk.anotherterm.utils.LimitedInputStream;

public final class CompoundDrawable {
    private CompoundDrawable() {
    }

    private abstract static class ProductDrawable extends Drawable {
        @Nullable
        private static <T extends Drawable> T getWrapped(@NonNull Drawable drawable) {
            while (drawable instanceof DrawableWrapperCompat) {
                drawable = ((DrawableWrapperCompat) drawable).getDrawable();
            }
            return (T) drawable;
        }

        /**
         * Legacy. No actual multiplication layer separation.
         */
        private static final class LegacyProductDrawable extends ProductDrawable {
            @NonNull
            private final List<? extends Drawable> drawables;

            private LegacyProductDrawable(@NonNull final Collection<? extends Drawable> drawables) {
                this.drawables = new LinkedList<>(drawables);
                for (final Drawable drawable : this.drawables.subList(1, this.drawables.size())) {
                    final Drawable wrapped = getWrapped(drawable);
                    if (wrapped instanceof BitmapDrawable) {
                        ((BitmapDrawable) wrapped).getPaint()
                                .setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
                    } else if (wrapped instanceof NinePatchDrawable) {
                        ((NinePatchDrawable) wrapped).getPaint()
                                .setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
                    }
                }
            }

            @Override
            protected void onBoundsChange(final Rect bounds) {
                super.onBoundsChange(bounds);
                for (final Drawable drawable : drawables) {
                    drawable.setBounds(bounds);
                }
            }

            @Override
            public void draw(@NonNull final Canvas canvas) {
                for (final Drawable drawable : drawables) {
                    drawable.draw(canvas);
                }
            }
        }

        /**
         * How it ought to be.
         */
        @RequiresApi(api = Build.VERSION_CODES.Q)
        private static final class RenderNodeProductDrawable extends ProductDrawable {
            @NonNull
            private final List<? extends Drawable> drawables;
            private final RenderNode node = new RenderNode(this.getClass().getName());

            private RenderNodeProductDrawable(@NonNull final Collection<? extends Drawable> drawables) {
                this.drawables = new LinkedList<>(drawables);
                for (final Drawable drawable : this.drawables.subList(1, this.drawables.size())) {
                    final Drawable wrapped = getWrapped(drawable);
                    if (wrapped instanceof BitmapDrawable) {
                        ((BitmapDrawable) wrapped).getPaint().setBlendMode(BlendMode.MULTIPLY);
                    } else if (wrapped instanceof NinePatchDrawable) {
                        ((NinePatchDrawable) wrapped).getPaint().setBlendMode(BlendMode.MULTIPLY);
                    }
                }
                if (this.drawables.size() > 1) {
                    node.setHasOverlappingRendering(true);
                }
            }

            @Override
            protected void onBoundsChange(final Rect bounds) {
                node.discardDisplayList();
                super.onBoundsChange(bounds);
                for (final Drawable drawable : drawables) {
                    drawable.setBounds(bounds);
                }
            }

            @Override
            public void draw(@NonNull final Canvas canvas) {
                if (canvas.isHardwareAccelerated()) {
                    if (!node.hasDisplayList()) {
                        node.setPosition(getBounds());
                        for (final Drawable drawable : drawables) {
                            final Canvas nodeCanvas = node.beginRecording();
                            try {
                                drawable.draw(nodeCanvas);
                            } finally {
                                node.endRecording();
                            }
                        }
                    }
                    canvas.drawRenderNode(node);
                } else { // Fallback
                    for (final Drawable drawable : drawables) {
                        drawable.draw(canvas);
                    }
                }
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
            return PixelFormat.TRANSLUCENT;
        }

        @NonNull
        public static ProductDrawable create(@NonNull final Collection<? extends Drawable> drawables) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    new RenderNodeProductDrawable(drawables) :
                    new LegacyProductDrawable(drawables);
        }
    }

    public static class ParseException extends RuntimeException {
        public ParseException(@NonNull final String message) {
            super(message);
        }

        public ParseException(@NonNull final String message, @NonNull final Throwable cause) {
            super(message, cause);
        }

        public ParseException(@NonNull final Throwable cause) {
            super(cause);
        }
    }

    private static final String MSG_BAD_IMAGE = "Error reading image data";

    private static final class ExtNinePatchDrawable extends NinePatchDrawable {
        @NonNull
        private final Bitmap bitmap;

        public ExtNinePatchDrawable(@NonNull final Bitmap bitmap, final byte[] chunk,
                                    final Rect padding, final String srcName) {
            super(bitmap, chunk, padding, srcName);
            this.bitmap = bitmap;
        }

        @NonNull
        public Bitmap getBitmap() {
            return bitmap;
        }
    }

    @NonNull
    private static Drawable loadNinePatch(@NonNull final InputStream source) {
        final Rect padding = new Rect();
        final Bitmap bitmap = BitmapFactory.decodeStream(source, padding, null);
        if (bitmap == null)
            throw new ParseException(MSG_BAD_IMAGE);
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        final byte[] np = bitmap.getNinePatchChunk();
        if (np != null && NinePatch.isNinePatchChunk(np))
            return new ExtNinePatchDrawable(bitmap, np, padding, null);
        return new BitmapDrawable(bitmap);
    }

    @NonNull
    private static BitmapDrawable loadTexture(@NonNull final InputStream source) {
        final Bitmap bitmap = BitmapFactory.decodeStream(source, null, null);
        if (bitmap == null)
            throw new ParseException(MSG_BAD_IMAGE);
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        final BitmapDrawable r = new BitmapDrawable(bitmap);
        r.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        return r;
    }

    private static void setDensity(@NonNull final Drawable drawable, final int density) {
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).getBitmap().setDensity(density);
        } else if (drawable instanceof ExtNinePatchDrawable) {
            ((ExtNinePatchDrawable) drawable).getBitmap().setDensity(density);
        }
    }

    private static final int F_FILTER_BITMAP = 1;

    private static boolean is(final int v, final int what) {
        return (v & what) == what;
    }

    private static final String MSG_BAD_UNITS = "Malformed units";

    @NonNull
    private static UiDimens.Length.Units parseUnits(final byte v) {
        switch (v) {
            case 1:
                return UiDimens.Length.Units.PARENT_WIDTH;
            case 2:
                return UiDimens.Length.Units.PARENT_HEIGHT;
            case 3:
                return UiDimens.Length.Units.PARENT_MIN_DIM;
            case 4:
                return UiDimens.Length.Units.PARENT_MAX_DIM;
            default:
                throw new ParseException(MSG_BAD_UNITS);
        }
    }

    @NonNull
    private static AdvancedScaleDrawable wrapWithPlacement(@NonNull final Drawable drawable,
                                                           @NonNull final DataInputStream data)
            throws IOException {
        final AdvancedScaleDrawable r = new AdvancedScaleDrawable(drawable);
        r.left.set(data.readFloat(), parseUnits(data.readByte()));
        r.top.set(data.readFloat(), parseUnits(data.readByte()));
        r.right.set(data.readFloat(), parseUnits(data.readByte()));
        r.bottom.set(data.readFloat(), parseUnits(data.readByte()));
        return r;
    }

    @NonNull
    public static LayerDrawable create(@NonNull final InputStream source)
            throws IOException {
        final DataInputStream data = new DataInputStream(source);
        final int version = data.readByte();
        if (version != 1)
            throw new ParseException("Unsupported format version");
        final List<Drawable> r = new LinkedList<>();
        while (true) {
            int blobLen;
            try {
                blobLen = data.readInt();
            } catch (final EOFException e) {
                break;
            }
            final Collection<Drawable> pd = new LinkedList<>();
            if (blobLen > 0) {
                final LimitedInputStream subStream =
                        new LimitedInputStream(data, blobLen);
                final Drawable drawable = loadNinePatch(subStream);
                subStream.skip(Long.MAX_VALUE);
                setDensity(drawable, data.readShort() & 0xFFFF);
                final byte flags = data.readByte();
                drawable.setFilterBitmap(is(flags, F_FILTER_BITMAP));
                pd.add(wrapWithPlacement(drawable, data));
            }
            blobLen = data.readInt();
            if (blobLen > 0) {
                final LimitedInputStream subStream =
                        new LimitedInputStream(data, blobLen);
                final Drawable drawable = loadTexture(subStream);
                subStream.skip(Long.MAX_VALUE);
                setDensity(drawable, data.readShort() & 0xFFFF);
                final byte flags = data.readByte();
                drawable.setFilterBitmap(is(flags, F_FILTER_BITMAP));
                pd.add(wrapWithPlacement(drawable, data));
            }
            r.add(ProductDrawable.create(pd));
        }
        return new LayerDrawable(r.toArray(new Drawable[0]));
    }
}
