package green_green_avk.anotherterm.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

public final class UiDimens {
    private UiDimens() {
    }

    public abstract static class Dimen<T extends Enum<T>> {
        @NonNull
        private final float[] values;
        @Nullable
        private final Runnable onChanged;

        protected Dimen(@NonNull final float[] values, @Nullable final Runnable onChanged) {
            this.values = values;
            this.onChanged = onChanged;
        }

        public final void set(@NonNull final Dimen<T> dimen) {
            System.arraycopy(dimen.values, 0, values, 0, values.length);
            if (onChanged != null)
                onChanged.run();
        }

        public final void reset() {
            Arrays.fill(values, 0f);
            if (onChanged != null)
                onChanged.run();
        }

        public final void setPart(final float value, @NonNull final T units) {
            values[units.ordinal()] = value;
            if (onChanged != null)
                onChanged.run();
        }

        public final void add(final float value, @NonNull final T units) {
            values[units.ordinal()] += value;
            if (onChanged != null)
                onChanged.run();
        }

        public final float getPart(@NonNull final T units) {
            return values[units.ordinal()];
        }

        protected float measure(@NonNull final float... weights) {
            float r = 0;
            for (int i = 0; i < values.length; i++) {
                r += values[i] * weights[i];
            }
            return r;
        }
    }

    public static final class Length extends Dimen<Length.Units> {
        public enum Units {
            PARENT_WIDTH, PARENT_HEIGHT,
            PARENT_MIN_DIM, PARENT_MAX_DIM,
            DP, SP
        }

        public Length(final float value, @NonNull final Units units,
                      @Nullable final Runnable onChanged) {
            super(new float[Units.values().length], onChanged);
            setPart(value, units);
        }

        public float measure(final float parentWidth, final float parentHeight,
                             final float density, final float scaledDensity) {
            return measure(parentWidth, parentHeight,
                    Math.min(parentWidth, parentHeight),
                    Math.max(parentWidth, parentHeight),
                    density, scaledDensity);
        }
    }
}
