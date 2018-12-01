package green_green_avk.anotherterm.utils;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Printer;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

// Alas! sendMessageAtFrontOfQueue() is final: we can't just extend the Handler class.

public class WeakHandler {

    public static class Exception extends RuntimeException {

        public Exception(Throwable cause) {
            super(cause);
        }

        public Exception(String message) {
            super(message);
        }
    }

    private static void copy(Message dst, Message src) {
        dst.obj = src.obj;
        dst.what = src.what;
        dst.arg1 = src.arg1;
        dst.arg2 = src.arg2;
        dst.replyTo = src.replyTo;
        dst.setData(src.peekData());
        if (Build.VERSION.SDK_INT >= 21)
            dst.sendingUid = src.sendingUid;
    }

    private static final class HandlerWrapper extends Handler {
        private WeakReference<WeakHandler> that;

        public HandlerWrapper(WeakHandler that) {
            super();
            this.that = new WeakReference<>(that);
        }

        public HandlerWrapper(WeakHandler that, Looper looper) {
            super(looper);
            this.that = new WeakReference<>(that);
        }

        public WeakHandler getThat() {
            return that.get();
        }

        public void setThat(WeakHandler that) {
            this.that = new WeakReference<>(that);
        }

        @Override
        public void dispatchMessage(Message msg) {
            final WeakHandler wh = that.get();
            if (wh == null) return;
            if (msg.obj instanceof ObjWrapper) {
                final Object o = ((ObjWrapper) msg.obj).obj.get();
                if (o == null) throw new Exception("Abnormal state");
                msg.obj = o;
            }
            final Runnable rw = msg.getCallback();
            if (rw instanceof RunnableWrapper) {
                final Runnable r = ((RunnableWrapper) rw).runnable.get();
                if (r == null) throw new Exception("Abnormal state");
                final Message m = Message.obtain(this, r);
                copy(m, msg);
                wh.dispatchMessage(m);
                m.recycle();
                return;

            }
            wh.dispatchMessage(msg);
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            final WeakHandler wh = that.get();
            if (wh == null) return false;
            // Just for Message.sendToTarget()
            return super.sendMessageAtTime(wh.wrapMessage(msg), uptimeMillis);
        }
    }

    private static final class RunnableWrapper implements Runnable {
        private final HandlerWrapper hw;
        public final WeakReference<Runnable> runnable;

