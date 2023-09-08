package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.FileObserver;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.io.File;

import green_green_avk.anotherterm.ui.FontProvider;
import green_green_avk.ptyprocess.PtyProcess;

public final class FontsManager {
    private FontsManager() {
    }

    private static final String CONSOLE_FONT_DIRNAME = "console-font";
    private static final String REGULAR_FONT_NAME = "regular";
    private static final String BOLD_FONT_NAME = "bold";
    private static final String ITALIC_FONT_NAME = "italic";
    private static final String BOLD_ITALIC_FONT_NAME = "bold-italic";

    public static final CharSequence LOCATION_DESC = "$DATA_DIR/" + CONSOLE_FONT_DIRNAME + "/{" +
            REGULAR_FONT_NAME + "," + BOLD_FONT_NAME + "," + ITALIC_FONT_NAME + "," +
            BOLD_ITALIC_FONT_NAME + "}";

    private static File dataDir = null;
    private static File consoleFontDir = null;
    private static File[] consoleFontFiles = null;

    @SuppressLint("StaticFieldLeak")
    private static Context ctx = null;

    public static final Typeface[] defaultTypefaces = {
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
    };

    @Size(4)
    @NonNull
    public static Typeface[] defaultConsoleTypefaces = defaultTypefaces;
    @Size(4)
    @NonNull
    public static Typeface[] consoleTypefaces = defaultTypefaces;

    private static boolean trackFontFiles = false;
    private static FontDirObserver dataDirObserver = null;
    private static FontDirObserver consoleFontDirObserver = null;

    private static final class FontDirObserver extends FileObserver {
        @NonNull
        private final File target;

        public FontDirObserver(@NonNull final File file) {
            this(file,
                    FileObserver.ATTRIB | FileObserver.DELETE_SELF | FileObserver.MOVE_SELF |
                            FileObserver.CREATE | FileObserver.DELETE |
                            FileObserver.MOVED_FROM | FileObserver.MOVED_TO |
                            FileObserver.CLOSE_WRITE
            );
        }

        public FontDirObserver(@NonNull final File file, final int mask) {
            super(file.getPath(), mask);
            target = file;
        }

        @Override
        public void onEvent(final int event, @Nullable final String path) {
            if ((event & FileObserver.ALL_EVENTS) != 0) {
                final File file = path != null ? new File(target, path) : target;
                if (consoleFontDir.equals(file) || consoleFontDir.equals(file.getParentFile())) {
                    mainHandler.removeCallbacks(refreshFromFs);
                    mainHandler.post(refreshFromFs);
                }
            }
        }
    }

    private static final Runnable refreshFromFs = () -> {
        try {
            if (!trackFontFiles)
                return;
            if (!consoleFontDir.isDirectory()) {
                dataDirObserver.startWatching();
                consoleFontDirObserver.stopWatching();
                consoleTypefaces = defaultConsoleTypefaces;
                consoleFontDir.mkdirs();
                return;
            }
            consoleFontDirObserver.startWatching();
            dataDirObserver.stopWatching();
            final Typeface[] tfs = loadFromFiles(consoleFontFiles);
            if (tfs[0] == null) {
                consoleTypefaces = defaultConsoleTypefaces;
                return;
            }
            consoleTypefaces = tfs;
        } catch (final SecurityException ignored) {
        }
    };

    private static Handler mainHandler = null;

    public static void init(@NonNull final Context ctx) {
        FontsManager.ctx = ctx.getApplicationContext();
        defaultConsoleTypefaces = loadFromAssets("DejaVuSansMono", ".ttf");
        mainHandler = new Handler(ctx.getMainLooper());
        dataDir = new File(ctx.getApplicationInfo().dataDir);
        consoleFontDir = new File(dataDir, CONSOLE_FONT_DIRNAME);
        consoleFontFiles = new File[]{
                new File(consoleFontDir, REGULAR_FONT_NAME),
                new File(consoleFontDir, BOLD_FONT_NAME),
                new File(consoleFontDir, ITALIC_FONT_NAME),
                new File(consoleFontDir, BOLD_ITALIC_FONT_NAME)
        };
        dataDirObserver = new FontDirObserver(dataDir);
        consoleFontDirObserver = new FontDirObserver(consoleFontDir);
    }

    private static void setFromDefaultAsset() {
        consoleTypefaces = defaultConsoleTypefaces;
    }

    public static void setFrom(final boolean fontDir) {
        if (fontDir) {
            trackFontFiles = true;
            refreshFromFs.run();
            if (consoleTypefaces == defaultTypefaces)
                setFromDefaultAsset();
        } else {
            trackFontFiles = false;
            dataDirObserver.stopWatching();
            consoleFontDirObserver.stopWatching();
            setFromDefaultAsset();
        }
    }

