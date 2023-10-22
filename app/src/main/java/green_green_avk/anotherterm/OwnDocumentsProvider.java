package green_green_avk.anotherterm;

import static android.provider.DocumentsContract.buildDocumentUri;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import green_green_avk.ptyprocess.PtyProcess;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public final class OwnDocumentsProvider extends DocumentsProvider {
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
    };
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_ICON,
            DocumentsContract.Document.COLUMN_SIZE
    };

    @NonNull
    private static String[] resolveRootProjection(@Nullable final String[] projection) {
        return projection == null ? DEFAULT_ROOT_PROJECTION : projection;
    }

    @NonNull
    private static String[] resolveDocumentProjection(@Nullable final String[] projection) {
        return projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection;
    }

    private static final String PUBLIC_ROOT_ID = "public";
    private static final String PUBLIC_ROOT_DIR_ID = "public";

    @NonNull
    private static String getExtension(@NonNull final File file) {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i > 0)
            return name.substring(i + 1);
        return "";
    }

    @NonNull
    private static String getMimeType(@NonNull final File file) {
        if (file.isDirectory())
            return DocumentsContract.Document.MIME_TYPE_DIR;
        final String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(file));
        return type == null ? "application/octet-stream" : type;
    }

    private static boolean isDirMimeType(@Nullable final String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    private static boolean isValidFilePathSegment(@NonNull final String v) {
        return v.indexOf('/') < 0;
    }

    private static boolean isRoot(@NonNull final String id) {
        return id.indexOf('/') < 0;
    }

    @NonNull
    private File getFileForDocId(@NonNull final String id) throws FileNotFoundException {
        // Alas, java.nio.file.Path is available since API 26 only: working around...
        final String[] parts = id.split("/", -1);
        for (final String part : parts) {
            switch (part) {
                case "":
                case ".":
                case "..":
                    throw new IllegalArgumentException();
            }
        }
        final File rootDir;
        switch (parts[0]) {
            case PUBLIC_ROOT_DIR_ID:
                rootDir = publicRootDir;
                break;
            default:
                throw new FileNotFoundException();
        }
        return FileUtils.getFile(rootDir,
                Arrays.copyOfRange(parts, 1, parts.length));
    }

    private File publicRootDir = null;

    @Override
    public Cursor queryRoots(final String[] projection) throws FileNotFoundException {
        final MatrixCursor result =
                new MatrixCursor(resolveRootProjection(projection));
        if (publicRootEnabled) {
            final MatrixCursor.RowBuilder row = result.newRow();
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, PUBLIC_ROOT_ID);
            row.add(DocumentsContract.Root.COLUMN_FLAGS,
                    DocumentsContract.Root.FLAG_LOCAL_ONLY
                            | DocumentsContract.Root.FLAG_SUPPORTS_CREATE
                            | DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
            row.add(DocumentsContract.Root.COLUMN_TITLE,
                    getContext().getString(R.string.title_document_root_public));
            row.add(DocumentsContract.Root.COLUMN_SUMMARY,
                    getContext().getString(R.string.summary_document_root_public));
            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, PUBLIC_ROOT_DIR_ID);
            row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
                    publicRootDir.getFreeSpace());
            row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        }
        return result;
    }

    private void addFile(@NonNull final MatrixCursor result, @NonNull final String documentId)
            throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        final boolean isRoot = isRoot(documentId);
        int flags = 0;
        if (file.isDirectory()) {
            if (file.canWrite()) {
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
            }
        } else if (file.canWrite()) {
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
        }
        if (!isRoot && file.getParentFile().canWrite()) {
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
        }
        final int iconRes;
        if (!file.exists()) {
            if (!PtyProcess.isSymlink(file.getPath()))
                throw new FileNotFoundException();
            iconRes = R.drawable.ic_help;
        } else {
            iconRes = ResourcesCompat.ID_NULL;
        }
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file));
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        row.add(DocumentsContract.Document.COLUMN_ICON, iconRes);
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.length());
    }

    @Override
    public Cursor queryDocument(final String documentId, final String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result =
                new MatrixCursor(resolveDocumentProjection(projection));
        addFile(result, documentId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(final String parentDocumentId, final String[] projection,
                                      final String sortOrder) throws FileNotFoundException {
        final File dir = getFileForDocId(parentDocumentId);
        final String[] names = dir.list();
        if (names == null)
            throw new FileNotFoundException();
        final MatrixCursor result =
                new MatrixCursor(resolveDocumentProjection(projection));
        for (final String name : names)
            addFile(result, parentDocumentId + '/' + name);
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(final String documentId, final String mode,
                                             @Nullable final CancellationSignal signal)
            throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        final int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    @Override
    public void deleteDocument(final String documentId) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        if (!file.exists())
            throw new FileNotFoundException();
        if (file.isDirectory()) {
            final String[] names = file.list();
            if (names == null)
                throw new FileNotFoundException();
            for (final String name : names) {
                deleteDocument(documentId + '/' + name);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            revokeDocumentPermission(documentId);
        } else {
            getContext().revokeUriPermission(buildDocumentUri(authority, documentId), ~0);
        }
        file.delete();
    }

    @Override
    public String createDocument(final String parentDocumentId,
                                 final String mimeType, final String displayName)
            throws FileNotFoundException {
        if (!isValidFilePathSegment(displayName))
            throw new IllegalArgumentException();
        final String documentId = parentDocumentId + '/' + displayName;
        final File file = getFileForDocId(documentId);
        if (isDirMimeType(mimeType)) {
            file.mkdir();
        } else {
            try {
                file.createNewFile();
                file.setReadable(true);
                file.setWritable(true);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return documentId;
    }

    @Override
    public boolean isChildDocument(final String parentDocumentId, final String documentId) {
        return documentId.startsWith(parentDocumentId + '/');
    }

    private String authority = null;

    @Override
    public void attachInfo(final Context context, final ProviderInfo info) {
        authority = info.authority;
        super.attachInfo(context, info);
    }

    @Override
    public boolean onCreate() {
        publicRootDir = OwnDocumentsManager.getRootFile(getContext());
        publicRootEnabled = OwnDocumentsManager.isEnabled(getContext());
        return true;
    }

    private static boolean publicRootEnabled = false;

    public static boolean isPublicRootEnabled() {
        return publicRootEnabled;
    }

    public static void setPublicRootEnabled(@NonNull final Context context, final boolean v) {
        if (publicRootEnabled == v)
            return;
        publicRootEnabled = v;
        final Context appCtx = context.getApplicationContext();
        final String authority;
        try {
            authority = appCtx.getPackageManager().getProviderInfo(
                    new ComponentName(appCtx, OwnDocumentsProvider.class),
                    0
            ).authority;
        } catch (final PackageManager.NameNotFoundException e) {
            throw new Error(e);
        }
        final Uri rootUri = DocumentsContract.buildRootsUri(authority);
        appCtx.getContentResolver().notifyChange(rootUri, null);
    }
}
