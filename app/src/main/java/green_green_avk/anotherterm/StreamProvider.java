package green_green_avk.anotherterm;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class StreamProvider extends ContentProvider {
    private static final int CODE_STREAM = 1;

    private static volatile StreamProvider instance = null;

    private String authority = null;
    private final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    public interface OnResult {
        void onResult(Object msg);
    }

    private static final class Stream {
        private final InputStream stream;
        private final String mimeType;
        private final String name;
        private final Integer size;
        private final OnResult onResult;

        private Stream(@NonNull final InputStream stream, @NonNull final String mimeType,
                       @Nullable final String name, @Nullable final Integer size,
                       @Nullable final OnResult onResult) {
            this.stream = stream;
            this.mimeType = mimeType;
            this.name = name;
            this.size = size;
            this.onResult = onResult;
        }
    }

    private final SparseArray<Stream> streams = new SparseArray<>();
    private final Object stateLock = new Object();

    private static int obtainId() {
        return (int) UUID.randomUUID().getLeastSignificantBits() & 0xFFFF;
    }

    @NonNull
    private Stream getStream(final int id) {
        synchronized (stateLock) {
            final Stream s = streams.get(id);
            if (s == null) throw new IllegalArgumentException("No such stream");
            return s;
        }
    }

    private int putStream(@NonNull final InputStream stream, @NonNull final String mime,
                          @Nullable final String name, @Nullable final Integer size,
                          @Nullable final OnResult onResult) {
        synchronized (stateLock) {
            final int id = obtainId();
            streams.append(id, new Stream(stream, mime, name, size, onResult));
            return id;
        }
    }

    private void removeStream(final int id) {
        synchronized (stateLock) {
            streams.delete(id);
        }
    }

    @NonNull
    private static Uri getUriById(final int id) {
        return Uri.parse("content://" + instance.authority + "/stream/" + id);
    }

    private static int getId(@NonNull final Uri uri) {
        final String id = uri.getLastPathSegment();
        if (id == null) throw new NumberFormatException("null");
        return Integer.parseInt(id);
    }

    private static void checkState() {
        if (instance == null) throw new IllegalStateException("Stream Provider is not ready");
    }

    @NonNull
    public static Uri obtainUri(@NonNull final InputStream inputStream, @NonNull final String mime,
                                @Nullable final String name, @Nullable final Integer size,
                                @Nullable final OnResult onResult) {
        checkState();
        final int id = instance.putStream(inputStream, mime, name, size, onResult);
        return getUriById(id);
    }

    public static void releaseUri(@NonNull final Uri uri) {
        checkState();
        try {
            instance.removeStream(getId(uri));
        } catch (final NumberFormatException ignored) {
        }
    }

    @NonNull
    public static Set<Uri> getBoundUriList() {
        checkState();
        final HashSet<Uri> r = new HashSet<>();
        synchronized (instance.stateLock) {
            if (instance.streams.size() <= 0) return Collections.emptySet();
            for (int i = 0; i < instance.streams.size(); ++i) {
                r.add(getUriById(instance.streams.keyAt(i)));
            }
        }
        return Collections.unmodifiableSet(r);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void attachInfo(final Context context, final ProviderInfo info) {
        super.attachInfo(context, info);
        authority = info.authority;
        matcher.addURI(authority, "/stream/*", CODE_STREAM);
        instance = this;
    }

    @Override
    public int delete(@NonNull final Uri uri, final String selection,
                      final String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        try {
            return getStream(getId(uri)).mimeType;
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public String[] getStreamTypes(@NonNull final Uri uri, @NonNull final String mimeTypeFilter) {
        try {
            return new String[]{getStream(getId(uri)).mimeType};
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    private final PipeDataWriter<String> streamWriter = new PipeDataWriter<String>() {
        @Override
        public void writeDataToPipe(@NonNull final ParcelFileDescriptor output,
                                    @NonNull final Uri uri, @NonNull final String mimeType,
                                    @Nullable final Bundle opts, @Nullable final String args) {
            final int id;
            final Stream is;
            try {
                id = getId(uri);
                is = getStream(id);
            } catch (final IllegalArgumentException e) {
                if (BuildConfig.DEBUG)
                    Log.w(this.getClass().getSimpleName(), "Invalid id");
                return;
            }
            final FileOutputStream os = new FileOutputStream(output.getFileDescriptor());
            final byte[] buf = new byte[8192];
            try {
                while (true) {
                    final int r = is.stream.read(buf);
                    if (r < 0) break;
                    try {
                        os.write(buf, 0, r);
                    } catch (final IOException ignored) {
                        return;
                    }
                }
            } catch (final IOException ignored) {
            }
            removeStream(id);
            if (is.onResult != null) is.onResult.onResult(null);
        }
    };

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull final Uri uri, @NonNull final String mode)
            throws FileNotFoundException {
        switch (matcher.match(uri)) {
            case CODE_STREAM: {
                return openPipeHelper(uri, "*/*", null, null, streamWriter);
            }
        }
        return super.openFile(uri, mode);
    }

    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder) {
        final int id;
        final Stream is;
        try {
            id = getId(uri);
            is = getStream(id);
        } catch (final IllegalArgumentException e) {
            if (BuildConfig.DEBUG)
                Log.w(this.getClass().getSimpleName(), "Invalid id");
            return null;
        }
        switch (matcher.match(uri)) {
            case CODE_STREAM: {
                final String name = (is.name == null) ?
                        String.format(Locale.getDefault(), "Stream #%d", id) :
                        is.name;
                final MatrixCursor cursor =
                        new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                                1);
                cursor.addRow(new Object[]{
                        name,
                        is.size
                });
                return cursor;
            }
        }
        return null;
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
}
