package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.core.math.MathUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownServiceException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.backends.local.LocalModule;
import green_green_avk.anotherterm.backends.usbUart.UsbUartModule;
import green_green_avk.anotherterm.ui.BackendUiShell;
import green_green_avk.anotherterm.utils.BinaryGetOpts;
import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.ChrootedFile;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.anotherterm.utils.PreferenceStorage;
import green_green_avk.anotherterm.utils.SslHelper;
import green_green_avk.anotherterm.utils.XmlToAnsi;
import green_green_avk.anothertermshellpluginutils.Plugin;
import green_green_avk.anothertermshellpluginutils.Protocol;
import green_green_avk.anothertermshellpluginutils.StringContent;
import green_green_avk.ptyprocess.PtyProcess;

public final class TermSh {
    private static final String USER_NOTIFICATION_CHANNEL_ID =
            TermSh.class.getName() + ".user";
    private static final String REQUEST_NOTIFICATION_CHANNEL_ID =
            TermSh.class.getName() + ".request";

    private static File getFileWithCWD(@NonNull final String cwd, @NonNull final String fn) {
        final File f = new File(fn);
        if (f.isAbsolute()) return f;
        return new File(cwd, fn);
    }

    private static void checkFile(@NonNull final File file) throws FileNotFoundException {
        if (!file.exists())
            throw new FileNotFoundException("No such file");
        if (file.isDirectory())
            throw new FileNotFoundException("File is a directory");
    }

    private static final class UiBridge {
        @NonNull
        private final Context ctx;
        @NonNull
        private final Handler handler;

        private final AtomicInteger notificationId = new AtomicInteger(0);

        @UiThread
        private UiBridge(@NonNull final Context context) {
            ctx = context;
            handler = new Handler();
        }

        private void runOnUiThread(@NonNull final Runnable runnable) {
            handler.post(runnable);
        }

        private int getNextNotificationId() {
            return notificationId.getAndIncrement();
        }

