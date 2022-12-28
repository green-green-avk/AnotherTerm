package name.green_green_avk.compatcolorpicker;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

final class Utils {
    private Utils() {
    }

    @NonNull
    private static ClipData.Item firstItemFromClipboard(@NonNull final Context ctx) {
        final ClipboardManager clipboard =
                (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null)
            throw new IllegalStateException("Can't get ClipboardManager");
        if (!clipboard.hasPrimaryClip())
            throw new IllegalStateException("Clipboard is empty");
        final ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() < 1)
            throw new IllegalStateException("Clipboard is empty");
        final ClipData.Item clipItem = clipData.getItemAt(0);
        if (clipItem == null)
            throw new IllegalStateException("Clipboard is empty");
        return clipItem;
    }

    @Nullable
    static String stringFromClipBoard(@NonNull final Context ctx) {
        final ClipData.Item item;
        try {
            item = firstItemFromClipboard(ctx);
        } catch (final IllegalStateException e) {
            return null;
        }
        final CharSequence r = item.coerceToText(ctx);
        return r != null ? r.toString() : null;
    }

    static void toClipboard(@NonNull final Context ctx, @NonNull final String v) {
        final ClipboardManager clipboard = (ClipboardManager) ctx
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Color value", v));
        Toast.makeText(
                ctx, R.string.ccp_msg_copied_to_clipboard,
                Toast.LENGTH_LONG
        ).show();
    }

    @NonNull
    static Drawable requireDrawable(@NonNull final Context ctx, @DrawableRes final int res) {
        final Drawable r = AppCompatResources.getDrawable(ctx, res);
        if (r == null)
            throw new IllegalArgumentException();
        return r;
    }
}
