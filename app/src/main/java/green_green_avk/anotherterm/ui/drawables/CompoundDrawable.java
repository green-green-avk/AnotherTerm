package green_green_avk.anotherterm.ui.drawables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.NinePatch;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;

import org.jetbrains.annotations.Contract;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
         * No actual multiplication layer separation at the moment.
         * <p>
         * Because {@link Canvas#saveLayer} does not support
         * {@link BitmapDrawable}'s {@link android.graphics.Paint#setXfermode}.
         * Possibly only the {@link Canvas#drawRect}
         * with its paint's shader of type {@link android.graphics.BitmapShader}
         * is not supported.
         * <p>
         * To be decided to use the {@link Shader} subclasses with a complete redesign.
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
            @NonNull
            public List<? extends Drawable> getChildren() {
                return drawables;
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
        public abstract List<? extends Drawable> getChildren();

        @NonNull
        public static ProductDrawable create(@NonNull final Collection<? extends Drawable> drawables) {
            return new LegacyProductDrawable(drawables);
        }
    }

    public static class ParseException extends RuntimeException {
        private ParseException(@NonNull final String message) {
            super(message);
        }

        private ParseException(@NonNull final String message, @NonNull final Throwable cause) {
            super(message, cause);
        }

        private ParseException(@NonNull final Throwable cause) {
            super(cause);
        }
    }

    private static final String MSG_BAD_IMAGE = "Error reading image data";

    private static final class ExtNinePatchDrawable extends NinePatchDrawable {
        @NonNull
        private final Bitmap bitmap;
        private final Rect sourcePadding;

        public ExtNinePatchDrawable(@NonNull final Bitmap bitmap, final byte[] chunk,
                                    final Rect padding, final String srcName) {
            super(bitmap, chunk, padding, srcName);
            this.bitmap = bitmap;
            this.sourcePadding = padding; // Similar to the constructor: not a copy
        }

        @NonNull
        public Bitmap getBitmap() {
            return bitmap;
        }

        public Rect getSourcePadding() {
            return sourcePadding;
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

    /**
     * Copies a {@link CompoundDrawable#create(InputStream)} result for each use
     * withholding underlying bitmaps from copying.
     *
     * @param drawable to copy
     * @return a new instance
     */
    @Contract("null -> null; !null -> !null")
    @Nullable
    public static Drawable copy(@Nullable final Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable from = (BitmapDrawable) drawable;
            final BitmapDrawable to = new BitmapDrawable(from.getBitmap());
            to.setTileModeXY(from.getTileModeX(), from.getTileModeY());
            to.setFilterBitmap(from.getPaint().isFilterBitmap());
            return to;
        }
        if (drawable instanceof ExtNinePatchDrawable) {
            final ExtNinePatchDrawable from = (ExtNinePatchDrawable) drawable;
            final ExtNinePatchDrawable to = new ExtNinePatchDrawable(from.getBitmap(),
                    from.getBitmap().getNinePatchChunk(), from.getSourcePadding(), null);
            to.setFilterBitmap(from.getPaint().isFilterBitmap());
            return to;
        }
        if (drawable instanceof AdvancedScaleDrawable) {
            final AdvancedScaleDrawable from = (AdvancedScaleDrawable) drawable;
            final AdvancedScaleDrawable to = new AdvancedScaleDrawable(copy(from.getDrawable()));
            to.left.set(from.left);
            to.top.set(from.top);
            to.right.set(from.right);
            to.bottom.set(from.bottom);
            return to;
        }
        if (drawable instanceof ProductDrawable) {
            final ProductDrawable from = (ProductDrawable) drawable;
            final List<Drawable> children = new ArrayList<>(from.getChildren().size());
            for (final Drawable child : from.getChildren()) {
                children.add(copy(child));
            }
            return ProductDrawable.create(children);
        }
        if (drawable instanceof LayerDrawable) {
            final LayerDrawable from = (LayerDrawable) drawable;
            final Drawable[] children = new Drawable[from.getNumberOfLayers()];
            for (int i = 0; i < children.length; i++) {
                children[i] = copy(from.getDrawable(i));
            }
            return new LayerDrawable(children);
        }
        Log.e(CompoundDrawable.class.getSimpleName(),
                "Unable to copy " + drawable.getClass().getName());
        return drawable.mutate();
    }
}
