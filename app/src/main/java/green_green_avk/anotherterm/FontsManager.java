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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import green_green_avk.ptyprocess.PtyProcess;

public final class FontsManager {
    private FontsManager() {
    }

    private static final String CONSOLE_FONT_DIRNAME = "console-font";
    private static final String REGULAR_FONT_NAME = "regular";
    private static final String BOLD_FONT_NAME = "bold";
    private static final String ITALIC_FONT_NAME = "italic";
    private static final String BOLD_ITALIC_FONT_NAME = "bold-italic";

    public static final String LOCATION_DESC = "$DATA_DIR/" + CONSOLE_FONT_DIRNAME + "/{" +
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

    public static Typeface[] defaultConsoleTypefaces = defaultTypefaces;
    public static Typeface[] consoleTypefaces = defaultTypefaces;

    private static boolean trackFontFiles = false;
    private static final List<FontFileObserver> fontFileObservers = new ArrayList<>(6);

    private static final class FontFileObserver extends FileObserver {
        public FontFileObserver(final String path) {
            super(path);
        }

        public FontFileObserver(final String path, final int mask) {
            super(path, mask);
        }

        @Override
        public void onEvent(final int event, @Nullable final String path) {
            if ((event & FileObserver.ALL_EVENTS) != 0) {
                mainHandler.removeCallbacks(refreshFromFs);
                mainHandler.post(refreshFromFs);
            }
        }
    }

    private static void clearFontFileObservers() {
        for (final FontFileObserver fo : fontFileObservers) fo.stopWatching();
        fontFileObservers.clear();
    }

    private static void addFontFileObserver(@NonNull final File f) {
        final FontFileObserver fo = new FontFileObserver(f.getPath(),
                FileObserver.ATTRIB |
                        (f.isDirectory() ?
                                FileObserver.CREATE | FileObserver.DELETE |
                                        FileObserver.MOVED_FROM | FileObserver.MOVED_TO :
                                FileObserver.CLOSE_WRITE
                        )
        );
        fontFileObservers.add(fo);
        fo.startWatching();
    }

