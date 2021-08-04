package green_green_avk.anotherterm;

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

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import green_green_avk.ptyprocess.PtyProcess;
import green_green_avk.wayland.os.WlEventHandler;
import green_green_avk.wayland.os.WlMmap;
import green_green_avk.wayland.os.WlSocket;
import green_green_avk.wayland.protocol.wayland.wl_buffer;
import green_green_avk.wayland.protocol.wayland.wl_compositor;
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

    @NonNull
    private final Context ctx;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final HandlerThread wlThread;
    @NonNull
    private final Handler wlHandler;
    private final WlDisplay wlDisplay = new WlDisplay();

    private final class ClientTask extends AsyncTask<Object, Object, Object> {
        private int sessionKey = -1;
        private boolean isStopped = false;

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
            final WlSocket wlSocket = new WlSocket() {
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
            wlClient = new WlClientImpl(wlDisplay, wlSocket, null);
            wlHandler.post(() -> {
                wlClient.init();
                final GraphicsCompositor compositor = new GraphicsCompositor();
                compositor.source = new GraphicsCompositor.Source() {
                    @Override
                    @UiThread
                    public void onStop() {
                        isStopped = true;
                        wlHandler.post(() -> {
                            try {
                                PtyProcess.shutdown(socket.getFileDescriptor(),
                                        PtyProcess.SHUT_RDWR);
                                socket.close(); // TODO: correct
                            } catch (final IOException ignored) {
                            }
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
                    sessionKey = ConsoleService.startGraphicsSession(ctx, compositor);
                    // TODO: show UI?
                });
            });
            try {
                final Queue<FileDescriptor> fdsQueue = new ConcurrentLinkedQueue<>();
                while (true) {
                    final ByteBuffer msg = WlMarshalling.readRPC(is);
                    final FileDescriptor[] fds = wlSocket.getAncillaryFileDescriptors();
                    if (fds != null)
                        Collections.addAll(fdsQueue, fds);
                    wlHandler.post(() -> {
                        try {
                            final WlMarshalling.Call call =
                                    WlMarshalling.unmakeRPC(wlClient.resources, msg, fdsQueue);
//                            Log.i(TAG, "call: " + call.object.id + "[" + call.object.getClass().getSimpleName() + "]" + "." + call.method.getName());
                            try {
                                call.call();
                            } catch (final Exception e) {
                                Log.w(TAG, e.getMessage() != null ? e.getMessage() : "???");
                                wlClient.returnError(call.object, e);
                            }
                        } catch (final WlMarshalling.ParseException e) {
                            Log.w(TAG, e.getMessage() != null ? e.getMessage() : "???");
                            wlClient.returnError(e);
                        }
                    });
                }
            } catch (final EOFException | InterruptedIOException e) {
                Log.i(TAG, "EOF/INT");
            } catch (final IOException e) {
                Log.w(TAG, e.getMessage() != null ? e.getMessage() : "-");
                wlHandler.post(() -> wlClient.returnError(e));
            } catch (final WlMarshalling.ParseException e) {
                Log.w(TAG, e.getMessage() != null ? e.getMessage() : "-");
                wlHandler.post(() -> wlClient.returnError(e));
            } finally {
                uiHandler.post(() -> {
                    if (!isStopped) {
                        ConsoleService.stopSession(sessionKey);
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

    private static final class WlClientImpl extends WlClient {
        private GraphicsCompositor compositor = null;
        private WlOutputImpl wlOutputRes = null;
        private WlSeatImpl wlSeatRes = null;

        private WlClientImpl(@NonNull final WlDisplay display, @NonNull final WlSocket socket,
                             @Nullable final WlEventHandler sendHandler) {
            super(display, socket, sendHandler);
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
            final int pw = (int) (((float) width) / dm.xdpi * 25.4f);
            final int ph = (int) (((float) height) / dm.ydpi * 25.4f);
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
    }

    private final class WlSurfaceImpl extends wl_surface {
        @NonNull
        private final WlClientImpl wlClient;
        @Nullable
        private WlSurfaceRole wlRole = null;
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
                    wlClient.removeResourceAndNotify(role.id);
                    role.destroy();
                }
                if (surface == wlClient.compositor.root)
                    wlClient.compositor.setRoot(null);
                if (surface == wlClient.compositor.pointer)
                    wlClient.compositor.pointer = null;
                if (surface != null)
                    surface.clean();
            };
        }

        private final class RequestsImpl implements Requests {
            @Override
            public void destroy() {
                wlClient.removeResourceAndNotify(id);
                WlSurfaceImpl.this.destroy();
            }

            @Override
            public void attach(@Nullable final wl_buffer buffer, final int x, final int y) {
                wlBuffer = (WlBuffer) buffer;
                wlBufferOrigin.x += x;
                wlBufferOrigin.y += y;
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

            @Override
            public void commit() {
                if (surface != null && wlBuffer != null) {
                    final Matrix _transform = new Matrix();
                    _transform.setTranslate(wlBufferOrigin.x, wlBufferOrigin.y);
                    try {
                        wlBuffer.pool.lock();
                    } catch (final IOException e) {
                        throw new RuntimeException("Invalid pool descriptor: " + e.getMessage());
                    }
                    Log.i(TAG, "Locked@" + wlBuffer.id);
                    final WlBuffer wlBufferCurrent = wlBuffer;
                    final NewId nextFrameCallbackCurrent = nextFrameCallback;
                    final Region _damage = new Region(wlBufferDamage);
                    _damage.op(wlSurfaceDamage, Region.Op.UNION); // TODO: transform
                    surface.commit(wlBuffer.pool.mem, wlBuffer.offset,
                            wlBuffer.width, wlBuffer.height, wlBuffer.stride,
                            (int) wlBuffer.format, _transform, _damage,
                            new GraphicsCompositor.OnSurfaceCommit() {
                                @Override
                                public void onBufferRelease() {
                                    wlBufferCurrent.pool.unlock();
                                    Log.i(TAG, "Unlocked@" + wlBufferCurrent.id);
                                    wlHandler.post(wlBufferCurrent.events::release);
                                }

                                @Override
                                public void onNextFrame() {
                                    if (nextFrameCallbackCurrent != null)
                                        wlHandler.post(() -> wlClient.returnCallback(nextFrameCallbackCurrent));
                                }
                            });
                }
                // TODO
                wlBufferDamage.setEmpty();
                wlSurfaceDamage.setEmpty();
//                transform = wl_output.Transform.normal;
//                scale = 1;
                nextFrameCallback = null;
            }

            @Override
            public void set_buffer_transform(final int transform) {
                WlSurfaceImpl.this.transform = transform;
            }

            @Override
            public void set_buffer_scale(final int scale) {
                if (scale < 1) return;
                WlSurfaceImpl.this.scale = scale;
            }

            @Override
            public void damage_buffer(final int x, final int y,
                                      final int width, final int height) {
                wlBufferDamage.op(x, y, x + width, y + height, Region.Op.UNION);
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
                wlSurface.surface = wlSurface.wlClient.compositor.createSurface();
                wlSurface.surface.source = wlSurface.eventsHandler;
                wlSurface.wlClient.compositor.setRoot(wlSurface.surface);
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
                    wlSurface.surface.hotspot = new Point(hotspot_x, hotspot_y);
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
