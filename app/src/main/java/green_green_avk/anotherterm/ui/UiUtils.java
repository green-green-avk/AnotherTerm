package green_green_avk.anotherterm.ui;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.util.AndroidException;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.ShareCompat;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.LinksProvider;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.WeakHandler;

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
                return String.format(Locale.getDefault(), "%.3f %s%sB",
                        (float) bytes / (1 << shift), unitPrefixes[i], i > 0 ? "i" : "");
            }
        }
        return "0";
    }

    @NonNull
    public static String brief(@NonNull final Context ctx, @NonNull final String v, final int n) {
        return v.length() < n ? v : ctx.getString(R.string.msg_s___, v.substring(0, n));
    }

    public static void shareUri(@NonNull final Activity ctx, @NonNull final Uri uri,
                                @NonNull final String description) {
        // https://stackoverflow.com/questions/29907030/sharing-text-plain-string-via-bluetooth-converts-data-into-html
        // So, via ContentProvider...
        ShareCompat.IntentBuilder.from(ctx).setType("text/html")
                .setStream(LinksProvider.getHtmlWithLink(uri, description)).startChooser();
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
//        clipboard.setPrimaryClip(ClipData.newRawUri(title, BackendsList.toUri(ps.get())));
        clipboard.setPrimaryClip(new ClipData(title,
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_URILIST},
                new ClipData.Item(uri.toString(), null, uri)));
    }

    public static void toClipboard(@NonNull final Context context, @Nullable final String v) {
        final ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        if (v == null) {
            Toast.makeText(context, R.string.msg_nothing_to_copy_to_clipboard, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            clipboard.setPrimaryClip(ClipData.newPlainText(brief(context, v, 16), v));
        } catch (final Throwable e) {
            if (e instanceof AndroidException || e.getCause() instanceof AndroidException) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.msg_text_is_too_large_to_be_copied_into_clipboard)
                        .setMessage(e.getMessage())
                        .show();
                return;
            }
            throw e;
        }
        Toast.makeText(context, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public static void sharePlainText(@NonNull final Activity context, @Nullable final String v) {
        if (v == null) {
            Toast.makeText(context, R.string.msg_nothing_to_share, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ShareCompat.IntentBuilder.from(context).setType("text/plain").setText(v).startChooser();
        } catch (final Throwable e) {
            if (e instanceof AndroidException || e.getCause() instanceof AndroidException) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.msg_text_is_too_large_to_be_shared)
                        .setMessage(e.getMessage())
                        .show();
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

    /* Workaround: the soft keyboard usually remains visible on some devices
    after the dialog ends */
    public static void hideIME(@NonNull final Dialog dialog) {
        final InputMethodManager imm =
                (InputMethodManager) dialog.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        final Window w = dialog.getWindow();
        if (w == null) return;
        imm.hideSoftInputFromWindow(w.getDecorView().getWindowToken(), 0);
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

    public static int getNavBarHeight(@NonNull final Context ctx) {
        final Resources resources = ctx.getResources();
        int id = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0) {
            return resources.getDimensionPixelSize(id);
        }
        return 0;
    }

    public static void showMessage(@NonNull final Activity ctx, final String msg) {
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(ctx)
                        .setMessage(msg)
                        .show();
            }
        });
    }

    @UiThread
    public static void confirm(@NonNull final Context ctx, final String msg, final Runnable onConfirm) {
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

//    protected static final Object isRunShrinkBottom = new Object();

    public static void setShrinkBottomWhenCovered(@NonNull final Activity activity) {
        final ViewGroup v = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver observer = v.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final Rect r = new Rect();
                v.getWindowVisibleDisplayFrame(r);
//                Log.d("Immersive", String.format("%d - %d = %d", v.getBottom(), v.getPaddingBottom(), r.bottom));
                if (v.getBottom() - v.getPaddingBottom() != r.bottom) {
//                    Log.d("Immersive", String.format("%d - %d = %d", v.getBottom(), v.getPaddingBottom(), r.bottom));
                    int h = v.getBottom() - r.bottom;
//                    if (h <= getNavBarHeight(activity)) h = 0;
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), h);
                }
            }
        });
    }

    public static void hideSystemUi(@NonNull final Activity activity) {
        final View v = activity.getWindow().getDecorView();
        if ((v.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0)
            v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public static void setHiddenSystemUi(@NonNull final Activity activity) {
        final View v = activity.getWindow().getDecorView();
        v.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    UiUtils.hideSystemUi(activity);
                }
            }
        });
        UiUtils.hideSystemUiImmersive(activity);
    }

    public static void hideSystemUiImmersive(@NonNull final Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    protected static final Map<Object, WeakHandler> handlers = new WeakHashMap<>();

    public static void runWithDelay(@NonNull final Object activity,
                                    @NonNull final Runnable r, @NonNull final Object token,
                                    final int delayMillis) {
        WeakHandler h = handlers.get(activity);
        if (h == null) {
            h = new WeakHandler();
            handlers.put(activity, h);
        } else h.removeCallbacksAndMessages(token);
        h.postDelayed(r, delayMillis);
    }

    protected static final Object isRunHideSystemUi = new Object();

    public static void setHiddenSystemUiImmersive(@NonNull final Activity activity) {
        final View v = activity.getWindow().getDecorView();
        v.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        UiUtils.hideSystemUiImmersive(activity);
                    }
                    runWithDelay(activity, new Runnable() { // Seems like race condition with IME
                        @Override
                        public void run() {
                            UiUtils.hideSystemUiImmersive(activity);
                        }
                    }, isRunHideSystemUi, 1000);
                }
            }
        });
        UiUtils.hideSystemUiImmersive(activity);
    }
}