    private static void fillFallbacks(@Size(4) @NonNull final Typeface[] tfs) {
        if (tfs[0] == null)
            return;
        if (tfs[1] == null)
            tfs[1] = tfs[0];
        if (tfs[2] == null)
            tfs[2] = tfs[0];
        if (tfs[3] == null)
            tfs[3] = tfs[2] != tfs[0] ? tfs[2] : tfs[1];
    }

    @Nullable
    private static Typeface loadFromAsset(@NonNull final String path) {
        final AssetManager am = ctx.getApplicationContext().getAssets();
        try {
            final Typeface r = Typeface.createFromAsset(am, path);
            return r != Typeface.DEFAULT ? r : null;
        } catch (final Exception ignored) {
        }
        return null;
    }

    public static void loadFromAssets(@Size(4) @NonNull final Typeface[] tfs,
                                      @NonNull final String name, @NonNull final String ext) {
        final String[] tns = {"-Regular", "-Bold", "-Italic", "-BoldItalic"};
        for (int i = 0; i < 4; ++i) {
            tfs[i] = loadFromAsset("fonts/" + name + tns[i] + ext);
        }
        fillFallbacks(tfs);
    }

    @NonNull
    public static Typeface[] loadFromAssets(@NonNull final String name, @NonNull final String ext) {
        final Typeface[] tfs = new Typeface[4];
        loadFromAssets(tfs, name, ext);
        return tfs;
    }

    @Nullable
    private static Typeface loadFromFile(@NonNull final File file) {
        if (file.isFile() && file.canRead()) {
            try {
                final Typeface r = Typeface.createFromFile(file);
                return r != Typeface.DEFAULT ? r : null;
            } catch (final Exception ignored) {
            }
        }
        return null;
    }

    public static void loadFromFiles(@Size(4) @NonNull final Typeface[] tfs,
                                     @Size(4) @NonNull final File[] files) {
        for (int i = 0; i < 4; ++i) {
            tfs[i] = loadFromFile(files[i]);
        }
        fillFallbacks(tfs);
    }

    @NonNull
    public static Typeface[] loadFromFiles(@Size(4) @NonNull final File[] files) {
        final Typeface[] tfs = new Typeface[4];
        loadFromFiles(tfs, files);
        return tfs;
    }

    public static boolean exists(@Size(4) @NonNull final Typeface[] tfs,
                                 @FontProvider.Style final int style) {
        if (tfs[style] == null)
            return false;
        if (style > 0) {
            for (int i = style - 1; i >= 0; i--)
                if (tfs[style] == tfs[i])
                    return false;
        }
        return true;
    }

    public static void populatePaint(@NonNull final Paint out,
                                     @Size(4) @NonNull final Typeface[] tfs,
                                     @FontProvider.Style final int style) {
        out.setFakeBoldText(false);
        out.setTextSkewX(0);
        Typeface tf = tfs[style];
        if (tf == null)
            tf = FontsManager.defaultConsoleTypefaces[style];
        if (tf == null)
            tf = FontsManager.defaultTypefaces[style];
        switch (style) {
            case Typeface.NORMAL:
                out.setTypeface(tf);
                break;
            case Typeface.BOLD:
                out.setTypeface(tf);
                if (tf == tfs[0] || !tf.isBold())
                    out.setFakeBoldText(true);
                break;
            case Typeface.ITALIC:
                out.setTypeface(tf);
                if (tf == tfs[0] || !tf.isItalic())
                    out.setTextSkewX(-0.25f);
                break;
            case Typeface.BOLD_ITALIC:
                out.setTypeface(tf);
                if (tf == tfs[2] || !tf.isBold())
                    out.setFakeBoldText(true);
                if (tf == tfs[1] || !tf.isItalic())
                    out.setTextSkewX(-0.25f);
                break;
        }
    }

    public static File[] getConsoleFontFiles() {
        return consoleFontFiles;
    }

    private static void delete(@NonNull final File f) {
        if (!f.exists())
            return;
        if (f.isDirectory() && !PtyProcess.isSymlink(f.getPath())) {
            f.setExecutable(true);
            f.setWritable(true);
            final File[] ff = f.listFiles();
            if (ff != null)
                for (final File fe : ff)
                    delete(fe);
        }
        f.delete();
    }

    public static boolean prepareConsoleFontDir(final boolean force) {
        try {
            if (consoleFontDir.isDirectory() && consoleFontDir.canWrite())
                return true;
            if (force) {
                delete(consoleFontDir);
            }
            return consoleFontDir.mkdirs();
        } catch (final SecurityException e) {
            return false;
        }
    }
}
