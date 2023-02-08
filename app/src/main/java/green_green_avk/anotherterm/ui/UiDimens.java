package green_green_avk.anotherterm.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UiDimens {
    private UiDimens() {
    }

    public static final class Length {
        public enum Units {
            PARENT_WIDTH, PARENT_HEIGHT,
            PARENT_MIN_DIM, PARENT_MAX_DIM
        }

        private float value;
        @NonNull
        private Units units;
        @Nullable
        private final Runnable onChanged;

        public Length(final float value, @NonNull final Units units,
                      @Nullable final Runnable onChanged) {
            this.value = value;
            this.units = units;
            this.onChanged = onChanged;
        }

        public void set(@NonNull final Length length) {
            set(length.value, length.units);
        }

        public void set(final float value, @NonNull final Units units) {
            this.value = value;
            this.units = units;
            if (onChanged != null)
                onChanged.run();
        }

        public float getValue() {
            return value;
        }

        @NonNull
        public Units getUnits() {
            return units;
        }
    }
}
