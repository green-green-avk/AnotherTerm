package green_green_avk.wayland.protocol_core;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Queue;

public final class WlMarshalling {
    private WlMarshalling() {
    }

    private static final int BUF_MAX = 4096;
    private static final int CLIENT_ID_MAX = 0xFEFFFFFF;

    private static final Charset UTF8 = Charset.forName("UTF8");

    public static final class ParseException extends Exception {
        public ParseException() {
        }

        public ParseException(final String message) {
            super(message);
        }

        public ParseException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public ParseException(final Throwable cause) {
            super(cause);
        }
    }

    public static final class Call {
        @NonNull
        public final WlInterface object;
        @NonNull
        public final Method method;
        @NonNull
        public final Object[] args;

        private Call(@NonNull final WlInterface object,
                     @NonNull final Method method,
                     @NonNull final Object[] args) {
            this.object = object;
            this.method = method;
            this.args = args;
        }

        public void call() throws Exception {
            try {
                method.invoke(object.callbacks, args);
            } catch (final InvocationTargetException e) {
                final Throwable t = e.getTargetException();
                if (t instanceof Exception)
                    throw (Exception) t;
                else
                    throw new Error(e);
            } catch (final Exception e) {
                throw new Error(e);
            }
        }
    }

    private static long be32(final long v) {
        if (ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN)
            return (((v & 0xFF) << 24) | ((v & 0xFF00) << 8) |
                    ((v & 0xFF0000) >>> 8) | ((v & 0xFF000000L) >>> 24));
        else
            return v & 0xFFFFFFFFL;
    }

    private static void putString(@NonNull final DataOutputStream dos, @Nullable final Object v)
            throws IOException {
        if (v == null) {
            dos.writeInt(0);
        } else {
            final byte[] s = (v.toString()).getBytes(UTF8);
            dos.writeInt((int) be32(s.length + 1));
            dos.write(s);
            dos.write(0); // NULL-terminator
            int pad = (s.length + 1) % 4;
            if (pad != 0) for (; pad < 4; pad++) dos.write(0);
        }
    }

    @NonNull
    public static ByteBuffer makeRPC(@NonNull final List<FileDescriptor> fds,
                                     @NonNull final WlInterface object,
                                     @NonNull final Method method,
                                     @NonNull final Object[] args) {
        final WlInterface.IMethod ma = method.getAnnotation(WlInterface.IMethod.class);
        final int methodId = ma.value();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(os);
        int i = 0;
        try {
            for (final Class<?> type : method.getParameterTypes()) {
                if (type == FileDescriptor.class) {
                    fds.add((FileDescriptor) args[i]);
                } else if (WlInterface.class.isAssignableFrom(type)) {
                    if (args[i] == null)
                        dos.writeInt(0);
                    else
                        dos.writeInt((int) be32(((WlInterface) args[i]).id));
                } else if (type == WlInterface.NewId.class) {
                    final WlInterface.NewId ni = (WlInterface.NewId) args[i];
                    final Class<? extends WlInterface> iface =
                            WlInterface.getParamInterface(method, i);
                    if (iface == null) {
                        putString(dos, ni.interfaceName);
                        dos.writeInt((int) be32(ni.version));
                    }
                    dos.writeInt((int) be32(ni.id));
                } else if (type == String.class) {
                    putString(dos, args[i]);
                } else if (type == int[].class) {
                    final int[] v = (int[]) args[i];
                    dos.writeInt((int) be32(v.length * 4));
                    for (final int ve : v)
                        dos.writeInt((int) be32(ve));
                } else if (type == float.class || type == double.class) {
                    dos.writeInt((int) be32((long) (((float) args[i]) * 256)));
                } else {
                    final long v;
                    if (args[i] instanceof Long) v = (long) args[i];
                    else v = (long) (int) args[i];
                    dos.writeInt((int) be32(v));
                }
                i++;
            }
            dos.flush();
        } catch (final IOException e) {
            // never happens
        }
        final int len = 8 + dos.size();
        return (ByteBuffer) ByteBuffer.allocate(len).order(ByteOrder.nativeOrder())
                .putInt(object.id)
                .putInt((methodId & 0xFFFF) | (len << 16))
                .put(os.toByteArray())
                .flip();
    }

    @NonNull
    private static WlInterface getObject(@NonNull final SparseArray<WlInterface> objects,
                                         final int id)
            throws ParseException {
        final WlInterface object;
        if ((object = objects.get(id)) == null)
            throw new ParseException("Bad object id: " + id);
        return object;
    }

