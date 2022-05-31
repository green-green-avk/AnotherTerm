package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.io.InputStream;

import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.Misc;

public final class ContentRequester extends Requester {
    private ContentRequester() {
    }

    public enum Type {
        BYTES,
        STREAM,
        URI,
        PERSISTENT_URI
    }

    public static final class UIFragment extends Requester.UiFragment {
        private final int requestCode = generateRequestCode();

        private BlockingSync<Object> result = null;
        private Type type = null;
        private long sizeLimit = 0;

        @Override
        public void onActivityResult(final int requestCode, final int resultCode,
                                     final Intent data) {
            if (requestCode != this.requestCode) return;
            recycle();
            if (data == null) {
                result.set(null);
                return;
            }
            requestPersistentInternal(result, type, sizeLimit,
                    requireContext(), data.getData());
        }

        private void requestContent(@NonNull final BlockingSync<Object> result,
                                    final Type type,
                                    final long sizeLimit,
                                    @NonNull final CharSequence message,
                                    @NonNull final String mimeType) {
            final boolean tryPersistent =
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
                            && type == Type.PERSISTENT_URI;
            this.result = result;
            this.type = type;
            this.sizeLimit = sizeLimit;
            final Intent i = new Intent(tryPersistent ?
                    Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType(mimeType);
            startActivityForResult(Intent.createChooser(i, message),
                    requestCode);
        }
    }

    public static void request(@NonNull final BlockingSync<Object> result,
                               final Type type,
                               final long sizeLimit,
                               @NonNull final Context ctx,
                               @NonNull final CharSequence message,
                               @NonNull final String mimeType) {
        ((FragmentActivity) ctx).runOnUiThread(() ->
                prepare(ctx, new UIFragment()).requestContent(result,
                        type, sizeLimit,
                        message, mimeType));
    }

    public static void requestPersistent(@NonNull final BlockingSync<Object> result,
                                         final Type type,
                                         final long sizeLimit,
                                         @NonNull final Context ctx,
                                         @Nullable final Uri uri) {
        new Handler(Looper.getMainLooper()).post(() -> requestPersistentInternal(
                result, type, sizeLimit, ctx, uri));
    }

    public static void releasePersistent(@NonNull final Context ctx, @Nullable final Uri uri) {
        if (uri == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ctx.getContentResolver().releasePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    @UiThread
    private static void requestPersistentInternal(@NonNull final BlockingSync<Object> result,
                                                  final Type type,
                                                  final long sizeLimit,
                                                  @NonNull final Context ctx,
                                                  @Nullable final Uri uri) {
        if (uri == null) {
            result.set(null);
            return;
        }
        if (type == Type.PERSISTENT_URI) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    ctx.getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (final Throwable e) {
                    result.set(e);
                    return;
                }
            }
            result.set(uri);
            return;
        }
        if (type == Type.URI) {
            result.set(uri);
            return;
        }
        final InputStream is;
        try {
            is = ctx.getContentResolver().openInputStream(uri);
        } catch (final Throwable e) {
            result.set(e);
            return;
        }
        if (is == null) {
            result.set(new IOException("Cannot open asset(?) for some reason..."));
            return;
        }
        if (type == Type.STREAM) {
            result.set(is);
            return;
        }
        @SuppressLint("StaticFieldLeak") final AsyncTask<Object, Object, Object> task =
                new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(final Object... params) {
                        final byte[] buf;
                        try {
                            buf = Misc.toArray(is, sizeLimit);
                        } catch (final Throwable e) {
                            result.set(e);
                            return null;
                        } finally {
                            try {
                                is.close();
                            } catch (final Throwable ignored) {
                            }
                        }
                        result.set(buf);
                        return null;
                    }
                };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[]) null);
    }
}
