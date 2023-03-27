package green_green_avk.anotherterm.ui;

import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public abstract class ChoreographerCompat {
    private ChoreographerCompat() {
    }

    private static final ThreadLocal<ChoreographerCompat> threadInstance =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                    new ThreadLocal<ChoreographerCompat>() {
                        @Override
                        @NonNull
                        protected ChoreographerCompat initialValue() {
                            return new ChoreographerCompatImplNew();
                        }
                    } :
                    new ThreadLocal<ChoreographerCompat>() {
                        @Override
                        @NonNull
                        protected ChoreographerCompat initialValue() {
                            return new ChoreographerCompatImplOld();
                        }
                    };

    @NonNull
    public static ChoreographerCompat getInstance() {
        return threadInstance.get();
    }

    public abstract void post(@NonNull final Runnable runnable);

    private static final class ChoreographerCompatImplOld extends ChoreographerCompat {
        private final Handler handler;

        {
            final Looper looper = Looper.myLooper();
            if (looper == null)
                throw new IllegalStateException("The current thread must have a looper!");
            handler = new Handler(looper);
        }

        @Override
        public void post(@NonNull final Runnable runnable) {
            handler.postDelayed(runnable, ValueAnimator.getFrameDelay());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private static final class ChoreographerCompatImplNew extends ChoreographerCompat {
        private final Choreographer choreographer = Choreographer.getInstance();

        @Override
        public void post(@NonNull final Runnable runnable) {
            choreographer.postFrameCallback(frameTimeNanos -> runnable.run());
        }
    }
}
