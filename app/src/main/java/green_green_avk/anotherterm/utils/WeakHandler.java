package green_green_avk.anotherterm.utils;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Printer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

// Alas! sendMessageAtFrontOfQueue() is final: we can't just extend the Handler class.

public class WeakHandler {

    public static class Exception extends RuntimeException {

        public Exception(final Throwable cause) {
            super(cause);
        }

        public Exception(final String message) {
            super(message);
        }
    }

    private static void copy(@NonNull final Message dst, @NonNull final Message src) {
        dst.obj = src.obj;
        dst.what = src.what;
        dst.arg1 = src.arg1;
        dst.arg2 = src.arg2;
        dst.replyTo = src.replyTo;
        dst.setData(src.peekData());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dst.sendingUid = src.sendingUid;
        }
    }

    private static final class HandlerWrapper extends Handler {
        @NonNull
        private final WeakReference<WeakHandler> that;

        private HandlerWrapper(final WeakHandler that) {
            super();
            this.that = new WeakReference<>(that);
        }

        private HandlerWrapper(final WeakHandler that, final Looper looper) {
            super(looper);
            this.that = new WeakReference<>(that);
        }

        private WeakHandler getThat() {
            return that.get();
        }

        @Override
        public void dispatchMessage(@NonNull final Message msg) {
            final WeakHandler wh = that.get();
            if (wh == null) return;
            if (msg.obj instanceof ObjWrapper) {
                final Object o = ((ObjWrapper) msg.obj).obj.get();
                if (o == null) return;
                msg.obj = o;
            }
            final Runnable rw = msg.getCallback();
            if (rw instanceof RunnableWrapper) {
                final Runnable r = ((RunnableWrapper) rw).runnable.get();
                if (r == null) return;
                final Message m = Message.obtain(this, r);
                copy(m, msg);
                wh.dispatchMessage(m);
                m.recycle();
                return;
            }
            wh.dispatchMessage(msg);
        }

        @Override
        public boolean sendMessageAtTime(@NonNull final Message msg, final long uptimeMillis) {
            final WeakHandler wh = that.get();
            if (wh == null) return false;
            // Just for Message.sendToTarget()
            return super.sendMessageAtTime(wh.wrapMessage(msg), uptimeMillis);
        }
    }

    private static final class RunnableWrapper implements Runnable {
        @NonNull
        private final HandlerWrapper hw;
        @NonNull
        private final WeakReference<Runnable> runnable;

        private RunnableWrapper(@NonNull final HandlerWrapper hw, @NonNull final Runnable r) {
            this.hw = hw;
            this.runnable = new WeakReference<>(r);
        }

        @Override
        protected void finalize() throws Throwable {
            final WeakHandler wh = hw.getThat();
            final Runnable r = runnable.get();
            if (wh != null && r != null) {
                wh.removeRunnableWrapper(r);
            }
            super.finalize();
        }

        @Override
        public void run() {
            // Can be empty, just in case...
            final WeakHandler wh = hw.getThat();
            final Runnable r = runnable.get();
            if (wh == null || r == null) {
                return;
            }
            r.run();
        }
    }

    private static final class ObjWrapper {
        @NonNull
        private final HandlerWrapper hw;
        @NonNull
        private final WeakReference<Object> obj;

        private ObjWrapper(@NonNull final HandlerWrapper hw, @NonNull final Object obj) {
            this.hw = hw;
            this.obj = new WeakReference<>(obj);
        }

        @Override
        protected void finalize() throws Throwable {
            final WeakHandler wh = hw.getThat();
            final Object obj = this.obj.get();
            if (wh != null && obj != null) {
                wh.removeObjWrapper(obj);
            }
            super.finalize();
        }
    }

    @NonNull
    private final HandlerWrapper handlerWrapper;
    private Handler.Callback mCallback = null;
    private final Map<Runnable, WeakReference<RunnableWrapper>> runnables = new HashMap<>();
    private final Map<Object, WeakReference<ObjWrapper>> objs = new HashMap<>();

    private synchronized RunnableWrapper obtainRunnableWrapper(@Nullable final Runnable r) {
        if (r == null) return null;
        final WeakReference<RunnableWrapper> wrw = runnables.get(r);
        RunnableWrapper rw = null;
        if (wrw != null)
            rw = wrw.get();
        if (rw == null) {
            rw = new RunnableWrapper(handlerWrapper, r);
            runnables.put(r, new WeakReference<>(rw));
        }
        return rw;
    }

    private synchronized RunnableWrapper getRunnableWrapper(@NonNull final Runnable r) {
        final WeakReference<RunnableWrapper> wrw = runnables.get(r);
        RunnableWrapper rw = null;
        if (wrw != null)
            rw = wrw.get();
        return rw;
    }

    private synchronized void removeRunnableWrapper(@NonNull final Runnable r) {
        runnables.remove(r);
    }

    private synchronized ObjWrapper obtainObjWrapper(@Nullable final Object obj) {
        if (obj == null) return null;
        final WeakReference<ObjWrapper> wow = objs.get(obj);
        ObjWrapper ow = null;
        if (wow != null)
            ow = wow.get();
        if (ow == null) {
            ow = new ObjWrapper(handlerWrapper, obj);
            objs.put(obj, new WeakReference<>(ow));
        }
        return ow;
    }

    private synchronized ObjWrapper getObjWrapper(@NonNull final Object obj) {
        final WeakReference<ObjWrapper> wow = objs.get(obj);
        ObjWrapper ow = null;
        if (wow != null)
            ow = wow.get();
        return ow;
    }

    private synchronized void removeObjWrapper(@NonNull final Object obj) {
        objs.remove(obj);
    }

    private Message wrapMessage(@NonNull final Message msg) {
        if (msg.obj != null && !(msg.obj instanceof ObjWrapper)) {
            msg.obj = obtainObjWrapper(msg.obj);
        }
        if (msg.getCallback() == null || msg.getCallback() instanceof RunnableWrapper) return msg;
        final Message m = Message.obtain(null, obtainRunnableWrapper(msg.getCallback()));
        copy(m, msg);
        msg.recycle(); // TODO: Message cannot be used after messageSend() call yet...
        return m;
    }

    @Override
    protected void finalize() throws Throwable {
        removeCallbacksAndMessages(null);
        super.finalize();
    }

    public WeakHandler() {
        handlerWrapper = new HandlerWrapper(this);
    }

    public WeakHandler(final Handler.Callback callback) {
        this.mCallback = callback;
        handlerWrapper = new HandlerWrapper(this);
    }

    public WeakHandler(final Looper looper) {
        handlerWrapper = new HandlerWrapper(this, looper);
    }

    public WeakHandler(final Looper looper, final Handler.Callback callback) {
        this.mCallback = callback;
        handlerWrapper = new HandlerWrapper(this, looper);
    }

    // TODO: other constructors

    public void handleMessage(@NonNull final Message msg) {
    }

    public void dispatchMessage(@NonNull final Message msg) {
        if (msg.getCallback() != null) {
            msg.getCallback().run();
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }

    /* // TODO: Later
    public static Handler createAsync(@NonNull Looper looper) {
        return Handler.createAsync(looper);
    }

    public static Handler createAsync(@NonNull Looper looper, @NonNull Handler.Callback callback) {
        return Handler.createAsync(looper, callback);
    }
    */

    @NonNull
    public String getMessageName(@NonNull final Message message) {
        return handlerWrapper.getMessageName(message);
    }

    @NonNull
    public Message obtainMessage() {
        return handlerWrapper.obtainMessage();
    }

    @NonNull
    public Message obtainMessage(final int what) {
        return handlerWrapper.obtainMessage(what);
    }

    @NonNull
    public Message obtainMessage(final int what, final Object obj) {
        return handlerWrapper.obtainMessage(what, obj);
    }

    @NonNull
    public Message obtainMessage(final int what, final int arg1, final int arg2) {
        return handlerWrapper.obtainMessage(what, arg1, arg2);
    }

    @NonNull
    public Message obtainMessage(final int what, final int arg1, final int arg2, final Object obj) {
        return handlerWrapper.obtainMessage(what, arg1, arg2, obj);
    }

    public boolean post(@NonNull final Runnable r) {
        return handlerWrapper.post(obtainRunnableWrapper(r));
    }

    public boolean postAtTime(@NonNull final Runnable r, final long uptimeMillis) {
        return handlerWrapper.postAtTime(obtainRunnableWrapper(r), uptimeMillis);
    }

    public boolean postAtTime(@NonNull final Runnable r, final Object token,
                              final long uptimeMillis) {
        return handlerWrapper.postAtTime(obtainRunnableWrapper(r), obtainObjWrapper(token),
                uptimeMillis);
    }

    public boolean postDelayed(@NonNull final Runnable r, final long delayMillis) {
        return handlerWrapper.postDelayed(obtainRunnableWrapper(r), delayMillis);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public boolean postDelayed(@NonNull final Runnable r, final Object token,
                               final long delayMillis) {
        return handlerWrapper.postDelayed(obtainRunnableWrapper(r), obtainObjWrapper(token),
                delayMillis);
    }

    public boolean postAtFrontOfQueue(@NonNull final Runnable r) {
        return handlerWrapper.postAtFrontOfQueue(obtainRunnableWrapper(r));
    }

    public void removeCallbacks(@NonNull Runnable r) {
        r = getRunnableWrapper(r);
        if (r == null) return;
        handlerWrapper.removeCallbacks(r);
    }

    public void removeCallbacks(@NonNull Runnable r, @Nullable Object token) {
        r = getRunnableWrapper(r);
        if (r == null) return;
        if (token != null) {
            token = getObjWrapper(token);
            if (token == null) return;
        }
        handlerWrapper.removeCallbacks(r, token);
    }

    public boolean sendMessage(@NonNull final Message msg) {
        return handlerWrapper.sendMessage(wrapMessage(msg));
    }

    public boolean sendEmptyMessage(final int what) {
        return handlerWrapper.sendEmptyMessage(what);
    }

    public boolean sendEmptyMessageDelayed(final int what, final long delayMillis) {
        return handlerWrapper.sendEmptyMessageDelayed(what, delayMillis);
    }

    public boolean sendEmptyMessageAtTime(final int what, final long uptimeMillis) {
        return handlerWrapper.sendEmptyMessageAtTime(what, uptimeMillis);
    }

    public boolean sendMessageDelayed(@NonNull final Message msg, final long delayMillis) {
        return handlerWrapper.sendMessageDelayed(wrapMessage(msg), delayMillis);
    }

    public boolean sendMessageAtTime(@NonNull final Message msg, final long uptimeMillis) {
        return handlerWrapper.sendMessageAtTime(wrapMessage(msg), uptimeMillis);
    }

    public boolean sendMessageAtFrontOfQueue(@NonNull final Message msg) {
        return handlerWrapper.sendMessageAtFrontOfQueue(wrapMessage(msg));
    }

    public void removeMessages(final int what) {
        handlerWrapper.removeMessages(what);
    }

    public void removeMessages(final int what, @Nullable Object object) {
        if (object != null) {
            object = getObjWrapper(object);
            if (object == null) return;
        }
        handlerWrapper.removeMessages(what, object);
    }

    public void removeCallbacksAndMessages(@Nullable Object token) {
        if (token != null) {
            token = getObjWrapper(token);
            if (token == null) return;
        }
        handlerWrapper.removeCallbacksAndMessages(token);
    }

    public boolean hasMessages(final int what) {
        return handlerWrapper.hasMessages(what);
    }

    public boolean hasMessages(final int what, @Nullable Object object) {
        if (object != null) {
            object = getObjWrapper(object);
            if (object == null) return false;
        }
        return handlerWrapper.hasMessages(what, object);
    }

    @NonNull
    public Looper getLooper() {
        return handlerWrapper.getLooper();
    }

    public void dump(@NonNull final Printer pw, final String prefix) {
        handlerWrapper.dump(pw, prefix);
    }

    @NonNull
    @Override
    public String toString() {
        return handlerWrapper.toString();
    }
}
