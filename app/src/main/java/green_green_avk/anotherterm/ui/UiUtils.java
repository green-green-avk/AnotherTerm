package green_green_avk.anotherterm.ui;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.util.AndroidException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.ShareCompat;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Stack;

import green_green_avk.anotherterm.LinksProvider;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.ScratchpadManager;
import green_green_avk.anotherterm.utils.Misc;

public final class UiUtils {
    private UiUtils() {
    }

    private static final String[] unitPrefixes = {"", "K", "M", "G", "T", "P", "E"};

    @NonNull
    public static String makeHumanReadableBytes(final long bytes) {
        final long v = Math.abs(bytes);
        int shift = 60;
        for (int i = 6; i >= 0; i--, shift -= 10) {
            if (v >> shift != 0) {
                return i > 0 ?
                        String.format(Locale.getDefault(), "%.3f %siB",
                                (float) bytes / (1 << shift), unitPrefixes[i]) :
                        String.format(Locale.getDefault(), "%d B",
                                bytes);
            }
        }
        return "0 B";
    }

    @NonNull
    public static String brief(@NonNull final Context ctx, @NonNull final String v, final int n) {
        return v.length() < n ? v : ctx.getString(R.string.msg_s___, v.substring(0, n));
    }

    @NonNull
    private static Uri _toScratchpad(@NonNull final Context ctx, @NonNull final String v)
            throws IOException {
        final ScratchpadManager sm = Misc.getApplication(ctx).scratchpadManager;
        return sm.getUri(sm.put(v));
    }

