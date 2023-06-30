package green_green_avk.anotherterm.wlterm;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Region;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.apache.commons.collections4.map.AbstractReferenceMap;
import org.apache.commons.collections4.map.ReferenceMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.ConsoleService;
import green_green_avk.anotherterm.GraphicsCompositor;
import green_green_avk.anotherterm.wlterm.protocol.wl_own_helper;
import green_green_avk.ptyprocess.PtyProcess;
import green_green_avk.wayland.os.WlEventHandler;
import green_green_avk.wayland.os.WlMmap;
import green_green_avk.wayland.os.WlSocket;
import green_green_avk.wayland.protocol.wayland.wl_buffer;
import green_green_avk.wayland.protocol.wayland.wl_compositor;
import green_green_avk.wayland.protocol.wayland.wl_display;
import green_green_avk.wayland.protocol.wayland.wl_keyboard;
import green_green_avk.wayland.protocol.wayland.wl_output;
import green_green_avk.wayland.protocol.wayland.wl_pointer;
import green_green_avk.wayland.protocol.wayland.wl_region;
import green_green_avk.wayland.protocol.wayland.wl_seat;
import green_green_avk.wayland.protocol.wayland.wl_shell;
import green_green_avk.wayland.protocol.wayland.wl_shell_surface;
import green_green_avk.wayland.protocol.wayland.wl_shm;
import green_green_avk.wayland.protocol.wayland.wl_surface;
import green_green_avk.wayland.protocol.wayland.wl_touch;
import green_green_avk.wayland.protocol.xdg_shell.xdg_positioner;
import green_green_avk.wayland.protocol.xdg_shell.xdg_surface;
import green_green_avk.wayland.protocol.xdg_shell.xdg_toplevel;
import green_green_avk.wayland.protocol.xdg_shell.xdg_wm_base;
import green_green_avk.wayland.protocol_core.WlInterface;
import green_green_avk.wayland.protocol_core.WlMarshalling;
import green_green_avk.wayland.server.WlBuffer;
import green_green_avk.wayland.server.WlClient;
import green_green_avk.wayland.server.WlDisplay;
import green_green_avk.wayland.server.WlGlobal;
import green_green_avk.wayland.server.WlResource;
import green_green_avk.wayland.server.WlShm;

public final class WlTermServer {
    private static final String TAG = "WlTermServer";

    private interface IORunnable {
        void run() throws IOException;
    }

    /**
     * Embedded custom protocol representation.
     */
    private static final class WlOwnCustomProtocolException extends WlOwnCustomException {
        @NonNull
        private final IORunnable handler;

        private WlOwnCustomProtocolException(@NonNull final IORunnable handler) {
            super();
            this.handler = handler;
        }
    }

    @NonNull
    private final Context ctx;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final HandlerThread wlThread;
    @NonNull
    private final Handler wlHandler;
    private final WlDisplay wlDisplay = new WlDisplay();

    private final Map<Long, Integer> termSessions = new HashMap<>();

    /**
     * @param uuid a helper UUID
     * @return an associated session key
     * @throws IndexOutOfBoundsException if no sessions are associated
     */
    @UiThread
    public int getSessionKeyByHelperUuid(final long uuid) {
        final Integer sid = termSessions.get(uuid);
        if (sid != null)
            return sid;
        else
            throw new IndexOutOfBoundsException(
                    "No session for " + Long.toHexString(uuid) + " exists");
    }

    private static final class ConnectionState {
        private int sessionKey = ConsoleService.INVALID_SESSION;
        private boolean isRunning = false;
    }

    private interface WlSocketImpl extends WlSocket {
        void shutdownAndClose();
    }

    private final class ClientTask extends AsyncTask<Object, Object, Object> {
        private final ConnectionState state = new ConnectionState();

        private void initNewSession(@NonNull final WlClientImpl wlClient) {
            final long uuid = wlClient.wlOwnHelper.uuid;
            wlClient.removeResource(WlClientImpl.TWEAK_ID); // Be ninja
            final GraphicsCompositor compositor = new GraphicsCompositor();
            compositor.source = new GraphicsCompositor.Source() {
                @Override
                @UiThread
                public void onStop() {
                    termSessions.remove(uuid);
                    state.isRunning = false;
                    wlHandler.post(() -> {
                        ((WlSocketImpl) wlClient.socket).shutdownAndClose();
                        wlClient.wlOwnHelper.remove();
                    });
                }

                @Override
                public void onResize(final int width, final int height) {
                    wlHandler.post(() -> {
                        if (wlClient.wlOutputRes != null)
                            wlClient.wlOutputRes.onResize(width, height);
                    });
                }
            };
            wlClient.compositor = compositor;
            uiHandler.post(() -> {
                state.sessionKey = ConsoleService.startGraphicsSession(ctx, compositor);
                state.isRunning = true;
                termSessions.put(uuid, state.sessionKey);
            });
        }

        volatile IORunnable customHandler = null;