        private void postNotification(final String message, final int id) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final Notification n = new NotificationCompat.Builder(
                            ctx.getApplicationContext(), USER_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(message)
                            .setSmallIcon(R.drawable.ic_stat_serv)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .build();
                    NotificationManagerCompat.from(ctx).notify(C.TERMSH_USER_TAG, id, n);
                }
            });
        }

        private void removeNotification(final int id) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NotificationManagerCompat.from(ctx).cancel(C.TERMSH_USER_TAG, id);
                }
            });
        }
    }

    private static final class UiServer implements Runnable {
        private static final BinaryGetOpts.Options CAT_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("insecure", new String[]{"--insecure"},
                                BinaryGetOpts.Option.Type.NONE)
                });
        private static final BinaryGetOpts.Options COPY_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("force", new String[]{"-f", "--force"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("insecure", new String[]{"--insecure"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("from-path", new String[]{"-fp", "--from-path"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("from-uri", new String[]{"-fu", "--from-uri"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("to-path", new String[]{"-tp", "--to-path"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("to-uri", new String[]{"-tu", "--to-uri"},
                                BinaryGetOpts.Option.Type.STRING)
                });
        private static final BinaryGetOpts.Options NOTIFY_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("id", new String[]{"-i", "--id"},
                                BinaryGetOpts.Option.Type.INT),
                        new BinaryGetOpts.Option("remove", new String[]{"-r", "--remove"},
                                BinaryGetOpts.Option.Type.NONE)
                });
        private static final BinaryGetOpts.Options OPEN_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("mime", new String[]{"-m", "--mime"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("notify", new String[]{"-N", "--notify"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("prompt", new String[]{"-p", "--prompt"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("recipient", new String[]{"-r", "--recipient"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("uri", new String[]{"-u", "--uri"},
                                BinaryGetOpts.Option.Type.NONE)
                });
        private static final BinaryGetOpts.Options PICK_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("force", new String[]{"-f", "--force"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("insecure", new String[]{"--insecure"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("mime", new String[]{"-m", "--mime"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("notify", new String[]{"-N", "--notify"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("prompt", new String[]{"-p", "--prompt"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("uri", new String[]{"-u", "--uri"},
                                BinaryGetOpts.Option.Type.NONE)
                });
        private static final BinaryGetOpts.Options PLUGIN_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("help", new String[]{"-h", "--help"},
                                BinaryGetOpts.Option.Type.NONE)
                });
        private static final BinaryGetOpts.Options SEND_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("mime", new String[]{"-m", "--mime"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("name", new String[]{"-n", "--name"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("notify", new String[]{"-N", "--notify"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("prompt", new String[]{"-p", "--prompt"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("size", new String[]{"-s", "--size"},
                                BinaryGetOpts.Option.Type.INT),
                        new BinaryGetOpts.Option("uri", new String[]{"-u", "--uri"},
                                BinaryGetOpts.Option.Type.NONE)
                });
        private static final BinaryGetOpts.Options URI_OPTS =
                new BinaryGetOpts.Options(new BinaryGetOpts.Option[]{
                        new BinaryGetOpts.Option("close", new String[]{"-c", "--close-stream"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("list", new String[]{"-l", "--list-streams"},
                                BinaryGetOpts.Option.Type.NONE),
                        new BinaryGetOpts.Option("mime", new String[]{"-m", "--mime"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("name", new String[]{"-n", "--name"},
                                BinaryGetOpts.Option.Type.STRING),
                        new BinaryGetOpts.Option("size", new String[]{"-s", "--size"},
                                BinaryGetOpts.Option.Type.INT),
                        new BinaryGetOpts.Option("wait", new String[]{"-w", "--wait"},
                                BinaryGetOpts.Option.Type.NONE)
                });

        private final UiBridge ui;

        private UiServer(@NonNull final UiBridge ui) {
            this.ui = ui;
        }

        private static final class ParseException extends RuntimeException {
            private ParseException(final String message) {
                super(message);
            }
        }

        private static final class ArgsException extends RuntimeException {
            private ArgsException(final String message) {
                super(message);
            }
        }

        private static final class ShellSecurityException extends RuntimeException {
            private static final String h = "The operation is not permitted in this session";

            private ShellSecurityException() {
                super(h);
            }

            private ShellSecurityException(final String message) {
                super(h + ": " + message);
            }
        }

        private static final class ShellCmdIO {
            private static final byte CMD_EXIT = 0;
            private static final byte CMD_OPEN = 1;

            private static final int ARGLEN_MAX = 1024 * 1024;
            private static final byte[][] NOARGS = new byte[0][];

            private volatile boolean closed = false;
            private final Object closeLock = new Object();
            private final LocalSocket socket;
            private final InputStream cis;
            @NonNull
            private final InputStream stdIn;
            @NonNull
            private final OutputStream stdOut;
            @NonNull
            private final OutputStream stdErr;
            @NonNull
            private final InputStream ctlIn;
            @NonNull
            private final long shellSessionToken;
            private final LocalModule.SessionData shellSessionData;
            private final String currDir;
            private final byte[][] args;
            private volatile Runnable onTerminate = null;

            private final Thread cth = new Thread("TermShServer.Control") {
                @Override
                public void run() {
                    try {
                        while (true) {
                            final int r = ctlIn.read();
                            if (r < 0) break;
                        }
                    } catch (final IOException e) {
                        Log.e("TermShServer", "Request", e);
                    }
                    close();
                }
            };

            private void setOnTerminate(@Nullable final Runnable onTerminate) {
                synchronized (closeLock) {
                    if (onTerminate != null && closed) onTerminate.run();
                    else this.onTerminate = onTerminate;
                }
            }

            private <T> T waitFor(@NonNull final Future<T> task)
                    throws ExecutionException, InterruptedException {
                this.setOnTerminate(new Runnable() {
                    @Override
                    public void run() {
                        task.cancel(true);
                    }
                });
                try {
                    return task.get();
                } finally {
                    this.setOnTerminate(null);
                }
            }

            private <T> T waitFor(@NonNull final BlockingSync<T> result,
                                  @NonNull final Runnable onTerminate)
                    throws InterruptedException {
                this.setOnTerminate(onTerminate);
                try {
                    return result.get();
                } finally {
                    this.setOnTerminate(null);
                }
            }

            private void close() {
                synchronized (closeLock) {
                    if (closed) return;
                    closed = true;
                    final Runnable ot = onTerminate;
                    if (ot != null) ot.run();
                    try {
                        if (stdIn != null) stdIn.close();
                    } catch (final IOException ignored) {
                    }
                    try {
                        if (stdOut != null) stdOut.close();
                    } catch (final IOException ignored) {
                    }
                    try {
                        if (stdErr != null) stdErr.close();
                    } catch (final IOException ignored) {
                    }
                }
            }

            @NonNull
            private static ParcelFileDescriptor wrapFD(final FileDescriptor fd)
                    throws IOException {
                final ParcelFileDescriptor pfd = ParcelFileDescriptor.dup(fd);
                try {
                    PtyProcess.close(fd);
                } catch (final IOException ignored) {
                }
                return pfd;
            }

            // TODO: better remove fallbacks

            @NonNull
            private static FileInputStream wrapInputFD(final FileDescriptor fd) {
                try {
                    final ParcelFileDescriptor pfd = wrapFD(fd);
                    try {
                        return new PtyProcess.InterruptableFileInputStream(pfd);
                    } catch (final IOException e) {
                        Log.e("TermShServer", "Request", e);
                    }
                    return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                } catch (final IOException e) {
                    Log.e("TermShServer", "Request", e);
                }
                return new FileInputStream(fd);
            }

            @NonNull
            private static FileOutputStream wrapOutputFD(final FileDescriptor fd) {
                try {
                    final ParcelFileDescriptor pfd = wrapFD(fd);
                    return new PtyProcess.PfdFileOutputStream(pfd);
                } catch (final IOException e) {
                    Log.e("TermShServer", "Request", e);
                }
                return new FileOutputStream(fd);
            }

            @NonNull
            private FileDescriptor[] getFds() throws IOException {
                return new FileDescriptor[]{
                        ((FileInputStream) stdIn).getFD(),
                        ((FileOutputStream) stdOut).getFD(),
                        ((FileOutputStream) stdErr).getFD()
                };
            }

            @NonNull
            private static long parseShellSessionToken(@NonNull final InputStream is)
                    throws IOException, ParseException {
                final DataInputStream dis = new DataInputStream(is);
                return dis.readLong();
            }

            @NonNull
            private static String parsePwd(@NonNull final InputStream is)
                    throws IOException, ParseException {
                final DataInputStream dis = new DataInputStream(is);
                final int l = dis.readInt();
                if (l < 0 || l > ARGLEN_MAX) throw new ParseException("Current dir parse error");
                final byte[] buf = new byte[l];
                dis.readFully(buf);
                return Misc.fromUTF8(buf);
            }

            @NonNull
            private static byte[][] parseArgs(@NonNull final InputStream is)
                    throws IOException, ParseException {
                /*
                 * It seems socket_read() contains a bug with exception throwing, see:
                 * https://android.googlesource.com/platform/frameworks/base/+/master/core/jni/android_net_LocalSocketImpl.cpp
                 */
                /*
                Possible W/A:
                final byte[] argc_b = new byte[1];
                final int r = is.read(argc_b);
                final int argc = r != 1 ? -1 : argc_b[0];
                */
                final int argc = is.read();
                if (argc <= 0) return NOARGS;
                final byte[][] args = new byte[argc][];
                final DataInputStream dis = new DataInputStream(is);
                for (int i = 0; i < argc; ++i) {
                    final int l = dis.readInt();
                    if (l < 0 || l > ARGLEN_MAX) throw new ParseException("Arguments parse error");
                    args[i] = new byte[l];
                    dis.readFully(args[i]);
                }
                return args;
            }

            private class ShellErrnoException extends IOException {
                public final int errno;

                public ShellErrnoException(final String message, final int errno) {
                    super(message);
                    this.errno = errno;
                }
            }

            private void exit(final int status) {
                try {
                    socket.getOutputStream().write(new byte[]{CMD_EXIT, (byte) status});
                } catch (final IOException ignored) {
                }
                close();
            }

            @NonNull
            private ParcelFileDescriptor open(@NonNull final String name, final int flags)
                    throws ParseException, IOException {
                final int errno;
                try {
                    final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeByte(CMD_OPEN);
                    dos.writeInt(flags);
                    final byte[] _name = Misc.toUTF8(name);
                    dos.writeInt(_name.length);
                    dos.write(_name);
                    final DataInputStream dis = new DataInputStream(socket.getInputStream());
                    final byte result = dis.readByte();
                    if (result == 0) {
                        final FileDescriptor[] fds = socket.getAncillaryFileDescriptors();
                        if (fds.length != 1)
                            throw new ParseException("Invalid descriptors received");
                        return wrapFD(fds[0]);
                    }
                    errno = dis.readInt();
                } catch (final IOException e) {
                    throw new ParseException(e.getMessage());
                }
                switch (errno) {
                    case PtyProcess.ENOENT:
                        throw new FileNotFoundException(name + ": No such file or directory");
                    default:
                        throw new ShellErrnoException(name + ": open() fails with errno=" + errno, errno);
                }
            }

            private final ChrootedFile.Ops ops = new ChrootedFile.Ops() {
                @Override
                public ParcelFileDescriptor open(final String path, final int flags)
                        throws IOException, ParseException {
                    return ShellCmdIO.this.open(path, flags);
                }
            };

            @NonNull
            private ChrootedFile getOriginal(@NonNull final String name)
                    throws ParseException {
                return new ChrootedFile(ops, name);
            }

            @NonNull
            private File getOriginalFile(@NonNull final String name)
                    throws ParseException, IOException {
                return new File(PtyProcess.getPathByFd(open(name, PtyProcess.O_PATH).getFd()));
            }

            private void checkPerms(final long perms) {
                if (shellSessionData == null ||
                        (shellSessionData.permissions &
                                perms) == 0) {
                    throw new ShellSecurityException();
                }
            }

            private ShellCmdIO(@NonNull final LocalSocket socket)
                    throws IOException, ParseException {
                this.socket = socket;
                cis = socket.getInputStream();
                shellSessionToken = parseShellSessionToken(cis);
                currDir = parsePwd(cis);
                args = parseArgs(cis);
                final FileDescriptor[] ioFds = socket.getAncillaryFileDescriptors();
                if (ioFds == null || ioFds.length != 4) {
                    if (ioFds != null) for (final FileDescriptor fd : ioFds) PtyProcess.close(fd);
                    throw new ParseException("Bad descriptors");
                }
                stdIn = wrapInputFD(ioFds[0]);
                stdOut = wrapOutputFD(ioFds[1]);
                stdErr = wrapOutputFD(ioFds[2]);
                ctlIn = wrapInputFD(ioFds[3]);
                cth.start();

                // Post init
                try {
                    shellSessionData = LocalModule.getSessionData(shellSessionToken);
                } catch (final IllegalArgumentException e) {
                    final String msg = "SHELL_SESSION_TOKEN env var is wrong!";
                    stdErr.write(Misc.toUTF8(msg + "\n"));
                    exit(1);
                    throw new ShellSecurityException(msg);
                }
            }
        }

        @NonNull
        private InputStream openInputStream(@NonNull final Uri uri, final boolean insecure)
                throws IOException {
            final InputStream is;
            final String scheme = uri.getScheme();
            if (scheme == null) throw new MalformedURLException("Malformed URL: " + uri.toString());
            switch (scheme) {
                case "http":
                case "https": {
                    final URL url = new URL(uri.toString());
                    final URLConnection conn = url.openConnection();
                    if (conn instanceof HttpsURLConnection && insecure) {
                        ((HttpsURLConnection) conn)
                                .setSSLSocketFactory(SslHelper.trustAllCertsCtx.getSocketFactory());
                    }
                    try {
                        is = conn.getInputStream();
                    } catch (final UnknownServiceException e) {
                        throw e;
                    } catch (final IOException e) {
                        // fix for bad Android error reporting ;)
                        final String msg = e.getMessage();
                        if (msg == null)
                            throw new IOException("Error getting content from " + uri.toString());
                        if (msg.substring(0, 4).toLowerCase().equals("http"))
                            throw new IOException("Error getting content from " + e.getMessage());
                        throw e;
                    }
                    break;
                }
                default:
                    is = ui.ctx.getContentResolver().openInputStream(uri);
                    if (is == null) {
                        // Asset
                        throw new FileNotFoundException(uri.toString() + " does not exist");
                    }
            }
            return is;
        }

        @NonNull
        private OutputStream openOutputStream(@NonNull final Uri uri) throws FileNotFoundException {
            final OutputStream os = ui.ctx.getContentResolver().openOutputStream(uri);
            if (os == null) {
                // Asset
                throw new FileNotFoundException(uri.toString() + " does not exist");
            }
            return os;
        }

        @Nullable
        private String deduceName(@NonNull final Uri uri) {
            return uri.getLastPathSegment();
        }

        @Nullable
        private String getName(@NonNull final Uri uri) {
            final Cursor c = ui.ctx.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (c == null) return deduceName(uri);
            try {
                c.moveToFirst();
                return c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } catch (final Throwable e) {
                return deduceName(uri);
            } finally {
                c.close();
            }
        }

        private long getSize(@NonNull final Uri uri) {
            final Cursor c = ui.ctx.getContentResolver().query(uri,
                    new String[]{OpenableColumns.SIZE},
                    null, null, null);
            if (c == null) return -1;
            try {
                c.moveToFirst();
                return c.getLong(c.getColumnIndex(OpenableColumns.SIZE));
            } catch (final Throwable e) {
                return -1;
            } finally {
                c.close();
            }
        }

        private void printHelp(@NonNull final String value, @NonNull final OutputStream output)
                throws IOException {
            if (output instanceof PtyProcess.PfdFileOutputStream) {
                printHelp(value, output, ((PtyProcess.PfdFileOutputStream) output).pfd.getFd());
                return;
            }
            throw new IllegalArgumentException("Unsupported stream type");
        }

        private void printHelp(@NonNull final String value, @NonNull final OutputStream output,
                               @NonNull final ShellCmdIO shellCmd) throws IOException {
            // Any of the standard pipes can be redirected; using controlling terminal by itself
            final ParcelFileDescriptor pfd = shellCmd.open("/dev/tty", PtyProcess.O_RDWR);
            try {
                printHelp(value, output, pfd.getFd());
            } finally {
                pfd.close();
            }
        }

        private void printHelp(@NonNull final String value, @NonNull final OutputStream output,
                               final int ctfd) throws IOException {
            final int[] size = new int[4];
            PtyProcess.getSize(ctfd, size);
            try {
                final XmlToAnsi hp = new XmlToAnsi(value);
                hp.width = MathUtils.clamp(size[0], 20, 140);
                hp.indentStep = hp.width / 20;
                for (final String s : hp)
                    output.write(Misc.toUTF8(s));
            } catch (final Throwable e) {
                throw new IOException(ui.ctx.getString(
                        R.string.msg_xml_parse_error_s, e.getLocalizedMessage()));
            }
        }

        private interface RunnableT {
            void run() throws Throwable;
        }

        private void runOnUiThread(@NonNull final ShellCmdIO shellCmd,
                                   @NonNull final RunnableT runnable)
                throws InterruptedException, IOException {
            final BlockingSync<Throwable> result = new BlockingSync<>();
            ui.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (final Throwable e) {
                        result.set(e);
                        return;
                    }
                    result.set(null);
                }
            });
            final Throwable e = shellCmd.waitFor(result, new Runnable() {
                @Override
                public void run() {
                    result.setIfIsNotSet(new IOException("Request terminated"));
                }
            });
            if (e != null) throw new IOException(e.getMessage());
        }

        @SuppressLint("StaticFieldLeak")
        private final class ClientTask extends AsyncTask<Object, Object, Object> {
            private volatile int exitStatus = 0;

            @Override
            protected Object doInBackground(final Object[] objects) {
                final LocalSocket socket = (LocalSocket) objects[0];
                final ShellCmdIO shellCmd;
                try {
                    if (Process.myUid() != socket.getPeerCredentials().getUid())
                        throw new ParseException("Spoofing detected!");
                    shellCmd = new ShellCmdIO(socket);
                } catch (final IOException | ParseException e) {
                    Log.e("TermShServer", "Request", e);
                    try {
                        socket.close();
                    } catch (final IOException ignored) {
                    }
                    return null;
                } catch (final ShellSecurityException e) {
                    Log.e("TermShServer", e.getMessage());
                    return null;
                }
                try {
                    exitStatus = 0;
                    if (shellCmd.args.length < 1) throw new ArgsException("No command specified");
                    final String command = Misc.fromUTF8(shellCmd.args[0]);
                    switch (command) {
                        case "help":
                            printHelp(ui.ctx.getString(R.string.desc_termsh_help),
                                    shellCmd.stdOut, shellCmd);
                            break;
                        case "notify": {
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(NOTIFY_OPTS);
                            final Integer _id = (Integer) opts.get("id");
                            if (opts.containsKey("remove")) {
                                if (_id == null)
                                    throw new ParseException("`id' argument is mandatory");
                                ui.removeNotification(_id);
                                break;
                            }
                            final int id = _id == null ? ui.getNextNotificationId() : _id;
                            final String msg;
                            switch (shellCmd.args.length - ap.position) {
                                case 1:
                                    msg = Misc.fromUTF8(shellCmd.args[ap.position]);
                                    break;
                                case 0: {
                                    final Reader reader =
                                            new InputStreamReader(shellCmd.stdIn, Misc.UTF8);
                                    final CharBuffer buf = CharBuffer.allocate(8192);
                                    String m = "";
                                    while (true) {
                                        ui.postNotification(m, id);
                                        if (reader.read(buf) < 0) break;
                                        if (buf.remaining() < 2) { // TODO: correct
                                            buf.position(buf.limit() / 2);
                                            buf.compact();
                                        }
                                        m = buf.duplicate().flip().toString();
                                    }
                                    msg = m;
                                    break;
                                }
                                default:
                                    throw new ParseException("Bad arguments");
                            }
                            ui.postNotification(msg, id);
                            break;
                        }
                        case "uri": {
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(URI_OPTS);
                            if (opts.containsKey("list")) {
                                for (final Uri uri : StreamProvider.getBoundUriList()) {
                                    shellCmd.stdOut.write(Misc.toUTF8(uri.toString() + "\n"));
                                }
                            } else if (opts.containsKey("close"))
                                switch (shellCmd.args.length - ap.position) {
                                    case 1:
                                        StreamProvider.releaseUri(
                                                Uri.parse(Misc.fromUTF8(
                                                        shellCmd.args[ap.position])));
                                        break;
                                    default:
                                        throw new ParseException("No URI specified");
                                }
                            else
                                switch (shellCmd.args.length - ap.position) {
                                    case 0: {
                                        final BlockingSync<Object> result = new BlockingSync<>();
                                        String mime = (String) opts.get("mime");
                                        if (mime == null) mime = "*/*";
                                        final Uri uri = StreamProvider.obtainUri(shellCmd.stdIn,
                                                mime,
                                                (String) opts.get("name"),
                                                (Integer) opts.get("size"),
                                                new StreamProvider.OnResult() {
                                                    @Override
                                                    public void onResult(final Object msg) {
                                                        result.set(null);
                                                    }
                                                });
                                        shellCmd.stdOut.write(Misc.toUTF8(uri.toString() + "\n"));
                                        if (opts.containsKey("wait")) {
                                            shellCmd.waitFor(result, new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        StreamProvider.releaseUri(uri);
                                                    } finally {
                                                        result.set(null);
                                                    }
                                                }
                                            });
                                        }
                                        break;
                                    }
                                    case 1: {
                                        final String filename = Misc.fromUTF8(shellCmd.args[ap.position]);
                                        final File file = shellCmd.getOriginalFile(filename);
                                        checkFile(file);
                                        final Uri uri;
                                        try {
                                            uri = FileProvider.getUriForFile(ui.ctx,
                                                    BuildConfig.APPLICATION_ID + ".fileprovider",
                                                    file);
                                        } catch (final IllegalArgumentException e) {
                                            throw new FileNotFoundException(e.getMessage());
                                        }
                                        shellCmd.stdOut.write(Misc.toUTF8(uri.toString() + "\n"));
                                        break;
                                    }
                                    default:
                                        throw new ParseException("Wrong number of arguments");
                                }
                            break;
                        }
                        case "view":
                        case "edit": {
                            final boolean writeable = "edit".equals(command);
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(OPEN_OPTS);
                            String mime = (String) opts.get("mime");
                            String prompt = (String) opts.get("prompt");
                            if (prompt == null)
                                prompt = ui.ctx.getString(R.string.msg_pick_application);
                            if (shellCmd.args.length - ap.position == 1) {
                                final String filename =
                                        Misc.fromUTF8(shellCmd.args[ap.position]);
                                final Uri uri;
                                if (opts.containsKey("uri")) {
                                    uri = Uri.parse(filename);
                                } else {
                                    final File file = shellCmd.getOriginalFile(filename);
                                    checkFile(file);
                                    try {
                                        uri = FileProvider.getUriForFile(ui.ctx,
                                                BuildConfig.APPLICATION_ID + ".fileprovider",
                                                file);
                                    } catch (final IllegalArgumentException e) {
                                        throw new FileNotFoundException(e.getMessage());
                                    }
                                }
                                final Intent i = new Intent(writeable ?
                                        Intent.ACTION_EDIT : Intent.ACTION_VIEW);
                                i.setDataAndType(uri, mime);
                                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | (writeable ?
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0));
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                final Intent ci;
                                final String recipient = (String) opts.get("recipient");
                                if (recipient == null) {
                                    ci = Intent.createChooser(i, prompt);
                                    ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                } else {
                                    if (recipient.indexOf('/') < 0)
                                        i.setClassName(ui.ctx.getApplicationContext(), recipient);
                                    else
                                        i.setComponent(ComponentName.unflattenFromString(recipient));
                                    ci = i;
                                }
                                if (opts.containsKey("notify"))
                                    RequesterActivity.showAsNotification(ui.ctx,
                                            ci,
                                            ui.ctx.getString(R.string.title_shell_of_s,
                                                    ui.ctx.getString(R.string.app_name)),
                                            prompt + " (" + filename + ")",
                                            REQUEST_NOTIFICATION_CHANNEL_ID,
                                            NotificationCompat.PRIORITY_HIGH);
                                else
                                    ui.ctx.startActivity(ci);
                            } else {
                                throw new ParseException("Bad arguments");
                            }
                            break;
                        }
                        case "send": {
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(SEND_OPTS);
                            String mime = (String) opts.get("mime");
                            if (mime == null) mime = "*/*";
                            String prompt = (String) opts.get("prompt");
                            if (prompt == null)
                                prompt = ui.ctx.getString(R.string.msg_pick_destination);
                            final String name;
                            final Uri uri;
                            final BlockingSync<Object> result = new BlockingSync<>();
                            switch (shellCmd.args.length - ap.position) {
                                case 1: {
                                    result.set(null);
                                    name = Misc.fromUTF8(shellCmd.args[ap.position]);
                                    if (opts.containsKey("uri")) {
                                        uri = Uri.parse(name);
                                    } else {
                                        final File file = shellCmd.getOriginalFile(name);
                                        checkFile(file);
                                        try {
                                            uri = FileProvider.getUriForFile(ui.ctx,
                                                    BuildConfig.APPLICATION_ID + ".fileprovider",
                                                    file);
                                        } catch (final IllegalArgumentException e) {
                                            throw new FileNotFoundException(e.getMessage());
                                        }
                                    }
                                    break;
                                }
                                case 0: {
                                    name = (String) opts.get("name");
                                    uri = StreamProvider.obtainUri(shellCmd.stdIn, mime,
                                            name, (Integer) opts.get("size"),
                                            new StreamProvider.OnResult() {
                                                @Override
                                                public void onResult(final Object msg) {
                                                    result.set(null);
                                                }
                                            });
                                    break;
                                }
                                default:
                                    throw new ParseException("Bad arguments");
                            }
                            final Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType(mime);
                            i.putExtra(Intent.EXTRA_STREAM, uri);
                            if (opts.containsKey("notify"))
                                RequesterActivity.showAsNotification(ui.ctx,
                                        Intent.createChooser(i, prompt),
                                        ui.ctx.getString(R.string.title_shell_of_s,
                                                ui.ctx.getString(R.string.app_name)),
                                        prompt + " (" + (name == null ? "Stream" : name) + ")",
                                        REQUEST_NOTIFICATION_CHANNEL_ID,
                                        NotificationCompat.PRIORITY_HIGH);
                            else
                                ui.ctx.startActivity(Intent.createChooser(i, prompt)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            shellCmd.waitFor(result, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        StreamProvider.releaseUri(uri);
                                    } finally {
                                        result.set(null);
                                    }
                                }
                            });
                            break;
                        }
                        case "pick": {
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(PICK_OPTS);
                            String mime = (String) opts.get("mime");
                            if (mime == null) mime = "*/*";
                            String prompt = (String) opts.get("prompt");
                            if (prompt == null) prompt = ui.ctx.getString(R.string.msg_pick_source);

                            OutputStream output;
                            final ChrootedFile outputFile;
                            switch (shellCmd.args.length - ap.position) {
                                case 0:
                                    output = shellCmd.stdOut;
                                    outputFile = null;
                                    break;
                                case 1: {
                                    output = null;
                                    final String name = Misc.fromUTF8(shellCmd.args[ap.position]);
                                    final ChrootedFile f = shellCmd.getOriginal(name);
                                    if (f.isDirectory()) {
                                        outputFile = f;
                                    } else if (f.exists()) {
                                        if (!opts.containsKey("force")) {
                                            throw new ParseException("File already exists");
                                        }
                                        outputFile = f;
                                    } else {
                                        final ChrootedFile pf = f.getParent();
                                        if (pf == null || !pf.isDirectory()) {
                                            throw new ParseException("Directory does not exist");
                                        }
                                        outputFile = pf;
                                    }
                                    if (!outputFile.canWrite()) {
                                        throw new ParseException("Directory write access denied");
                                    }
                                    break;
                                }
                                default:
                                    throw new ParseException("Bad arguments");
                            }

                            final BlockingSync<Intent> r = new BlockingSync<>();
                            final Intent i = new Intent(Intent.ACTION_GET_CONTENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE).setType(mime);
                            final RequesterActivity.OnResult onResult =
                                    new RequesterActivity.OnResult() {
                                        @Override
                                        public void onResult(@Nullable final Intent result) {
                                            r.setIfIsNotSet(result);
                                        }
                                    };
                            final RequesterActivity.Request request = opts.containsKey("notify") ?
                                    RequesterActivity.request(
                                            ui.ctx, Intent.createChooser(i, prompt), onResult,
                                            ui.ctx.getString(R.string.title_shell_of_s,
                                                    ui.ctx.getString(R.string.app_name)),
                                            prompt, REQUEST_NOTIFICATION_CHANNEL_ID,
                                            NotificationCompat.PRIORITY_HIGH) :
                                    RequesterActivity.request(
                                            ui.ctx, Intent.createChooser(i, prompt), onResult);
                            final Intent ri = shellCmd.waitFor(r, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        request.cancel();
                                    } finally {
                                        r.setIfIsNotSet(null);
                                    }
                                }
                            });
                            final Uri uri;
                            if (ri == null || (uri = ri.getData()) == null) {
                                shellCmd.exit(1);
                                return null;
                            }
                            if (output == null) {
                                if (outputFile.isDirectory()) {
                                    String filename = getName(uri);
                                    if (filename == null) {
                                        shellCmd.stdErr.write(
                                                Misc.toUTF8("Cannot deduce file name\n"));
                                        filename = C.UNNAMED_FILE_NAME;
                                        exitStatus = 2;
                                    }
                                    final ChrootedFile of = outputFile.getChild(filename);
                                    if (!opts.containsKey("force") && of.isFile())
                                        throw new ParseException("File already exists");
                                    output = new FileOutputStream(of.create().getOriginalFile());
                                } else {
                                    output = new FileOutputStream(outputFile.getOriginalFile());
                                }
                            }
                            if (opts.containsKey("uri")) {
                                output.write(Misc.toUTF8(uri.toString()));
                            } else {
                                final InputStream is = openInputStream(uri,
                                        opts.containsKey("insecure"));
                                try {
                                    Misc.copy(output, is);
                                } finally {
                                    try {
                                        is.close();
                                    } finally {
                                        output.close();
                                    }
                                }
                            }
                            break;
                        }
                        case "copy": {
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(COPY_OPTS);
                            if (shellCmd.args.length - ap.position != 0)
                                throw new ParseException("Wrong number of arguments");
                            final InputStream is;
                            final OutputStream os;
                            String name;
                            Uri fromUri = null;
                            File fromFile = null;
                            if ((name = (String) opts.get("from-uri")) != null) {
                                fromUri = Uri.parse(name);
                                is = openInputStream(fromUri, opts.containsKey("insecure"));
                            } else if ((name = (String) opts.get("from-path")) != null) {
                                fromFile = shellCmd.getOriginalFile(name);
                                is = new FileInputStream(fromFile);
                            } else {
                                is = shellCmd.stdIn;
                            }
                            if ((name = (String) opts.get("to-uri")) != null) {
                                os = openOutputStream(Uri.parse(name));
                            } else if ((name = (String) opts.get("to-path")) != null) {
                                ChrootedFile of = shellCmd.getOriginal(name);
                                if (of.isDirectory()) {
                                    String filename = null;
                                    if (fromUri != null) {
                                        filename = getName(fromUri);
                                    } else if (fromFile != null) {
                                        filename = fromFile.getName();
                                    }
                                    if (filename == null) {
                                        shellCmd.stdErr.write(Misc.toUTF8("Cannot deduce file name\n"));
                                        filename = C.UNNAMED_FILE_NAME;
                                        exitStatus = 2;
                                    }
                                    of = of.getChild(filename);
                                }
                                if (!opts.containsKey("force") && of.isFile())
                                    throw new ParseException("File already exists");
                                os = new FileOutputStream(of.create().getOriginalFile());
                            } else {
                                os = shellCmd.stdOut;
                            }
                            try {
                                Misc.copy(os, is);
                            } finally {
                                try {
                                    is.close();
                                } finally {
                                    os.close();
                                }
                            }
                            break;
                        }
                        case "cat": {
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(CAT_OPTS);
                            if (!PtyProcess.isatty(shellCmd.stdIn) ||
                                    shellCmd.args.length - ap.position < 1) {
                                Misc.copy(shellCmd.stdOut, shellCmd.stdIn);
                            }
                            for (int i = ap.position; i < shellCmd.args.length; ++i) {
                                final Uri uri = Uri.parse(Misc.fromUTF8(shellCmd.args[i]));
                                final InputStream is = openInputStream(uri,
                                        opts.containsKey("insecure"));
                                try {
                                    Misc.copy(shellCmd.stdOut, is);
                                } finally {
                                    is.close();
                                }
                            }
                            break;
                        }
                        case "name": {
                            if (shellCmd.args.length != 2)
                                throw new ParseException("Wrong number of arguments");
                            final Uri uri = Uri.parse(Misc.fromUTF8(shellCmd.args[1]));
                            final String name = getName(uri);
                            if (name == null) {
                                shellCmd.stdOut.write(Misc.toUTF8(C.UNNAMED_FILE_NAME + "\n"));
                                exitStatus = 2;
                            } else
                                shellCmd.stdOut.write(Misc.toUTF8(name + "\n"));
                            break;
                        }
                        case "size": {
                            if (shellCmd.args.length != 2)
                                throw new ParseException("Wrong number of arguments");
                            final Uri uri = Uri.parse(Misc.fromUTF8(shellCmd.args[1]));
                            final long size = getSize(uri);
                            if (size < 0) {
                                shellCmd.stdOut.write(Misc.toUTF8(C.UNDEFINED_FILE_SIZE + "\n"));
                                exitStatus = 2;
                            } else
                                shellCmd.stdOut.write(Misc.toUTF8(size + "\n"));
                            break;
                        }
                        case "serial": {
                            if (shellCmd.args.length > 2)
                                throw new ParseException("Wrong number of arguments");
                            final Map<String, ?> params;
                            try {
                                params = shellCmd.args.length == 2
                                        ? UsbUartModule.meta.fromUri(Uri.parse(
                                        "uart:/" + Misc.fromUTF8(shellCmd.args[1])))
                                        : null;
                            } catch (final BackendModule.ParametersUriParseException e) {
                                throw new ArgsException(e.getMessage());
                            }
                            final BackendModule be = new UsbUartModule();
                            be.setContext(ui.ctx);
                            be.setOnMessageListener(new BackendModule.OnMessageListener() {
                                @Override
                                public void onMessage(@NonNull final Object msg) {
                                    if (msg instanceof Throwable) {
                                        try {
                                            shellCmd.stdErr.write(Misc.toUTF8(((Throwable) msg)
                                                    .getMessage() + "\n"));
                                        } catch (final IOException ignored) {
                                        }
                                    }
                                }
                            });
                            final BackendUiShell ui = new BackendUiShell();
                            ui.setIO(shellCmd.stdIn, shellCmd.stdOut, shellCmd.stdErr);
                            be.setUi(ui);
                            be.setOutputStream(shellCmd.stdOut);
                            final OutputStream toBe = be.getOutputStream();
                            try {
                                if (params != null) be.setParameters(params);
                                be.connect();
                                final byte[] buf = new byte[8192];
                                try {
                                    while (true) {
                                        final int r = shellCmd.stdIn.read(buf);
                                        if (r < 0) break;
                                        toBe.write(buf, 0, r);
                                    }
                                } catch (final IOException | BackendException e) {
                                    try {
                                        be.disconnect();
                                    } catch (final BackendException ignored) {
                                    }
                                    throw new IOException(e.getMessage());
                                }
                                be.disconnect();
                            } catch (final BackendException e) {
                                throw new IOException(e.getMessage());
                            }
                            break;
                        }
                        case "uri-encode": {
                            switch (shellCmd.args.length) {
                                case 3: {
                                    final String allow = Misc.fromUTF8(shellCmd.args[2]);
                                    final String v = Misc.fromUTF8(shellCmd.args[1]);
                                    shellCmd.stdOut.write(Misc.toUTF8(
                                            Uri.encode(v, allow) + "\n"));
                                    break;
                                }
                                case 2: {
                                    final String v = Misc.fromUTF8(shellCmd.args[1]);
                                    shellCmd.stdOut.write(Misc.toUTF8(
                                            URLEncoder.encode(v, "UTF8") + "\n"));
                                    break;
                                }
                                default:
                                    throw new ParseException("Wrong number of arguments");
                            }
                            break;
                        }
                        case "uri-decode": {
                            if (shellCmd.args.length != 2)
                                throw new ParseException("Wrong number of arguments");
                            final String v = Misc.fromUTF8(shellCmd.args[1]);
                            final String r;
                            try {
                                r = URLDecoder.decode(v, "UTF8");
                            } catch (final IllegalArgumentException e) {
                                throw new ParseException(e.getMessage());
                            }
                            shellCmd.stdOut.write(Misc.toUTF8(r + "\n"));
                            break;
                        }
                        case "has-favorite": {
                            shellCmd.checkPerms(LocalModule.SessionData.PERM_FAVMGMT);
                            if (shellCmd.args.length != 2)
                                throw new ParseException("Wrong number of arguments");
                            final String name = Misc.fromUTF8(shellCmd.args[1]);
                            runOnUiThread(shellCmd, new RunnableT() {
                                @Override
                                public void run() {
                                    if (!FavoritesManager.contains(name)) {
                                        exitStatus = 2;
                                    }
                                }
                            });
                            break;
                        }
                        case "create-shell-favorite": {
                            shellCmd.checkPerms(LocalModule.SessionData.PERM_FAVMGMT);
                            if (shellCmd.args.length != 3)
                                throw new ParseException("Wrong number of arguments");
                            final String name = Misc.fromUTF8(shellCmd.args[1]);
                            final String execute = Misc.fromUTF8(shellCmd.args[2]);
                            runOnUiThread(shellCmd, new RunnableT() {
                                @Override
                                public void run() throws IOException {
                                    if (FavoritesManager.contains(name)) {
                                        shellCmd.stdErr.write(Misc.toUTF8("Favorite `" + name
                                                + "' is already exists\n"));
                                        exitStatus = 2;
                                        return;
                                    }
                                    final PreferenceStorage ps = new PreferenceStorage();
                                    ps.put("type", BackendsList.get(LocalModule.class).typeStr);
                                    ps.put("execute", execute);
                                    FavoritesManager.set(name, ps);
                                }
                            });
                            break;
                        }
                        case "plugin": {
                            shellCmd.checkPerms(LocalModule.SessionData.PERM_PLUGINEXEC);
                            final BinaryGetOpts.Parser ap = new BinaryGetOpts.Parser(shellCmd.args);
                            ap.skip();
                            final Map<String, ?> opts = ap.parse(PLUGIN_OPTS);
                            if (shellCmd.args.length - ap.position < 1)
                                throw new ParseException("Wrong number of arguments");
                            final String pkgName = Misc.fromUTF8(shellCmd.args[ap.position]);
                            final ComponentName cn = Plugin.getComponent(ui.ctx, pkgName);
                            if (cn == null)
                                throw new IOException(ui.ctx.getString(R.string.msg_s_is_not_a_plugin, pkgName));
                            if (!PluginsManager.verify(pkgName))
                                throw new IOException(ui.ctx.getString(
                                        R.string.msg_s_is_not_permitted_to_run, pkgName));
                            final Plugin plugin = Plugin.bind(ui.ctx, cn);
                            if (opts.containsKey("help")) {
                                try {
                                    final StringContent content = plugin.getMetaStringContent(
                                            Protocol.META_KEY_INFO_RES_ID,
                                            Protocol.META_KEY_INFO_RES_TYPE
                                    );
                                    if (content == null) {
                                        printHelp(ui.ctx.getString(R.string.msg_no_info_page),
                                                shellCmd.stdOut, shellCmd);
                                    } else if (content.type == Protocol.STRING_CONTENT_TYPE_XML_AT) {
                                        printHelp(content.text, shellCmd.stdOut, shellCmd);
                                    } else {
                                        shellCmd.stdOut.write(Misc.toUTF8(content.text + "\n"));
                                    }
                                } finally {
                                    plugin.unbind();
                                }
                            } else {
                                try {
                                    shellCmd.setOnTerminate(new Runnable() {
                                        @Override
                                        public void run() {
                                            plugin.signal(Protocol.SIG_FINALIZE);
                                        }
                                    });
                                    exitStatus = plugin.exec(
                                            Arrays.copyOfRange(shellCmd.args,
                                                    ap.position + 1, shellCmd.args.length),
                                            shellCmd.getFds()
                                    );
                                } finally {
                                    shellCmd.setOnTerminate(null);
                                    plugin.unbind();
                                }
                            }
                            break;
                        }
                        default:
                            throw new ParseException("Unknown command");
                    }
                    shellCmd.exit(exitStatus);
                } catch (final InterruptedException | SecurityException | IOException |
                        ParseException | ArgsException | ShellSecurityException |
                        BinaryGetOpts.ParseException | ActivityNotFoundException e) {
                    try {
                        if (e instanceof ArgsException) {
                            printHelp(ui.ctx.getString(R.string.desc_termsh_help),
                                    shellCmd.stdErr, shellCmd);
                        }
                        shellCmd.stdErr.write(Misc.toUTF8(e.getMessage() + "\n"));
                        shellCmd.exit(1);
                    } catch (final IOException ignored) {
                    }
                }
                return null;
            }
        }

        @Override
        public void run() {
            LocalServerSocket serverSocket = null;
            try {
                serverSocket = new LocalServerSocket(ui.ctx.getPackageName() + ".termsh");
                while (!Thread.interrupted()) {
                    final LocalSocket socket = serverSocket.accept();
                    ui.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, socket);
                        }
                    });
                }
            } catch (final InterruptedIOException ignored) {
            } catch (final IOException e) {
                Log.e("TermShServer", "IO", e);
            }
            if (serverSocket != null)
                try {
                    serverSocket.close();
                } catch (final IOException ignored) {
                }
        }
    }

    private final UiBridge ui;
    private final UiServer uiServer;
    private final Thread lth;

    @UiThread
    public TermSh(@NonNull final Context context) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(
                    USER_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.title_shell_of_s,
                            context.getString(R.string.app_name)),
                    NotificationManager.IMPORTANCE_HIGH
            ));
            nm.createNotificationChannel(new NotificationChannel(
                    REQUEST_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.title_shell_script_request_of_s,
                            context.getString(R.string.app_name)),
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }

        ui = new UiBridge(context);
        uiServer = new UiServer(ui);
        lth = new Thread(uiServer, "TermShServer");
        lth.setDaemon(true);
        lth.start();
    }

    @Override
    protected void finalize() throws Throwable {
        lth.interrupt();
        super.finalize();
    }
}