    @Nullable
    private static String getString(@NonNull final ByteBuffer buffer) throws ParseException {
        final int sl = buffer.getInt();
        if (sl < 0 || sl > BUF_MAX)
            throw new ParseException("Bad string length: " + sl);
        if (sl == 0) {
            return null;
        } else {
            final byte[] s = new byte[sl - 1];
            buffer.get(s);
            buffer.get(); // NULL-terminator
            final int pad = buffer.position() % 4;
            if (pad != 0) try {
                buffer.position(buffer.position() + 4 - pad);
            } catch (final IllegalArgumentException e) {
                throw new ParseException("Bad string padding");
            }
            return new String(s, UTF8);
        }
    }

    @NonNull
    public static Call unmakeRPC(@NonNull final SparseArray<WlInterface> objects,
                                 @NonNull final ByteBuffer buffer,
                                 @Nullable final Queue<FileDescriptor> fds)
            throws ParseException {
        buffer.order(ByteOrder.nativeOrder());
        final int id = buffer.getInt();
        final int ml = buffer.getInt();
        final int methodId = ml & 0xFFFF;
        final int len = ml >>> 16;
        final WlInterface object = getObject(objects, id);
        final WlInterface.Callbacks cbs = object.callbacks;
        if (cbs == null)
            throw new ParseException("Bad method id: " + methodId);
        final Method method = WlInterface.getMethod(cbs.getClass(), methodId);
        if (method == null)
            throw new ParseException("Bad method id: " + methodId);
        final Class<?>[] types = method.getParameterTypes();
        final Object[] args = new Object[types.length];
        int i = 0;
        try {
            for (final Class<?> type : types) {
                if (type == FileDescriptor.class) {
                    final FileDescriptor fd;
                    if (fds == null || (fd = fds.poll()) == null)
                        throw new ParseException("Not enough FDs passed");
                    args[i] = fd;
                } else if (WlInterface.class.isAssignableFrom(type)) {
                    final int vId = buffer.getInt();
                    final WlInterface v;
                    if (vId == 0) {
                        if (!WlInterface.isParamNullable(method, i))
                            throw new ParseException("NULL object passed");
                        else
                            v = null;
                    } else
                        v = getObject(objects, vId);
                    args[i] = v;
                } else if (type == WlInterface.NewId.class) {
                    final Class<? extends WlInterface> iface =
                            WlInterface.getParamInterface(method, i);
                    final String ni_if;
                    final int ni_ver;
                    if (iface == null) {
                        ni_if = getString(buffer);
                        ni_ver = buffer.getInt();
                    } else {
                        ni_if = WlInterface.getName(iface);
                        ni_ver = 0;
                    }
                    final int ni_id = buffer.getInt();
                    if (ni_id > CLIENT_ID_MAX && ni_id < 1)
                        throw new ParseException("Bad new id: " + ni_id);
                    args[i] = new WlInterface.NewId(ni_if, ni_ver, ni_id);
                } else if (type == String.class) {
                    final String v = getString(buffer);
                    if (v == null && !WlInterface.isParamNullable(method, i))
                        throw new ParseException("NULL string passed");
                    args[i] = v;
                } else if (type == int[].class) {
                    final int alb = buffer.getInt();
                    final int[] a = new int[alb / 4];
                    for (int ai = 0; ai < a.length; ai++)
                        a[ai] = buffer.getInt();
                    if (alb % 4 != 0)
                        buffer.getInt(); // Just in case
                    args[i] = a;
                } else if (type == float.class || type == double.class) {
                    args[i] = ((float) (((long) buffer.getInt()) & 0xFFFFFFFFL)) / 256;
                } else if (type == long.class) {
                    args[i] = ((long) buffer.getInt()) & 0xFFFFFFFFL;
                } else {
                    args[i] = buffer.getInt();
                }
                i++;
            }
        } catch (final BufferUnderflowException e) {
            throw new ParseException("Wire protocol format error", e);
        }
        return new Call(object, method, args);
    }

    @NonNull
    public static ByteBuffer readRPC(@NonNull final InputStream is)
            throws IOException, ParseException {
        final DataInputStream dis = new DataInputStream(is);
        final int id = (int) be32(dis.readInt());
        final int ml = (int) be32(dis.readInt());
        final int len = ml >>> 16;
        if (len < 8 || len > BUF_MAX)
            throw new ParseException("Bad message length: " + len);
        final ByteBuffer r = ByteBuffer.allocate(len).order(ByteOrder.nativeOrder())
                .putInt(id).putInt(ml);
        dis.readFully(r.array(), r.arrayOffset() + r.position(), r.remaining());
        r.clear();
        return r;
    }
}