    public static void toScratchpad(@NonNull final Context ctx, @Nullable final String v) {
        if (v == null) {
            Toast.makeText(ctx, R.string.msg_nothing_to_save, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            _toScratchpad(ctx, v);
        } catch (final IOException e) {
            Toast.makeText(ctx,
                    ctx.getString(R.string.msg_scratchpad_malfunction_s, e.getLocalizedMessage()),
                    Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(ctx, R.string.msg_saved_to_scratchpad, Toast.LENGTH_SHORT).show();
    }

    /**
     * External URLs sharing.
     */
    public static void shareUri(@NonNull final Activity ctx, @NonNull final Uri uri,
                                @NonNull final String description) {
        // https://stackoverflow.com/questions/29907030/sharing-text-plain-string-via-bluetooth-converts-data-into-html
        // So, via ContentProvider...
        ShareCompat.IntentBuilder.from(ctx).setType("text/html")
                .setStream(LinksProvider.getHtmlWithLink(uri, description)).startChooser();
    }

    /**
     * Content URIs sharing.
     */
    public static void share(@NonNull final Activity ctx, @NonNull final Uri uri) {
        final String type = ctx.getContentResolver().getType(uri);
        ShareCompat.IntentBuilder.from(ctx).setType(type != null ? type : "text/plain")
                .setStream(uri).startChooser();
    }

    @NonNull
    public static Uri uriFromClipboard(@NonNull final Context ctx) {
        final ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) throw new IllegalStateException("Can't get ClipboardManager");
        if (!clipboard.hasPrimaryClip()) throw new IllegalStateException("Clipboard is empty");
        final ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() < 1)
            throw new IllegalStateException("Clipboard is empty");
        final ClipData.Item clipItem = clipData.getItemAt(0);
        if (clipItem == null) throw new IllegalStateException("Clipboard is empty");
        Uri uri = clipItem.getUri();
        if (uri == null) uri = Uri.parse(clipItem.coerceToText(ctx).toString());
        return uri;
    }

    public static void uriToClipboard(@NonNull final Context ctx, @NonNull final Uri uri,
                                      @NonNull final String title) {
        final ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) throw new IllegalStateException("Can't get ClipboardManager");
        clipboard.setPrimaryClip(new ClipData(title,
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_URILIST},
                new ClipData.Item(uri.toString(), null, uri)));
    }

    public static void toClipboard(@NonNull final Context ctx, @NonNull final Uri uri,
                                   @NonNull final String title) {
        final ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) throw new IllegalStateException("Can't get ClipboardManager");
        final String type = ctx.getContentResolver().getType(uri);
        clipboard.setPrimaryClip(new ClipData(title,
                new String[]{type != null ? type : "text/plain"},
                new ClipData.Item(uri)));
        Toast.makeText(ctx, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private static boolean canMarshall(@NonNull final Context ctx, @NonNull final String v) {
        final int max = ctx.getResources().getInteger(R.integer.scratchpad_use_threshold_max);
        final int limit = Misc.getApplication(ctx).settings.scratchpad_use_threshold;
        return limit >= max || v.length() / 512 < limit; // [KiB]
    }

    private static void toClipboard_wa(@NonNull final Context ctx, @NonNull final String v,
                                       @NonNull final String title) {
        try {
            toClipboard(ctx, _toScratchpad(ctx, v), title);
            Toast.makeText(ctx, R.string.msg_via_scratchpad, Toast.LENGTH_SHORT).show();
        } catch (final IOException e) {
            Toast.makeText(ctx,
                    ctx.getString(R.string.msg_scratchpad_malfunction_s, e.getLocalizedMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void toClipboard(@NonNull final Context ctx, @Nullable final String v) {
        final ClipboardManager clipboard =
                (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        if (v == null) {
            Toast.makeText(ctx, R.string.msg_nothing_to_copy_to_clipboard, Toast.LENGTH_SHORT).show();
            return;
        }
        final String title = brief(ctx, v, 16);
        if (!canMarshall(ctx, v)) {
            toClipboard_wa(ctx, v, title);
            return;
        }
        try {
            clipboard.setPrimaryClip(ClipData.newPlainText(title, v));
        } catch (final Throwable e) {
            if (e instanceof AndroidException || e.getCause() instanceof AndroidException) {
                toClipboard_wa(ctx, v, title);
                return;
            }
            throw e;
        }
        Toast.makeText(ctx, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private static void sharePlainText_wa(@NonNull final Activity ctx, @NonNull final String v) {
        try {
            share(ctx, _toScratchpad(ctx, v));
            Toast.makeText(ctx, R.string.msg_via_scratchpad, Toast.LENGTH_SHORT).show();
        } catch (final IOException e) {
            Toast.makeText(ctx,
                    ctx.getString(R.string.msg_scratchpad_malfunction_s, e.getLocalizedMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void sharePlainText(@NonNull final Activity ctx, @Nullable final String v) {
        if (v == null) {
            Toast.makeText(ctx, R.string.msg_nothing_to_share, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canMarshall(ctx, v)) {
            sharePlainText_wa(ctx, v);
            return;
        }
        try {
            ShareCompat.IntentBuilder.from(ctx).setType("text/plain").setText(v).startChooser();
        } catch (final Throwable e) {
            if (e instanceof AndroidException || e.getCause() instanceof AndroidException) {
                sharePlainText_wa(ctx, v);
                return;
            }
            throw e;
        }
    }

    @NonNull
    public static Iterable<View> getIterable(@Nullable final View root) {
        return new Iterable<View>() {

            @NonNull
            @Override
            public Iterator<View> iterator() {
                return new Iterator<View>() {
                    private View v = root;
                    private final Stack<Integer> ii = new Stack<>(); // a little optimization

                    @Override
                    public boolean hasNext() {
                        return v != null;
                    }

                    @Override
                    public View next() {
                        if (v == null) throw new NoSuchElementException();
                        final View r = v;
                        if (v instanceof ViewGroup) {
                            v = ((ViewGroup) v).getChildAt(0);
                            ii.push(0);
                        } else {
                            while (true) {
                                if (v == root) {
                                    v = null;
                                    break;
                                }
                                final ViewGroup p = (ViewGroup) v.getParent();
                                final int i = ii.pop() + 1;
                                if (p.getChildCount() == i) {
                                    v = p;
                                    continue;
                                }
                                v = p.getChildAt(i);
                                ii.push(i);
                                break;
                            }
                        }
                        return r;
                    }
                };
            }
        };
    }

    public static void enableAnimation(@NonNull final View root) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
        if (root instanceof ViewGroup) {
            final LayoutTransition lt = ((ViewGroup) root).getLayoutTransition();
            if (lt != null)
                lt.enableTransitionType(LayoutTransition.CHANGING);
            for (int i = 0; i < ((ViewGroup) root).getChildCount(); ++i) {
                final View v = ((ViewGroup) root).getChildAt(i);
                enableAnimation(v);
            }
        }
    }

    /**
     * One can't simply get horizontal scrollability when `scrollHorizontally' is set...
     */
    public static boolean canScrollHorizontally(@NonNull final TextView w) {
        final Layout l = w.getLayout();
        float width = 0;
        for (int i = 0; i < l.getLineCount(); i++)
            width = Math.max(width, l.getLineRight(i));
        return width > (w.getWidth() - w.getTotalPaddingLeft() - w.getTotalPaddingRight());
    }

    @UiThread
    public static void confirm(@NonNull final Context ctx, @NonNull final String msg,
                               @NonNull final Runnable onConfirm) {
        new AlertDialog.Builder(ctx)
                .setMessage(msg)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.dismiss();
                        onConfirm.run();
                    }
                })
                .show();
    }

    public static void showContextMenuOnBottom(@NonNull final View v) {
        if (Build.VERSION.SDK_INT < 24) {
            v.showContextMenu();
        } else {
            v.showContextMenu(0, v.getHeight());
        }
    }

    public static void setShowContextMenuOnClick(@NonNull final View v) {
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                showContextMenuOnBottom(v);
                return true;
            }
        });
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                showContextMenuOnBottom(v);
            }
        });
    }
}