        @Override
        @Nullable
        protected Object doInBackground(@NonNull final Object[] objects) {
            final LocalSocket socket = (LocalSocket) objects[0];
            try {
                if (Process.myUid() != socket.getPeerCredentials().getUid())
                    throw new IOException("Spoofing detected!");
            } catch (final IOException e) {
                Log.e(TAG, "Oops!", e);
                try {
                    socket.close();
                } catch (final IOException ignored) {
                }
                return null;
            }
            final WlSocket wlSocket = new WlSocketImpl() {
                @Override
                @NonNull
                public InputStream getInputStream() throws IOException {
                    return socket.getInputStream();
                }

                private OutputStream osw = null;

                @Override
                @NonNull
                public OutputStream getOutputStream() throws IOException {
                    if (osw == null)
                        osw = new OutputStream() {
                            private final OutputStream os = socket.getOutputStream();

                            @Override
                            public void write(final int b) throws IOException {
                                os.write(b);
                                socket.setFileDescriptorsForSend(null); // Bugs, sweet bugs...
                            }

                            @Override
                            public void write(final byte[] b) throws IOException {
                                os.write(b);
                                socket.setFileDescriptorsForSend(null); // Bugs, sweet bugs...
                            }

                            @Override
                            public void write(final byte[] b, final int off, final int len)
                                    throws IOException {
                                os.write(b, off, len);
                                socket.setFileDescriptorsForSend(null); // Bugs, sweet bugs...
                            }

                            @Override
                            public void flush() throws IOException {
//                                os.flush();
                            }

                            @Override
                            public void close() throws IOException {
                                os.close();
                            }
                        };
                    return osw;
                }

                @Override
                @Nullable
                public FileDescriptor[] getAncillaryFileDescriptors() throws IOException {
                    return socket.getAncillaryFileDescriptors();
                }

                @Override
                public void setFileDescriptorsForSend(@Nullable final FileDescriptor[] fds) {
                    if (fds.length <= 0)
                        socket.setFileDescriptorsForSend(null);
                    else
                        socket.setFileDescriptorsForSend(fds);
                }

                @Override
                public void shutdownAndClose() {
                    try {
                        PtyProcess.shutdown(socket.getFileDescriptor(),
                                PtyProcess.SHUT_RDWR);
                        socket.close(); // TODO: correct
                    } catch (final IOException ignored) {
                    }
                }
            };
            final InputStream is;
            final WlClientImpl wlClient;
            try {
                is = wlSocket.getInputStream();
            } catch (final IOException e) {
                Log.e(TAG, "Something is very broken...", e);
                try {
                    socket.close();
                } catch (final IOException ignored) {
                }
                return null;
            }
            wlClient = new WlClientImpl(wlDisplay, state,
                    wlSocket, null);
            wlHandler.post(wlClient::init);
            try {
                final Queue<FileDescriptor> fdsQueue = new ConcurrentLinkedQueue<>();
                final Semaphore lock = new Semaphore(1, true);
                while (true) {
                    lock.acquire();
                    final IORunnable ch = customHandler;
                    if (ch != null)
                        ch.run();
                    final ByteBuffer msg = WlMarshalling.readRPC(is);
                    final FileDescriptor[] fds = wlSocket.getAncillaryFileDescriptors();
                    if (fds != null)
                        Collections.addAll(fdsQueue, fds);
                    final boolean r = wlHandler.post(() -> {
                        try {
                            final WlMarshalling.Call call =
                                    WlMarshalling.unmakeRPC(wlClient.resources,
                                            msg, fdsQueue);
                            if (wlClient.compositor == null && call.object instanceof wl_display)
                                initNewSession(wlClient); // Actual session start
//                            if (BuildConfig.DEBUG)
//                                Log.i(TAG, "call: " + call.object.id +
//                                        "[" + call.object.getClass().getSimpleName() + "]" +
//                                        "." + call.method.getName());
                            try {
                                call.call();
                            } catch (final WlOwnCustomProtocolException e) {
                                customHandler = e.handler;
                            } catch (final Exception e) {
                                if (BuildConfig.DEBUG)
                                    Log.w(TAG, e.getMessage() != null ? e.getMessage()
                                            : "???");
                                wlClient.returnError(call.object, e);
                            }
                        } catch (final WlMarshalling.ParseException e) {
                            if (BuildConfig.DEBUG)
                                Log.w(TAG, e.getMessage() != null ? e.getMessage()
                                        : "???");
                            wlClient.returnError(e);
                        } finally {
                            lock.release();
                        }
                    });
                    if (!r)
                        throw new EOFException();
                }
            } catch (final EOFException | InterruptedIOException | InterruptedException e) {
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "EOF/INT");
            } catch (final IOException e) {
                if (BuildConfig.DEBUG)
                    Log.w(TAG, e.getMessage() != null ? e.getMessage() : "-");
                wlHandler.post(() -> wlClient.returnError(e));
            } catch (final WlMarshalling.ParseException e) {
                if (BuildConfig.DEBUG)
                    Log.w(TAG, e.getMessage() != null ? e.getMessage() : "-");
                wlHandler.post(() -> wlClient.returnError(e));
            } finally {
                uiHandler.post(() -> {
                    if (state.isRunning) {
                        ConsoleService.stopSession(state.sessionKey);
                    }
                });
                wlHandler.post(wlClient::destroy);
                try {
                    socket.close();
                } catch (final IOException ignored) {
                }
            }
            return null;
        }
    }

    private final class SocketListener implements Runnable {
        @Override
        public void run() {
            LocalServerSocket serverSocket = null;
            try {
                serverSocket = new LocalServerSocket(BuildConfig.APPLICATION_ID + ".wlterm");
                while (!Thread.interrupted()) {
                    final LocalSocket socket = serverSocket.accept();
                    uiHandler.post(() -> new ClientTask()
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, socket));
                }
            } catch (final InterruptedIOException ignored) {
            } catch (final IOException e) {
                Log.e(TAG, "IO", e);
            } finally {
                if (serverSocket != null)
                    try {
                        serverSocket.close();
                    } catch (final IOException ignored) {
                    }
            }
        }
    }

    private final SocketListener socketListener;
    private final Thread lth;

    private final WlMmap wlMmapImpl = new WlMmap() {
        @Override
        @NonNull
        public ByteBuffer mmap(@NonNull final FileDescriptor fd, final int size)
                throws IOException {
            return PtyProcess.asByteBuffer(PtyProcess.mmap(0, size,
                    PtyProcess.PROT_READ, PtyProcess.MAP_SHARED,
                    fd, 0), size);
        }

        @Override
        public void munmap(@NonNull final ByteBuffer mem) {
            try {
                PtyProcess.munmap(PtyProcess.getAddress(mem), mem.capacity());
            } catch (final IOException ignored) {
            }
        }

        @Override
        public void close(@NonNull final FileDescriptor fd) {
            try {
                PtyProcess.close(fd);
            } catch (final IOException ignored) {
            }
        }
    };

    private static void preWlTransform(@NonNull final Matrix target,
                                       final int width, final int height,
                                       final int transform) {
        switch (transform) {
            case wl_output.Enums.Transform._90:
                target.preTranslate(0, width);
                target.preRotate(270);
                break;
            case wl_output.Enums.Transform._180:
                target.preTranslate(width, height);
                target.preRotate(180);
                break;
            case wl_output.Enums.Transform._270:
                target.preTranslate(height, 0);
                target.preRotate(90);
                break;
            case wl_output.Enums.Transform.flipped:
                target.preTranslate(width, 0);
                target.preScale(-1, 1);
                break;
            case wl_output.Enums.Transform.flipped_90:
                target.preRotate(270);
                target.preScale(-1, 1);
                break;
            case wl_output.Enums.Transform.flipped_180:
                target.preTranslate(0, height);
                target.preRotate(180);
                target.preScale(-1, 1);
                break;
            case wl_output.Enums.Transform.flipped_270:
                target.preTranslate(height, width);
                target.preRotate(90);
                target.preScale(-1, 1);
                break;
        }
    }

    private static boolean isTranslationOnly(@NonNull final float[] m) {
        return m[0] == 1f &&
                m[1] == 0f &&
                m[3] == 0f &&
                m[4] == 1f &&
                m[6] == 0f &&
                m[7] == 0f &&
                m[8] == 1f;
    }

    private final float[] _tmp_matrix = new float[9];

    private void transformRegion(@NonNull final Region target,
                                 final int width, final int height,
                                 @NonNull final Matrix matrix) { // TODO: refactor
        if (target.isEmpty() || matrix.isIdentity())
            return;
        matrix.getValues(_tmp_matrix);
        if (isTranslationOnly(_tmp_matrix)) {
            target.translate(-(int) _tmp_matrix[2], -(int) _tmp_matrix[5]);
            return;
        }
        target.set(0, 0, width, height); // Just simple stupid
//        final Matrix im = new Matrix();
//        if (!matrix.invert(im)) {
//            target.set(0, 0, width, height);
//            return;
//        }
//        final Path p = target.getBoundaryPath();
//        p.transform(im);
//        final Region clip = new Region(0, 0, width, height);
//        target.setPath(p, clip);
    }

    private final class WlClientImpl extends WlClient {
        public static final int TWEAK_ID = 2; // Tweak

        @NonNull
        private final ConnectionState connectionState;

        private GraphicsCompositor compositor = null;
        private WlOutputImpl wlOutputRes = null;
        private WlSeatImpl wlSeatRes = null;

        private WlOwnHelperImpl wlOwnHelper = null;

        private WlClientImpl(@NonNull final WlDisplay display,
                             @NonNull final ConnectionState connectionState,
                             @NonNull final WlSocket socket,
                             @Nullable final WlEventHandler sendHandler) {
            super(display, socket, sendHandler);
            this.connectionState = connectionState;
        }

        @Override
        public void init() {
            super.init();
            wlOwnHelper = new WlOwnHelperImpl(this);
            addResource(WlResource.make(this, wlOwnHelper, TWEAK_ID));
        }
    }

    private final Map<Long, WlOwnHelperImpl> ownHelpers = new ReferenceMap<>(
            AbstractReferenceMap.ReferenceStrength.HARD,
            AbstractReferenceMap.ReferenceStrength.WEAK);

    private final class WlOwnHelperImpl extends wl_own_helper {
        @NonNull
        private final WlClientImpl wlClient;
        private long uuid = 0;
        private boolean hasUuid = false;
        private DataInputStream _dis = null;
        private DataOutputStream _dos = null;

        private WlOwnHelperImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        @NonNull
        private DataInputStream dis() throws IOException {
            if (_dis == null)
                _dis = new DataInputStream(wlClient.socket.getInputStream());
            return _dis;
        }

        @NonNull
        private DataOutputStream dos() throws IOException {
            if (_dos == null)
                _dos = new DataOutputStream(wlClient.socket.getOutputStream());
            return _dos;
        }

        private static final String TAG = WlTermServer.TAG + " helper";

        private static final int TAG_ERROR = 0;
        private static final int TAG_CLIPBOARD_INLINE = 1;
        private static final int TAG_CLIPBOARD_FD = 2;
        private static final int TAG_CLIPBOARD_REQ = 3;
        private static final int TAG_IM_PUT_STRING = 0x11;

        private int readTag() throws IOException {
            return dis().readInt();
        }

        private void writeTag(final int tag) throws IOException {
            dos().writeInt(tag);
        }

        @NonNull
        private byte[] readBytes() throws IOException {
            final int l = dis().readInt();
            final byte[] b = new byte[l];
            dis().readFully(b);
            return b;
        }

        private void writeBytes(@NonNull final byte[] bytes) throws IOException {
            dos().writeInt(bytes.length);
            dos().write(bytes);
        }

        @NonNull
        private String readString() throws IOException {
            return new String(readBytes(), "UTF8");
        }

        private void writeString(@NonNull final String str) throws IOException {
            writeBytes(str.getBytes("UTF8"));
        }

        private void remove() {
            if (hasUuid) {
                for (final Map.Entry<Long, WlOwnHelperImpl> helper : ownHelpers.entrySet())
                    if (helper.getKey() == uuid)
                        ((WlSocketImpl) helper.getValue().wlClient.socket).shutdownAndClose();
                ownHelpers.remove(uuid);
                hasUuid = false;
            }
        }

        @NonNull
        private GraphicsCompositor getCompositor() {
            final GraphicsCompositor c = wlClient.compositor;
            if (c == null)
                throw new IllegalStateException("Client without compositor");
            return c;
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void mark(final long uuid) {
                remove();
                WlOwnHelperImpl.this.uuid = uuid;
                ownHelpers.put(uuid, WlOwnHelperImpl.this);
                WlOwnHelperImpl.this.hasUuid = true;
            }

            @Override
            public void connect(final long uuid, final long protocol) throws WlOwnCustomException {
                if (protocol != Enums.Protocol.simple)
                    throw new RuntimeException(String.format(Locale.ROOT,
                            "Bad helper protocol: %d", protocol));
                final WlOwnHelperImpl wlParentHelper = ownHelpers.get(uuid);
                if (wlParentHelper == null)
                    throw new RuntimeException("Bad UUID");
                throw new WlOwnCustomProtocolException(new IORunnable() {
                    private GraphicsCompositor.ClipboardContentCb cbCb = null;

                    @Override
                    public void run() throws IOException {
                        try {
                            uiHandler.post(() -> wlParentHelper.getCompositor().setAuxSource(
                                    new GraphicsCompositor.AuxSource() {
                                        @Override
                                        public void clipboardContent(@NonNull final String mime,
                                                                     @NonNull final byte[] data) {
                                            wlHandler.post(() -> {
                                                try {
                                                    writeTag(TAG_CLIPBOARD_INLINE);
                                                    writeString(mime);
                                                    writeBytes(data);
                                                } catch (final IOException e) {
                                                    if (BuildConfig.DEBUG)
                                                        Log.w(TAG,
                                                                e.getMessage() != null
                                                                        ? e.getMessage()
                                                                        : "???");
                                                }
                                            });
                                        }

                                        @Override
                                        public void clipboardContentRequest(@NonNull final String mime,
                                                                            @NonNull final GraphicsCompositor.ClipboardContentCb cb) {
                                            wlHandler.post(() -> {
                                                cbCb = cb;
                                                try {
                                                    writeTag(TAG_CLIPBOARD_REQ);
                                                    writeString(mime);
                                                } catch (final IOException e) {
                                                    if (BuildConfig.DEBUG)
                                                        Log.w(TAG,
                                                                e.getMessage() != null
                                                                        ? e.getMessage()
                                                                        : "???");
                                                }
                                            });
                                        }

                                        @Override
                                        public void imPutString(@NonNull final String str) {
                                            wlHandler.post(() -> {
                                                try {
                                                    writeTag(TAG_IM_PUT_STRING);
                                                    writeString(str);
                                                } catch (final IOException e) {
                                                    if (BuildConfig.DEBUG)
                                                        Log.w(TAG,
                                                                e.getMessage() != null
                                                                        ? e.getMessage()
                                                                        : "???");
                                                }
                                            });
                                        }
                                    }));
                            while (true) {
                                final int tag = readTag();
                                switch (tag) {
                                    case TAG_CLIPBOARD_INLINE:
                                        final String mime = readString();
                                        final byte[] data = readBytes();
                                        wlHandler.post(() -> {
                                            final GraphicsCompositor.ClipboardContentCb cb = cbCb;
                                            if (cb != null) {
                                                cbCb = null;
                                                cb.clipboardContent(mime, data);
                                            }
                                        });
                                        break;
                                    default:
                                        throw new IOException("Bad tag");
                                }
                            }
                        } finally {
                            uiHandler.post(() ->
                                    wlParentHelper.getCompositor().setAuxSource(null));
                        }
                    }
                });
            }
        }
    }

    private final class WlOutputImpl extends wl_output {
        @NonNull
        private final WlClientImpl wlClient;

        private WlOutputImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void release() {
                if (wlClient.wlOutputRes == WlOutputImpl.this)
                    wlClient.wlOutputRes = null;
                wlClient.removeResourceAndNotify(id);
                destroy();
            }
        }

        private void onResize(final int width, final int height) {
            final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            final int pw = (int) (width / dm.xdpi * 25.4f);
            final int ph = (int) (height / dm.ydpi * 25.4f);
            events.geometry(0, 0, pw, ph,
                    wl_output.Enums.Subpixel.none,
                    "Lindon inc.", "Palantir v1",
                    wl_output.Enums.Transform.normal);
            events.mode(wl_output.Enums.Mode.current | wl_output.Enums.Mode.preferred,
                    width, height, 60000);
            events.done();
        }

        private void onResize() {
            onResize(wlClient.compositor.defaultWidth, wlClient.compositor.defaultHeight);
        }
    }

    private final class WlOutputGlobalImpl implements WlGlobal {
        @Override
        @NonNull
        public Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
        getInterface() {
            return wl_output.class;
        }

        @Override
        @NonNull
        public WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
        bind(@NonNull final WlClient client, @NonNull final WlInterface.NewId newId)
                throws BindException {
            final WlInterface<wl_output.Requests, wl_output.Events> res =
                    WlResource.make(client, new WlOutputImpl((WlClientImpl) client), newId.id);
            ((WlClientImpl) client).wlOutputRes = (WlOutputImpl) res;
            ((WlOutputImpl) res).onResize();
            return res;
        }
    }

    private final class WlRegionImpl extends wl_region {
        @NonNull
        private final WlClientImpl wlClient;
        private final Region region = new Region();

        private WlRegionImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void destroy() {
                wlClient.removeResourceAndNotify(id);
                WlRegionImpl.this.destroy();
            }

            @Override
            public void add(final int x, final int y,
                            final int width, final int height) {
                region.op(x, y, x + width, y + height, Region.Op.UNION);
            }

            @Override
            public void subtract(final int x, final int y,
                                 final int width, final int height) {
                region.op(x, y, x + width, y + height, Region.Op.DIFFERENCE);
            }
        }
    }

    private static long getEventTime() {
        return System.currentTimeMillis();
    }

    private final class WlSurfaceSourceEvents implements GraphicsCompositor.SurfaceSource {
        @NonNull
        private final WlSurfaceImpl wlSurface;

        private WlSurfaceSourceEvents(@NonNull final WlSurfaceImpl wlSurface) {
            this.wlSurface = wlSurface;
        }

        @Override
        public void onEnter() {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlOutputRes == null) return;
                wlSurface.events.enter(wlClient.wlOutputRes);
            });
        }

        @Override
        public void onLeave() {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlOutputRes == null) return;
                wlSurface.events.leave(wlClient.wlOutputRes);
            });
        }

        @Override
        public void onPointerButtonEvent(final long time, final float x, final float y,
                                         final int buttonId, final int action) {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlSeatRes == null) return;
                final WlPointerImpl wlPointer = wlSurface.wlClient.wlSeatRes.wlPointerRes;
                if (wlPointer == null) return;
                if (wlPointer.wlSurfaceCurrent != wlSurface) {
                    if (wlPointer.wlSurfaceCurrent != null)
                        wlPointer.events.leave(wlDisplay.nextSerial(), wlPointer.wlSurfaceCurrent);
                    wlPointer.events.enter(wlDisplay.nextSerial(), wlSurface,
                            0, 0); // TODO: Coords
                    wlPointer.wlSurfaceCurrent = wlSurface;
                }
                wlPointer.events.motion(time, x, y);
                try {
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            if (buttonId == 0) break;
                            wlPointer.events.button(wlDisplay.nextSerial(), time,
                                    WlTermServerMaps.getPointerButtonId(buttonId),
                                    wl_pointer.Enums.Button_state.pressed);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            if (buttonId == 0) break;
                            wlPointer.events.button(wlDisplay.nextSerial(), time,
                                    WlTermServerMaps.getPointerButtonId(buttonId),
                                    wl_pointer.Enums.Button_state.released);
                            break;
                    }
                } catch (final WlTermServerMaps.TranslationException e) {
                    Log.w(TAG, e);
                }
                wlPointer.events.frame();
            });
        }

        @Override
        public void onPointerAxisEvent(final long time, final float x, final float y,
                                       final int axisId, final float value) {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlSeatRes == null) return;
                final WlPointerImpl wlPointer = wlSurface.wlClient.wlSeatRes.wlPointerRes;
                if (wlPointer == null) return;
                wlPointer.events.axis_discrete(WlTermServerMaps.getAxisId(axisId), -((int) value));
                wlPointer.events.axis(time, WlTermServerMaps.getAxisId(axisId), -(value * 10));
                wlPointer.events.axis_source(wl_pointer.Enums.Axis_source.wheel);
                wlPointer.events.frame();
            });
        }

        @Override
        public void onKeyEvent(final long time, final int keyCode, final boolean pressed) {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlSeatRes == null) return;
                final WlKeyboardImpl wlKeyboard = wlSurface.wlClient.wlSeatRes.wlKeyboardRes;
                if (wlKeyboard == null) return;
                final int kc = WlTermServerMaps.getKeyId(keyCode);
                if (kc < 0) return;
                wlKeyboard.events.key(wlDisplay.nextSerial(), time, kc, pressed ?
                        wl_keyboard.Enums.Key_state.pressed :
                        wl_keyboard.Enums.Key_state.released);
            });
        }

        @Override
        public void onKeyEvent(final boolean enter) {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlSeatRes == null) return;
                final WlKeyboardImpl wlKeyboard = wlSurface.wlClient.wlSeatRes.wlKeyboardRes;
                if (wlKeyboard == null) return;
                if (enter)
                    wlKeyboard.events.enter(wlDisplay.nextSerial(), wlSurface, new int[0]);
                else
                    wlKeyboard.events.leave(wlDisplay.nextSerial(), wlSurface);
            });
        }

        @Override
        public void onTouchEvent(final long time, final int ptId,
                                 final float x, final float y,
                                 final int action) {
            wlHandler.post(() -> {
                final WlClientImpl wlClient = wlSurface.wlClient;
                if (wlClient.wlSeatRes == null) return;
                final WlTouchImpl wlTouch = wlSurface.wlClient.wlSeatRes.wlTouchRes;
                if (wlTouch == null) return;
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        wlTouch.events.down(wlDisplay.nextSerial(), time, wlSurface, ptId, x, y);
                        wlTouch.events.frame();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        wlTouch.events.motion(time, ptId, x, y);
                        wlTouch.events.up(wlDisplay.nextSerial(), time, ptId);
                        wlTouch.events.frame();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        wlTouch.events.motion(time, ptId, x, y);
                        wlTouch.events.frame();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        wlTouch.events.cancel();
                        wlTouch.events.frame();
                        break;
                }
            });
        }
    }

    private interface WlSurfaceRole {
        default boolean onBeforeAttach(final boolean hasBuffer) {
            return true;
        }

        default boolean onBeforeCommit() {
            return true;
        }
    }

    private final class WlSurfaceImpl extends wl_surface {
        @NonNull
        private final WlClientImpl wlClient;
        @Nullable
        private WlSurfaceRole wlRole = null;
        private boolean hasPendingAttach = false;
        @Nullable
        private WlBuffer wlBuffer = null;
        private final Point wlBufferOrigin = new Point();
        private final Region wlBufferDamage = new Region();
        private final Region wlSurfaceDamage = new Region();
        private int transform = wl_output.Enums.Transform.normal;
        private int scale = 1;
        @Nullable
        private NewId nextFrameCallback = null;
        @Nullable
        private Region inputRegion = null;
        @Nullable
        private GraphicsCompositor.Surface surface = null;
        private final GraphicsCompositor.SurfaceSource eventsHandler =
                new WlSurfaceSourceEvents(this);

        private WlSurfaceImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
            onDestroy = () -> {
                if (wlRole instanceof WlInterface) {
                    final WlInterface role = (WlInterface) wlRole;
                    wlClient.removeResource(role.id);
                    role.destroy();
                }
                unmap();
            };
        }

        private void unmap() {
            if (surface == wlClient.compositor.root)
                wlClient.compositor.setRoot(null);
            if (surface == wlClient.compositor.pointer)
                wlClient.compositor.pointer = null;
            if (surface != null) {
                surface.clean();
                surface = null;
            }
        }

        private void mapAsRoot() {
            if (surface == null) {
                surface = wlClient.compositor.createSurface();
                surface.source = eventsHandler;
                wlClient.compositor.setRoot(surface);
            }
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void destroy() {
                wlClient.removeResourceAndNotify(id);
                WlSurfaceImpl.this.destroy();
            }

            @Override
            public void attach(@Nullable final wl_buffer buffer, final int x, final int y) {
                if (wlRole != null && !wlRole.onBeforeAttach(buffer != null))
                    return;
                wlBuffer = (WlBuffer) buffer;
                wlBufferOrigin.offset(x, y);
                hasPendingAttach = true;
            }

            @Override
            public void damage(final int x, final int y,
                               final int width, final int height) {
                wlSurfaceDamage.op(x, y, x + width, y + height, Region.Op.UNION);
            }

            @Override
            public void frame(@NonNull final NewId callback) {
                nextFrameCallback = callback;
            }

            @Override
            public void set_opaque_region(@Nullable final wl_region region) {
                // TODO
            }

            @Override
            public void set_input_region(@Nullable final wl_region region) {
                inputRegion = region != null ? new Region(((WlRegionImpl) region).region) : null;
            }

            private final Matrix currentTransform = new Matrix();
            private final Region currentDamage = new Region();

            @Override
            public void commit() {
                if (wlRole != null && !wlRole.onBeforeCommit())
                    return;
                hasPendingAttach = false;
                if (surface != null && wlBuffer != null) {
                    currentTransform.setTranslate(wlBufferOrigin.x, wlBufferOrigin.y);
                    currentTransform.preScale(1f / scale, 1f / scale);
                    preWlTransform(currentTransform, wlBuffer.width, wlBuffer.height,
                            transform);
                    currentDamage.set(wlSurfaceDamage);
                    transformRegion(currentDamage, wlBuffer.width, wlBuffer.height,
                            currentTransform);
                    currentDamage.op(wlBufferDamage, Region.Op.UNION);
                    try {
                        wlBuffer.pool.lock();
                    } catch (final IOException e) {
                        throw new RuntimeException("Invalid pool descriptor: " + e.getMessage());
                    }
//                    if (BuildConfig.DEBUG)
//                        Log.i(TAG, "Locked@" + wlBuffer.id);
                    final WlBuffer wlBufferCurrent = wlBuffer;
                    final NewId nextFrameCallbackCurrent = nextFrameCallback;
                    surface.commit(wlBuffer.pool.mem, wlBuffer.offset,
                            wlBuffer.width, wlBuffer.height, wlBuffer.stride,
                            (int) wlBuffer.format, currentTransform, currentDamage,
                            new GraphicsCompositor.OnSurfaceCommit() {
                                @Override
                                public void onBufferRelease() {
                                    wlBufferCurrent.pool.unlock();
//                                    if (BuildConfig.DEBUG)
//                                        Log.i(TAG, "Unlocked@" + wlBufferCurrent.id);
                                    wlHandler.post(wlBufferCurrent.events::release);
                                }

                                @Override
                                public void onNextFrame() {
                                    if (nextFrameCallbackCurrent != null)
                                        wlHandler.post(() -> wlClient.returnCallback(nextFrameCallbackCurrent));
                                }
                            });
                }
                wlBufferDamage.setEmpty();
                wlSurfaceDamage.setEmpty();
                nextFrameCallback = null;
            }

            @Override
            public void set_buffer_transform(final int transform) {
                WlSurfaceImpl.this.transform = transform;
            }

            @Override
            public void set_buffer_scale(final int scale) {
                if (scale < 1)
                    return;
                WlSurfaceImpl.this.scale = scale;
            }

            @Override
            public void damage_buffer(final int x, final int y,
                                      final int width, final int height) {
                wlBufferDamage.op(x, y, x + width, y + height, Region.Op.UNION);
            }

            @Override
            public void offset(final int x, final int y) {
                wlBufferOrigin.offset(x, y);
            }
        }
    }

    private final class WlCompositorImpl extends wl_compositor {
        @NonNull
        private final WlClientImpl wlClient;

        private WlCompositorImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void create_surface(@NonNull final NewId id) {
                wlClient.addResource(WlResource.make(wlClient, new WlSurfaceImpl(wlClient), id.id));
            }

            @Override
            public void create_region(@NonNull final NewId id) {
                wlClient.addResource(WlResource.make(wlClient, new WlRegionImpl(wlClient), id.id));
            }
        }
    }

    private final class WlCompositorGlobalImpl implements WlGlobal {
        @Override
        @NonNull
        public Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
        getInterface() {
            return wl_compositor.class;
        }

        @Override
        @NonNull
        public WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
        bind(@NonNull final WlClient client, @NonNull final WlInterface.NewId newId)
                throws BindException {
            return WlResource.make(client, new WlCompositorImpl((WlClientImpl) client), newId.id);
        }
    }

    private final class WlShellSurfaceImpl extends wl_shell_surface implements WlSurfaceRole {
        @NonNull
        private final WlSurfaceImpl wlSurface;
        @Nullable
        private Role role = null;
        @Nullable
        private String title = null;
        @Nullable
        private String className = null;

        private WlShellSurfaceImpl(@NonNull final WlSurfaceImpl wlSurface) {
            this.wlSurface = wlSurface;
            callbacks = new RequestsImpl();
            this.wlSurface.wlRole = this;
        }

        private abstract class Role {
        }

        private final class TopLevelRole extends Role {
        }

        private final class TransientRole extends Role {
            @NonNull
            private final WlSurfaceImpl parent;
            private final Point origin = new Point();
            private final long flags;

            private TransientRole(@NonNull final WlSurfaceImpl parent,
                                  final int x, final int y, final long flags) {
                this.parent = parent;
                this.origin.x = x;
                this.origin.y = y;
                this.flags = flags;
            }
        }

        private final class FullscreenRole extends Role {
            private final long method;
            private final long framerate;
            @Nullable
            private final wl_output output;

            private FullscreenRole(final long method, final long framerate,
                                   @Nullable final wl_output output) {
                this.method = method;
                this.framerate = framerate;
                this.output = output;
            }
        }

        private final class PopupRole extends Role {
            @NonNull
            private final wl_seat seat;
            private final long serial;
            @NonNull
            private final wl_surface parent;
            private final int x;
            private final int y;
            private final long flags;

            private PopupRole(@NonNull final wl_seat seat, final long serial,
                              @NonNull final wl_surface parent,
                              final int x, final int y, final long flags) {
                this.seat = seat;
                this.serial = serial;
                this.parent = parent;
                this.x = x;
                this.y = y;
                this.flags = flags;
            }
        }

        private final class MaximizedRole extends Role {
            @Nullable
            private final wl_output output;

            private MaximizedRole(@Nullable final wl_output output) {
                this.output = output;
            }
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void pong(final long serial) {
                // TODO
            }

            @Override
            public void move(@NonNull final wl_seat seat, final long serial) {
                // TODO
            }

            @Override
            public void resize(@NonNull final wl_seat seat, final long serial, final long edges) {
                // TODO
            }

            @Override
            public void set_toplevel() {
                role = new TopLevelRole();
                wlSurface.mapAsRoot();
            }

            @Override
            public void set_transient(@NonNull final wl_surface parent,
                                      final int x, final int y, final long flags) {
                role = new TransientRole((WlSurfaceImpl) parent, x, y, flags);
            }

            @Override
            public void set_fullscreen(final long method, final long framerate,
                                       @Nullable final wl_output output) {
                role = new FullscreenRole(method, framerate, output);
            }

            @Override
            public void set_popup(@NonNull final wl_seat seat, final long serial,
                                  @NonNull final wl_surface parent,
                                  final int x, final int y, final long flags) {
                role = new PopupRole(seat, serial, parent, x, y, flags);
            }

            @Override
            public void set_maximized(@Nullable final wl_output output) {
                role = new MaximizedRole(output);
            }

            @Override
            public void set_title(@NonNull final String title) {
                WlShellSurfaceImpl.this.title = title;
            }

            @Override
            public void set_class(@NonNull final String class_Name) {
                WlShellSurfaceImpl.this.className = class_Name;
            }
        }
    }

    private final class WlShellImpl extends wl_shell {
        @NonNull
        private final WlClientImpl wlClient;

        private WlShellImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void get_shell_surface(@NonNull final NewId id,
                                          @NonNull final wl_surface surface) {
                if (((WlSurfaceImpl) surface).wlRole != null) {
                    wlClient.returnError(WlShellImpl.this, Enums.Error.role,
                            "The role is already set");
                    return;
                }
                wlClient.addResource(WlResource.make(wlClient,
                        new WlShellSurfaceImpl((WlSurfaceImpl) surface), id.id));
            }
        }
    }

    private final class WlShellGlobalImpl implements WlGlobal {
        @Override
        @NonNull
        public Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
        getInterface() {
            return wl_shell.class;
        }

        @Override
        @NonNull
        public WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
        bind(@NonNull final WlClient client, @NonNull final WlInterface.NewId newId)
                throws BindException {
            return WlResource.make(client, new WlShellImpl((WlClientImpl) client), newId.id);
        }
    }

    private interface XdgSurfaceRole {
    }

    private final class XdgToplevelImpl extends xdg_toplevel implements XdgSurfaceRole {
        @NonNull
        private final XdgSurfaceImpl surface;
        @Nullable
        private String title = null;
        @Nullable
        private String appId = null;

        private XdgToplevelImpl(@NonNull final XdgSurfaceImpl surface) {
            this.surface = surface;
            callbacks = new RequestsImpl();
            surface.role = this;
            this.surface.wlSurface.mapAsRoot();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void destroy() {
                surface.role = null;
                surface.wlSurface.wlClient.removeResourceAndNotify(id);
                XdgToplevelImpl.this.destroy();
                surface.wlSurface.unmap();
            }

            @Override
            public void set_parent(@Nullable final xdg_toplevel parent) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_title(@NonNull final String title) {
                XdgToplevelImpl.this.title = title;
            }

            @Override
            public void set_app_id(@NonNull final String app_id) {
                XdgToplevelImpl.this.appId = app_id;
            }

            @Override
            public void show_window_menu(@NonNull final wl_seat seat, final long serial,
                                         final int x, final int y) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void move(@NonNull final wl_seat seat, final long serial) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void resize(@NonNull final wl_seat seat, final long serial, final long edges) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_max_size(final int width, final int height) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_min_size(final int width, final int height) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_maximized() {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void unset_maximized() {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_fullscreen(@Nullable final wl_output output) {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void unset_fullscreen() {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_minimized() {
                surface.wlSurface.wlClient.returnNotImplemented(null);
            }
        }
    }

    private final class XdgSurfaceImpl extends xdg_surface implements WlSurfaceRole {
        @NonNull
        private final WlSurfaceImpl wlSurface;
        @Nullable
        private XdgSurfaceRole role = null;
        private boolean isConfigured = false;
        private boolean inAckConfigure = false;
        private int ackConfigureSerial;

        private XdgSurfaceImpl(@NonNull final WlSurfaceImpl wlSurface) {
            this.wlSurface = wlSurface;
            callbacks = new RequestsImpl();
            onDestroy = () -> {
                // In case of destroying without check, just do it.
                if (role instanceof WlInterface) {
                    final WlInterface _role = (WlInterface) role;
                    this.wlSurface.wlClient.removeResource(_role.id);
                    _role.destroy();
                }
            };
            this.wlSurface.wlRole = this;
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void destroy() {
                if (role != null) {
                    wlSurface.wlClient.returnError(XdgSurfaceImpl.this,
                            Enums.Error.defunct_role_object,
                            "The role is still assigned");
                    return;
                }
                wlSurface.wlRole = null;
                wlSurface.wlClient.removeResourceAndNotify(id);
                XdgSurfaceImpl.this.destroy();
            }

            @Override
            public void get_toplevel(@NonNull final NewId id) {
                wlSurface.wlClient.addResource(WlResource.make(wlSurface.wlClient,
                        new XdgToplevelImpl(XdgSurfaceImpl.this), id.id));
            }

            @Override
            public void get_popup(@NonNull final NewId id, @Nullable final xdg_surface parent,
                                  @NonNull final xdg_positioner positioner) {
                wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void set_window_geometry(final int x, final int y,
                                            final int width, final int height) {
                wlSurface.wlClient.returnNotImplemented(null);
            }

            @Override
            public void ack_configure(final long serial) {
                if (!inAckConfigure || ackConfigureSerial != serial) {
                    wlSurface.wlClient.returnError(XdgSurfaceImpl.this,
                            Enums.Error.invalid_serial,
                            "");
                    return;
                }
                isConfigured = true;
                inAckConfigure = false;
            }
        }

        @Override
        public boolean onBeforeAttach(final boolean hasBuffer) {
            if (!isConfigured) {
                wlSurface.wlClient.returnError(this,
                        Enums.Error.unconfigured_buffer,
                        "");
                return false;
            }
            if (!hasBuffer) {
                isConfigured = false;
            }
            return true;
        }

        @Override
        public boolean onBeforeCommit() {
            if (role == null) {
                wlSurface.wlClient.returnError(this,
                        Enums.Error.not_constructed,
                        "");
                return false;
            }
            if (!isConfigured) {
                ackConfigureSerial = wlDisplay.nextSerial();
                events.configure(ackConfigureSerial);
                inAckConfigure = true;
            }
            return true;
        }
    }

    private final class XdgWmBaseImpl extends xdg_wm_base {
        @NonNull
        private final WlClientImpl wlClient;

        private XdgWmBaseImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void destroy() {
                wlClient.removeResourceAndNotify(id);
                XdgWmBaseImpl.this.destroy();
            }

            @Override
            public void create_positioner(@NonNull final NewId id) {
                wlClient.returnNotImplemented(null);
            }

            @Override
            public void get_xdg_surface(@NonNull final NewId id,
                                        @NonNull final wl_surface surface) {
                if (((WlSurfaceImpl) surface).wlRole != null) {
                    wlClient.returnError(XdgWmBaseImpl.this, Enums.Error.role,
                            "The role is already set");
                    return;
                }
                if (((WlSurfaceImpl) surface).hasPendingAttach) {
                    wlClient.returnError(XdgWmBaseImpl.this,
                            Enums.Error.invalid_surface_state,
                            "Something is already attached");
                    return;
                }
                wlClient.addResource(WlResource.make(wlClient,
                        new XdgSurfaceImpl((WlSurfaceImpl) surface), id.id));
            }

            @Override
            public void pong(final long serial) {
                // TODO
            }
        }
    }

    private final class XdgWmBaseGlobalImpl implements WlGlobal {
        @Override
        @NonNull
        public Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
        getInterface() {
            return xdg_wm_base.class;
        }

        @Override
        @NonNull
        public WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
        bind(@NonNull final WlClient client, @NonNull final WlInterface.NewId newId)
                throws BindException {
            return WlResource.make(client, new XdgWmBaseImpl((WlClientImpl) client), newId.id);
        }
    }

    private final class WlPointerImpl extends wl_pointer {
        @NonNull
        private final WlSeatImpl wlSeat;
        @Nullable
        private WlSurfaceImpl wlSurfaceCurrent = null;

        private WlPointerImpl(@NonNull final WlSeatImpl wlSeat) {
            this.wlSeat = wlSeat;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void set_cursor(final long serial, @Nullable final wl_surface surface,
                                   final int hotspot_x, final int hotspot_y) {
                if (surface instanceof WlSurfaceImpl) {
                    final WlSurfaceImpl wlSurface = (WlSurfaceImpl) surface;
                    wlSurface.surface = wlSeat.wlClient.compositor.createSurface();
                    wlSurface.surface.source = wlSurface.eventsHandler;
                    wlSurface.surface.setHotspot(hotspot_x, hotspot_y);
                    wlSeat.wlClient.compositor.pointer = wlSurface.surface;
                } else
                    wlSeat.wlClient.compositor.pointer = null;
            }

            @Override
            public void release() {
                wlSeat.wlPointerRes = null;
                wlSeat.wlClient.removeResourceAndNotify(id);
                WlPointerImpl.this.destroy();
            }
        }
    }

    private final class WlKeyboardImpl extends wl_keyboard {
        @NonNull
        private final WlSeatImpl wlSeat;

        private WlKeyboardImpl(@NonNull final WlSeatImpl wlSeat) {
            this.wlSeat = wlSeat;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void release() {
                wlSeat.wlKeyboardRes = null;
                wlSeat.wlClient.removeResourceAndNotify(id);
                WlKeyboardImpl.this.destroy();
            }
        }
    }

    private final class WlTouchImpl extends wl_touch {
        @NonNull
        private final WlSeatImpl wlSeat;

        private WlTouchImpl(@NonNull final WlSeatImpl wlSeat) {
            this.wlSeat = wlSeat;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void release() {
                wlSeat.wlTouchRes = null;
                wlSeat.wlClient.removeResourceAndNotify(id);
                WlTouchImpl.this.destroy();
            }
        }
    }

    private final class WlSeatImpl extends wl_seat {
        @NonNull
        private final WlClientImpl wlClient;
        @Nullable
        private WlPointerImpl wlPointerRes = null;
        private WlKeyboardImpl wlKeyboardRes = null;
        private WlTouchImpl wlTouchRes = null;

        private WlSeatImpl(@NonNull final WlClientImpl wlClient) {
            this.wlClient = wlClient;
            callbacks = new RequestsImpl();
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void get_pointer(@NonNull final NewId id) {
                wlPointerRes = (WlPointerImpl) wlClient.addResource(WlResource.make(wlClient,
                        new WlPointerImpl(WlSeatImpl.this), id.id));
            }

            @Override
            public void get_keyboard(@NonNull final NewId id) {
                wlKeyboardRes = (WlKeyboardImpl) wlClient.addResource(WlResource.make(wlClient,
                        new WlKeyboardImpl(WlSeatImpl.this), id.id));
                final GraphicsCompositor.Surface kf = wlClient.compositor.keyboardFocus;
                if (kf != null)
                    kf.source.onKeyEvent(true);
            }

            @Override
            public void get_touch(@NonNull final NewId id) {
                wlTouchRes = (WlTouchImpl) wlClient.addResource(WlResource.make(wlClient,
                        new WlTouchImpl(WlSeatImpl.this), id.id));
            }

            @Override
            public void release() {
                wlClient.wlSeatRes = null;
                wlClient.removeResourceAndNotify(id);
                WlSeatImpl.this.destroy();
            }
        }
    }

    private final class WlSeatGlobalImpl implements WlGlobal {
        @Override
        @NonNull
        public Class<? extends WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>>
        getInterface() {
            return wl_seat.class;
        }

        @Override
        @NonNull
        public WlInterface<? extends WlInterface.Requests, ? extends WlInterface.Events>
        bind(@NonNull final WlClient client, @NonNull final WlInterface.NewId newId)
                throws BindException {
            final WlInterface<? extends wl_seat.Requests, ? extends wl_seat.Events> res =
                    WlResource.make(client, new WlSeatImpl((WlClientImpl) client), newId.id);
            ((WlClientImpl) client).wlSeatRes = (WlSeatImpl) res;
            res.events.name("Narya");
            res.events.capabilities(wl_seat.Enums.Capability.pointer
                    | wl_seat.Enums.Capability.keyboard
                    | wl_seat.Enums.Capability.touch);
            return res;
        }
    }

    private void defineGlobals() {
        wlDisplay.addGlobal(new WlShm(wlMmapImpl, new int[]{
                wl_shm.Enums.Format.abgr8888,
                wl_shm.Enums.Format.xbgr8888
        }));
        wlDisplay.addGlobal(new WlOutputGlobalImpl());
        wlDisplay.addGlobal(new WlCompositorGlobalImpl());
        wlDisplay.addGlobal(new WlShellGlobalImpl());
        wlDisplay.addGlobal(new XdgWmBaseGlobalImpl());
        wlDisplay.addGlobal(new WlSeatGlobalImpl());
    }

    @UiThread
    public WlTermServer(@NonNull final Context context) {
        ctx = context;
        defineGlobals();
        wlThread = new HandlerThread(TAG + ".WlEvents");
        wlThread.setDaemon(false);
        wlThread.start();
        wlHandler = new Handler(wlThread.getLooper());
        socketListener = new WlTermServer.SocketListener();
        lth = new Thread(socketListener, TAG);
        lth.setDaemon(true);
        lth.start();
    }

    @Override
    protected void finalize() throws Throwable {
        lth.interrupt();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            wlThread.quitSafely();
        } else {
            wlThread.quit();
        }
        super.finalize();
    }
}
