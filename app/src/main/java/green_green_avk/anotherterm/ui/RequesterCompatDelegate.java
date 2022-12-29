package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Temporary
 * {@link androidx.activity.result.ActivityResultCaller#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract, androidx.activity.result.ActivityResultCallback)}
 * like solution before full AndroidX / Fragments migration...
 */
public final class RequesterCompatDelegate {
    @NonNull
    private final ViewModelStoreOwner that;

    public RequesterCompatDelegate(@NonNull final ViewModelStoreOwner that) {
        this.that = that;
    }

    public interface ActivityResultCallback {
        void onActivityResult(int resultCode, @Nullable Intent data);
    }

    private static int requestCodeOf(@NonNull final Object v) {
        return v.hashCode() & 0xFFFF;
    }

    private static class ResultMeta {
        @NonNull
        private WeakReference<ActivityResultCallback> callback;
        private boolean isReady = false;
        private int resultCode = Activity.RESULT_CANCELED;
        @Nullable
        private Intent data = null;

        private ResultMeta(@NonNull final WeakReference<ActivityResultCallback> callback) {
            this.callback = callback;
        }
    }

    public static class ResultModel extends ViewModel {
        private final Map<Integer, ResultMeta> map = new HashMap<>();
    }

    private ResultModel resultModel;

    public void onCreate(@Nullable final Bundle savedInstanceState) {
        resultModel = new ViewModelProvider(that).get(ResultModel.class);
    }

    public void onResume() {
        for (final Iterator<Map.Entry<Integer, ResultMeta>> it =
             resultModel.map.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Integer, ResultMeta> et = it.next();
            final ResultMeta v = et.getValue();
            if (v.isReady) {
                final ActivityResultCallback cb = v.callback.get();
                if (cb != null) {
                    it.remove();
                    cb.onActivityResult(v.resultCode, v.data);
                }
            }
        }
    }

    public void onActivityResult(final int requestCode,
                                 final int resultCode, @Nullable final Intent data) {
        final ResultMeta v = resultModel.map.get(requestCode);
        if (v != null) {
            v.resultCode = resultCode;
            v.data = data;
            v.isReady = true;
        }
    }

    private void startActivityForResult(@NonNull final Intent intent, final int requestCode) {
        if (that instanceof Activity)
            ((Activity) that).startActivityForResult(intent, requestCode);
        else
            throw new IllegalStateException();
    }

    private boolean isResumed() {
        if (that instanceof AppCompatActivity)
            return ((LifecycleOwner) that).getLifecycle().getCurrentState()
                    == Lifecycle.State.RESUMED;
        else
            throw new IllegalStateException();
    }

    public void check(@NonNull final Object key,
                      @NonNull final ActivityResultCallback callback) {
        final int code = requestCodeOf(key);
        final ResultMeta v = resultModel.map.get(code);
        if (v == null)
            return;
        if (v.isReady) {
            resultModel.map.remove(code);
            callback.onActivityResult(v.resultCode, v.data);
        }
    }

    public void checkOnResume(@NonNull final Object key,
                              @NonNull final ActivityResultCallback callback) {
        final int code = requestCodeOf(key);
        final ResultMeta v = resultModel.map.get(code);
        if (v == null)
            return;
        v.callback = new WeakReference<>(callback);
        if (isResumed() && v.isReady) {
            final ActivityResultCallback cb = v.callback.get();
            if (cb != null) {
                resultModel.map.remove(code);
                cb.onActivityResult(v.resultCode, v.data);
            }
        }
    }

    public void launch(@NonNull final Object key,
                       @NonNull final Intent intent,
                       @NonNull final ActivityResultCallback callback) {
        final int code = requestCodeOf(key);
        if (resultModel.map.get(code) != null)
            return;
        resultModel.map.put(code, new ResultMeta(new WeakReference<>(callback)));
        startActivityForResult(intent, code);
    }

    public void cancel(@NonNull final Object key) {
        resultModel.map.remove(requestCodeOf(key));
    }
}
