package green_green_avk.anotherterm.ui;

import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public final class ChoreographerCompat {
    private final Choreographer mChoreographer;
    private final Handler mHandler;

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mChoreographer = Choreographer.getInstance();
            mHandler = null;
        } else {
            mChoreographer = null;
            mHandler = new Handler(Looper.getMainLooper());
        }
    }

    private ChoreographerCompat() {
    }

    private static final ThreadLocal<ChoreographerCompat> sInstance =
            new ThreadLocal<ChoreographerCompat>() {
                @Override
                protected ChoreographerCompat initialValue() {
                    return new ChoreographerCompat();
                }
            };

    public static ChoreographerCompat getInstance() {
        return sInstance.get();
    }

    public void post(@NonNull final Runnable runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            _post(runnable);
        else
            mHandler.postDelayed(runnable, ValueAnimator.getFrameDelay());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void _post(@NonNull final Runnable runnable) {
        mChoreographer.postFrameCallback(frameTimeNanos -> runnable.run());
    }
}