        public RunnableWrapper(HandlerWrapper hw, @NonNull Runnable r) {
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
        private final HandlerWrapper hw;
        public final WeakReference<Object> obj;

        public ObjWrapper(HandlerWrapper hw, @NonNull Object obj) {
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

    private final HandlerWrapper handlerWrapper;
    private Handler.Callback mCallback = null;
    private final Map<Runnable, WeakReference<RunnableWrapper>> runnables = new HashMap<>();
    private final Map<Object, WeakReference<ObjWrapper>> objs = new HashMap<>();

    private synchronized RunnableWrapper obtainRunnableWrapper(Runnable r) {
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

    private synchronized RunnableWrapper getRunnableWrapper(@NonNull Runnable r) {
        final WeakReference<RunnableWrapper> wrw = runnables.get(r);
        RunnableWrapper rw = null;
        if (wrw != null)
            rw = wrw.get();
        return rw;
    }

    private synchronized void removeRunnableWrapper(@NonNull Runnable r) {
        runnables.remove(r);
    }

    private synchronized ObjWrapper obtainObjWrapper(Object obj) {
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

    private synchronized ObjWrapper getObjWrapper(@NonNull Object obj) {
        final WeakReference<ObjWrapper> wow = objs.get(obj);
        ObjWrapper ow = null;
        if (wow != null)
            ow = wow.get();
        return ow;
    }

    private synchronized void removeObjWrapper(@NonNull Object obj) {
        objs.remove(obj);
    }

    private Message wrapMessage(Message msg) {
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

    public WeakHandler(Handler.Callback callback) {
        this.mCallback = callback;
        handlerWrapper = new HandlerWrapper(this);
    }

    public WeakHandler(Looper looper) {
        handlerWrapper = new HandlerWrapper(this, looper);
    }

    public WeakHandler(Looper looper, Handler.Callback callback) {
        this.mCallback = callback;
        handlerWrapper = new HandlerWrapper(this, looper);
    }

    // TODO: other constructors

    public void handleMessage(Message msg) {
    }

    public void dispatchMessage(Message msg) {
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

    public String getMessageName(Message message) {
        return handlerWrapper.getMessageName(message);
    }

    public Message obtainMessage() {
        return handlerWrapper.obtainMessage();
    }

    public Message obtainMessage(int what) {
        return handlerWrapper.obtainMessage(what);
    }

    public Message obtainMessage(int what, Object obj) {
        return handlerWrapper.obtainMessage(what, obj);
    }

    public Message obtainMessage(int what, int arg1, int arg2) {
        return handlerWrapper.obtainMessage(what, arg1, arg2);
    }

    public Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        return handlerWrapper.obtainMessage(what, arg1, arg2, obj);
    }

    public boolean post(Runnable r) {
        return handlerWrapper.post(obtainRunnableWrapper(r));
    }

    public boolean postAtTime(Runnable r, long uptimeMillis) {
        return handlerWrapper.postAtTime(obtainRunnableWrapper(r), uptimeMillis);
    }

    public boolean postAtTime(Runnable r, Object token, long uptimeMillis) {
        return handlerWrapper.postAtTime(obtainRunnableWrapper(r), obtainObjWrapper(token), uptimeMillis);
    }

    public boolean postDelayed(Runnable r, long delayMillis) {
        return handlerWrapper.postDelayed(obtainRunnableWrapper(r), delayMillis);
    }

    public boolean postDelayed(Runnable r, Object token, long delayMillis) {
        return handlerWrapper.postDelayed(obtainRunnableWrapper(r), obtainObjWrapper(token), delayMillis);
    }

    public boolean postAtFrontOfQueue(Runnable r) {
        return handlerWrapper.postAtFrontOfQueue(obtainRunnableWrapper(r));
    }

    public void removeCallbacks(Runnable r) {
        if (r != null) {
            r = getRunnableWrapper(r);
            if (r == null) return;
        }
        handlerWrapper.removeCallbacks(r);
    }

    public void removeCallbacks(Runnable r, Object token) {
        if (r != null) {
            r = getRunnableWrapper(r);
            if (r == null) return;
        }
        if (token != null) {
            token = getObjWrapper(token);
            if (token == null) return;
        }
        handlerWrapper.removeCallbacks(r, token);
    }

    public boolean sendMessage(Message msg) {
        return handlerWrapper.sendMessage(wrapMessage(msg));
    }

    public boolean sendEmptyMessage(int what) {
        return handlerWrapper.sendEmptyMessage(what);
    }

    public boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        return handlerWrapper.sendEmptyMessageDelayed(what, delayMillis);
    }

    public boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
        return handlerWrapper.sendEmptyMessageAtTime(what, uptimeMillis);
    }

    public boolean sendMessageDelayed(Message msg, long delayMillis) {
        return handlerWrapper.sendMessageDelayed(wrapMessage(msg), delayMillis);
    }

    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        return handlerWrapper.sendMessageAtTime(wrapMessage(msg), uptimeMillis);
    }

    public boolean sendMessageAtFrontOfQueue(Message msg) {
        return handlerWrapper.sendMessageAtFrontOfQueue(wrapMessage(msg));
    }

    public void removeMessages(int what) {
        handlerWrapper.removeMessages(what);
    }

    public void removeMessages(int what, Object object) {
        if (object != null) {
            object = getObjWrapper(object);
            if (object == null) return;
        }
        handlerWrapper.removeMessages(what, object);
    }

    public void removeCallbacksAndMessages(Object token) {
        if (token != null) {
            token = getObjWrapper(token);
            if (token == null) return;
        }
        handlerWrapper.removeCallbacksAndMessages(token);
    }

    public boolean hasMessages(int what) {
        return handlerWrapper.hasMessages(what);
    }

    public boolean hasMessages(int what, Object object) {
        if (object != null) {
            object = getObjWrapper(object);
            if (object == null) return false;
        }
        return handlerWrapper.hasMessages(what, object);
    }

    public Looper getLooper() {
        return handlerWrapper.getLooper();
    }

    public void dump(Printer pw, String prefix) {
        handlerWrapper.dump(pw, prefix);
    }

    @NonNull
    @Override
    public String toString() {
        return handlerWrapper.toString();
    }
}