    private static final Runnable refreshFromFs = () -> {
        try {
            clearFontFileObservers();
            if (!trackFontFiles) return;
            addFontFileObserver(dataDir);
            consoleFontDir.mkdirs();
            if (!consoleFontDir.isDirectory()) {
                consoleTypefaces = defaultConsoleTypefaces;
                return;
            }
            addFontFileObserver(consoleFontDir);
            for (final File f : consoleFontFiles)
                addFontFileObserver(f);
            final Typeface[] tfs = loadFromFilesFb(consoleFontFiles);
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
        defaultConsoleTypefaces = loadFromAsset("DejaVuSansMono", ".ttf");
        mainHandler = new Handler(ctx.getMainLooper());
        dataDir = new File(ctx.getApplicationInfo().dataDir);
        consoleFontDir = new File(dataDir, CONSOLE_FONT_DIRNAME);
        consoleFontFiles = new File[]{
                new File(consoleFontDir, REGULAR_FONT_NAME),
                new File(consoleFontDir, BOLD_FONT_NAME),
                new File(consoleFontDir, ITALIC_FONT_NAME),
                new File(consoleFontDir, BOLD_ITALIC_FONT_NAME)
        };
    }

    private static void setFromDefaultAsset() {
        consoleTypefaces = defaultConsoleTypefaces;
    }

    public static void setFrom(final boolean fontDir) {
        if (fontDir) {
            trackFontFiles = true;
            refreshFromFs.run();
            if (consoleTypefaces == defaultTypefaces) setFromDefaultAsset();
        } else {
            trackFontFiles = false;
            clearFontFileObservers();
            setFromDefaultAsset();
        }
    }

    public static void loadFromAsset(@NonNull final Typeface[] tfs,
                                     @NonNull final String name, @NonNull final String ext) {
        final AssetManager am = ctx.getApplicationContext().getAssets();
        final String[] tns = {"-Regular", "-Bold", "-Italic", "-BoldItalic"};
        for (int i = 0; i < 4; ++i) {
            tfs[i] = Typeface.createFromAsset(am, "fonts/" + name + tns[i] + ext);
        }
    }

    @NonNull
    public static Typeface[] loadFromAsset(@NonNull final String name, @NonNull final String ext) {
        final Typeface[] tfs = new Typeface[4];
        loadFromAsset(tfs, name, ext);
        return tfs;
    }

    private static void loadFromFiles(@NonNull final Typeface[] tfs, @NonNull final File[] files)
            throws IOException {
        for (int i = 0; i < 4; ++i) {
            try {
                tfs[i] = Typeface.createFromFile(files[i]);
            } catch (final RuntimeException e) {
                throw new FileNotFoundException(e.getMessage());
            }
        }
    }

    @NonNull
    private static Typeface[] loadFromFiles(@NonNull final File[] files)
            throws IOException {
        final Typeface[] tfs = new Typeface[4];
        loadFromFiles(tfs, files);
        return tfs;
    }

    @Nullable
    private static Typeface loadFromFile(@NonNull final File file) {
        if (file.isFile() && file.canRead())
            try {
                return Typeface.createFromFile(file);
            } catch (final RuntimeException ignored) {
            }
        return null;
    }

    public static void loadFromFilesFb(@NonNull final Typeface[] tfs, @NonNull final File[] files) {
        for (int i = 0; i < 4; ++i) tfs[i] = loadFromFile(files[i]);
        if (tfs[0] == null) return;
        if (tfs[1] == null) tfs[1] = tfs[0];
        if (tfs[2] == null) tfs[2] = tfs[0];
        if (tfs[3] == null) tfs[3] = tfs[2] != tfs[0] ? tfs[2] : tfs[1];
    }

    @NonNull
    public static Typeface[] loadFromFilesFb(@NonNull final File[] files) {
        final Typeface[] tfs = new Typeface[4];
        loadFromFilesFb(tfs, files);
        return tfs;
    }

    public static boolean isExists(@NonNull final Typeface[] tfs, final int style) {
        if (tfs[style] == null) return false;
        if (style > 0) {
            int i = style - 1;
            while (i >= 0) {
                if (tfs[style] == tfs[i]) return false;
                i--;
            }
        }
        return true;
    }

    public static void setPaint(@NonNull final Paint paint, @NonNull final Typeface[] tfs,
                                final int style) {
        paint.setFakeBoldText(false);
        paint.setTextSkewX(0);
        Typeface tf = tfs[style];
        if (tf == null) tf = FontsManager.defaultConsoleTypefaces[style];
        if (tf == null) tf = FontsManager.defaultTypefaces[style];
        switch (style) {
            case Typeface.NORMAL:
                paint.setTypeface(tf);
                break;
            case Typeface.BOLD:
                paint.setTypeface(tf);
                if (tf == tfs[0] || !tf.isBold()) paint.setFakeBoldText(true);
                break;
            case Typeface.ITALIC:
                paint.setTypeface(tf);
                if (tf == tfs[0] || !tf.isItalic()) paint.setTextSkewX(-0.25F);
                break;
            case Typeface.BOLD_ITALIC:
                paint.setTypeface(tf);
                if (tf == tfs[2] || !tf.isBold()) paint.setFakeBoldText(true);
                if (tf == tfs[1] || !tf.isItalic()) paint.setTextSkewX(-0.25F);
                break;
        }
    }

    public static File[] getConsoleFontFiles() {
        return consoleFontFiles;
    }

    private static void delete(@NonNull final File f) {
        if (!f.exists()) return;
        if (f.isDirectory() && !PtyProcess.isSymlink(f.getPath())) {
            f.setExecutable(true);
            f.setWritable(true);
            final File[] ff = f.listFiles();
            if (ff != null) for (final File fe : ff) delete(fe);
        }
        f.delete();
    }

    public static boolean prepareConsoleFontDir(final boolean force) {
        try {
            if (consoleFontDir.isDirectory() && consoleFontDir.canWrite()) return true;
            if (force) {
                delete(consoleFontDir);
            }
            return consoleFontDir.mkdirs();
        } catch (final SecurityException e) {
            return false;
        }
    }
}
