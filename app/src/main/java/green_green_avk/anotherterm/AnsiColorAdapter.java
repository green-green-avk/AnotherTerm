package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import green_green_avk.anotherterm.ui.ProfileAdapter;
import green_green_avk.anotherterm.ui.UniAdapter;
import green_green_avk.anotherterm.utils.ProfileManager;

public final class AnsiColorAdapter extends ProfileAdapter<AnsiColorProfile> {
    public AnsiColorAdapter(@NonNull final Context context) {
        super(context);
    }

    @Override
    @NonNull
    protected AnsiColorManager getManager() {
        return AnsiColorManagerUi.instance.getManager(context);
    }

    @Override
    public void startEditor(@Nullable final String name) {
        AnsiColorEditorActivity.start(context, name);
    }

    private static final class EditablePreviewDrawable extends Drawable {
        @NonNull
        private final AnsiColorProfile.Editable profile;
        private final Paint paint = new Paint();

        {
            paint.setStyle(Paint.Style.FILL);
        }

        private EditablePreviewDrawable(@NonNull final AnsiColorProfile.Editable profile) {
            this.profile = profile;
        }

        @Override
        public void draw(@NonNull final Canvas canvas) {
            final Rect bounds = getBounds();
            final int wStep = bounds.width() / 3;
            final int hStep = bounds.height() / 10;
            int hPos = bounds.bottom - hStep * 9;
            paint.setColor(profile.getDefaultBg());
            canvas.drawRect(bounds.left, bounds.top, bounds.right, hPos, paint);
            paint.setColor(profile.getDefaultFgNormal());
            canvas.drawRect(bounds.left, hPos,
                    bounds.left + wStep, hPos + hStep,
                    paint);
            paint.setColor(profile.getDefaultFgBold());
            canvas.drawRect(bounds.left + wStep, hPos,
                    bounds.right - wStep, hPos + hStep,
                    paint);
            paint.setColor(profile.getDefaultFgFaint());
            canvas.drawRect(bounds.right - wStep, hPos,
                    bounds.right, hPos + hStep,
                    paint);
            for (int i = 0; i < 8; i++) {
                hPos += hStep;
                paint.setColor(profile.getBasicNormal(i));
                canvas.drawRect(bounds.left, hPos,
                        bounds.left + wStep, hPos + hStep,
                        paint);
                paint.setColor(profile.getBasicBold(i));
                canvas.drawRect(bounds.left + wStep, hPos,
                        bounds.right - wStep, hPos + hStep,
                        paint);
                paint.setColor(profile.getBasicFaint(i));
                canvas.drawRect(bounds.right - wStep, hPos,
                        bounds.right, hPos + hStep,
                        paint);
            }
        }

        @Override
        public int getIntrinsicWidth() {
            return Short.MAX_VALUE;
        }

        @Override
        public int getIntrinsicHeight() {
            return Short.MAX_VALUE;
        }

        @Override
        public void setAlpha(final int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable final ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    @Override
    @Nullable
    protected Drawable onGetPreview(@NonNull final ProfileManager.Meta meta) {
        final AnsiColorProfile profile = getManager().get(meta);
        if (profile instanceof AnsiColorProfile.Editable)
            return new EditablePreviewDrawable((AnsiColorProfile.Editable) profile);
        return null;
    }

    @NonNull
    public static AnsiColorAdapter getSelf(@Nullable final Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }

    @NonNull
    public static AnsiColorAdapter getSelf(@Nullable final RecyclerView.Adapter adapter) {
        return UniAdapter.getSelf(adapter);
    }
}
