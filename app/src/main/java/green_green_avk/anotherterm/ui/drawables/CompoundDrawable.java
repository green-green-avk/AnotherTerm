package green_green_avk.anotherterm.ui.drawables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import green_green_avk.anotherterm.ui.UiDimens;
import green_green_avk.anotherterm.utils.LimitedInputStream;

public final class CompoundDrawable {
    private CompoundDrawable() {
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
        return new BitmapTextureDrawable(bitmap);
    }

    private static void setDensity(@NonNull final Drawable drawable, final int density) {
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).getBitmap().setDensity(density);
        } else if (drawable instanceof ExtNinePatchDrawable) {
            ((ExtNinePatchDrawable) drawable).getBitmap().setDensity(density);
        }
    }

    private static void setXfermode(@NonNull final Drawable drawable,
                                    @Nullable final Xfermode xfermode) {
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).getPaint().setXfermode(xfermode);
        } else if (drawable instanceof BitmapTextureDrawable) {
            ((BitmapTextureDrawable) drawable).setXfermode(xfermode);
        } else if (drawable instanceof ExtNinePatchDrawable) {
            ((ExtNinePatchDrawable) drawable).getPaint().setXfermode(xfermode);
        }
    }

    private static final PorterDuffXfermode[] F_XFERMODE = new PorterDuffXfermode[]{
            new PorterDuffXfermode(PorterDuff.Mode.CLEAR),
            new PorterDuffXfermode(PorterDuff.Mode.SRC),
            new PorterDuffXfermode(PorterDuff.Mode.DST),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER),
            new PorterDuffXfermode(PorterDuff.Mode.DST_OVER),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_IN),
            new PorterDuffXfermode(PorterDuff.Mode.DST_IN),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT),
            new PorterDuffXfermode(PorterDuff.Mode.DST_OUT),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP),
            new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP),
            new PorterDuffXfermode(PorterDuff.Mode.XOR),
            new PorterDuffXfermode(PorterDuff.Mode.ADD),
            new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY),
            new PorterDuffXfermode(PorterDuff.Mode.SCREEN),
            new PorterDuffXfermode(PorterDuff.Mode.OVERLAY),
            new PorterDuffXfermode(PorterDuff.Mode.DARKEN),
            new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
    };
    private static final int F_XFERMODE_MASK = 0x1F;
    private static final int F_FILTER_BITMAP = 0x80;

    private static final int F_AXIS_SHIFT = 4;
    private static final int F_AXIS_TILE_MODE_MASK = 7;
    private static final Shader.TileMode[] F_AXIS_TILE_MODE = new Shader.TileMode[]{
            Shader.TileMode.CLAMP,
            Shader.TileMode.REPEAT,
            Shader.TileMode.MIRROR
    };
    private static final int F_AXIS_TILE_EXT = 8; // TODO: Implement in future.

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
            case 5:
                return UiDimens.Length.Units.DP;
            case 6:
                return UiDimens.Length.Units.SP;
            default:
                throw new ParseException(MSG_BAD_UNITS);
        }
    }

    private static final String MSG_BAD_XFERMODE = "Bad xfer mode";

    private static PorterDuffXfermode parseXfermode(final byte v) {
        try {
            return F_XFERMODE[v & F_XFERMODE_MASK];
        } catch (final IndexOutOfBoundsException e) {
            throw new ParseException(MSG_BAD_XFERMODE);
        }
    }

    private static final String MSG_BAD_TILE_MODE = "Bad tile mode";

    @NonNull
    private static Shader.TileMode parseTileModeX(final byte v) {
        try {
            return F_AXIS_TILE_MODE[v & F_AXIS_TILE_MODE_MASK];
        } catch (final IndexOutOfBoundsException e) {
            throw new ParseException(MSG_BAD_TILE_MODE);
        }
    }

    private static void checkTileExtX(final byte v) {
        if (is(v, F_AXIS_TILE_EXT))
            throw new ParseException("Not implemented yet");
    }

    @NonNull
    private static Shader.TileMode parseTileModeY(final byte v) {
        return parseTileModeX((byte) (v >>> F_AXIS_SHIFT));
    }

    private static void checkTileExtY(final byte v) {
        checkTileExtX((byte) (v >>> F_AXIS_SHIFT));
    }

    private static void parseLength(@NonNull final UiDimens.Length out,
                                    @NonNull final DataInputStream data)
            throws IOException {
        while (true) {
            final byte u = data.readByte();
            if (u == 0)
                return;
            out.add(data.readFloat(), parseUnits(u));
        }
    }

    @NonNull
    private static AdvancedScaleDrawable wrapWithPlacement(@NonNull final Drawable drawable,
                                                           @NonNull final DataInputStream data)
            throws IOException {
        final AdvancedScaleDrawable r = new AdvancedScaleDrawable(drawable);
        parseLength(r.left, data);
        parseLength(r.top, data);
        parseLength(r.right, data);
        parseLength(r.bottom, data);
        return r;
    }

    private static final int TAG_INLINE_LAYER = 1; // Inlined images: no pool

    /**
     * A way to load something fancy packaged into a PNG.
     * <p>
     * It does not set the target density
     * as it is supposed to be used with {@link #copy(Context, Drawable)}.
     *
     * @param source to decode
     * @return the resulting drawable
     * @throws IOException
     * @throws ParseException
     * @see #copy(Context, Drawable)
     */
    @NonNull
    public static LayerDrawable create(@NonNull final InputStream source)
            throws IOException {
        final DataInputStream data = new DataInputStream(source);
        final int version = data.readByte();
        if (version != 1)
            throw new ParseException("Unsupported format version");
        final List<Drawable> r = new LinkedList<>();
        while (true) {
            final byte tagType;
            try {
                tagType = data.readByte();
            } catch (final EOFException e) {
                break;
            }
            switch (tagType) {
                case TAG_INLINE_LAYER: {
                    final int imageBlobLen = data.readInt();
                    if (imageBlobLen > 0) {
                        final LimitedInputStream subStream =
                                new LimitedInputStream(data, imageBlobLen);
                        final Drawable drawable = loadNinePatch(subStream);
                        subStream.skip(Long.MAX_VALUE);
                        if (drawable instanceof BitmapTextureDrawable) {
                            final byte texFlags = data.readByte();
                            ((BitmapTextureDrawable) drawable).setTileModeXY(
                                    parseTileModeX(texFlags),
                                    parseTileModeY(texFlags)
                            );
                            checkTileExtX(texFlags);
                            checkTileExtY(texFlags);
                            parseLength(((BitmapTextureDrawable) drawable).tileSizeX, data);
                            parseLength(((BitmapTextureDrawable) drawable).tileSizeY, data);
                            parseLength(((BitmapTextureDrawable) drawable).tileOffsetX, data);
                            parseLength(((BitmapTextureDrawable) drawable).tileOffsetY, data);
                        } else if (drawable instanceof ExtNinePatchDrawable) {
                            setDensity(drawable, data.readShort() & 0x7FFF);
                        }
                        final byte flags = data.readByte();
                        setXfermode(drawable, parseXfermode(flags));
                        drawable.setFilterBitmap(is(flags, F_FILTER_BITMAP));
                        r.add(wrapWithPlacement(drawable, data));
                    }
                    break;
                }
                default:
                    throw new ParseException("Unknown tag type");
            }
        }
        return new LayerDrawable(r.toArray(new Drawable[0]));
    }

    /**
     * Copies a {@link CompoundDrawable#create(InputStream)} result
     * to bind with each particular view
     * withholding underlying bitmaps from copying
     * and populating them with the context parameters.
     *
     * @param ctx      to get for
     * @param drawable to copy
     * @return a new instance
     */
    @Contract("_, null -> null; _, !null -> !null")
    @Nullable
    public static Drawable copy(@NonNull final Context ctx, @Nullable final Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable from = (BitmapDrawable) drawable;
            final BitmapDrawable to = new BitmapDrawable(from.getBitmap());
            to.setTargetDensity(ctx.getResources().getDisplayMetrics());
            to.getPaint().setXfermode(from.getPaint().getXfermode());
            to.setTileModeXY(from.getTileModeX(), from.getTileModeY());
            to.setFilterBitmap(from.getPaint().isFilterBitmap());
            return to;
        }
        if (drawable instanceof BitmapTextureDrawable) {
            final BitmapTextureDrawable from = (BitmapTextureDrawable) drawable;
            final BitmapTextureDrawable to = new BitmapTextureDrawable(from.getBitmap());
            to.setMetrics(ctx.getResources().getDisplayMetrics());
            to.setXfermode(from.getXfermode());
            to.setTileModeXY(from.getTileModeX(), from.getTileModeY());
            to.tileSizeX.set(from.tileSizeX);
            to.tileSizeY.set(from.tileSizeY);
            to.tileOffsetX.set(from.tileOffsetX);
            to.tileOffsetY.set(from.tileOffsetY);
            to.setFilterBitmap(from.isFilterBitmap());
            return to;
        }
        if (drawable instanceof ExtNinePatchDrawable) {
            final ExtNinePatchDrawable from = (ExtNinePatchDrawable) drawable;
            final ExtNinePatchDrawable to = new ExtNinePatchDrawable(from.getBitmap(),
                    from.getBitmap().getNinePatchChunk(), from.getSourcePadding(), null);
            to.setTargetDensity(ctx.getResources().getDisplayMetrics());
            to.getPaint().setXfermode(from.getPaint().getXfermode());
            to.setFilterBitmap(from.getPaint().isFilterBitmap());
            return to;
        }
        if (drawable instanceof AdvancedScaleDrawable) {
            final AdvancedScaleDrawable from = (AdvancedScaleDrawable) drawable;
            final AdvancedScaleDrawable to =
                    new AdvancedScaleDrawable(copy(ctx, from.getDrawable()));
            to.setMetrics(ctx.getResources().getDisplayMetrics());
            to.left.set(from.left);
            to.top.set(from.top);
            to.right.set(from.right);
            to.bottom.set(from.bottom);
            return to;
        }
        if (drawable instanceof LayerDrawable) {
            final LayerDrawable from = (LayerDrawable) drawable;
            final Drawable[] children = new Drawable[from.getNumberOfLayers()];
            for (int i = 0; i < children.length; i++) {
                children[i] = copy(ctx, from.getDrawable(i));
            }
            return new LayerDrawable(children);
        }
        Log.e(CompoundDrawable.class.getSimpleName(),
                "Unable to copy " + drawable.getClass().getName());
        return drawable.mutate();
    }
}
