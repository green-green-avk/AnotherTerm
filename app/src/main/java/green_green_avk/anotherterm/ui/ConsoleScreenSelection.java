package green_green_avk.anotherterm.ui;

import android.graphics.Point;

import androidx.annotation.NonNull;

public class ConsoleScreenSelection {
    public final Point first;
    public final Point last;
    public boolean isRectangular = false;
    private final ConsoleScreenSelection direct;

    public ConsoleScreenSelection() {
        first = new Point();
        last = new Point();
        direct = new ConsoleScreenSelection(this);
    }

    private ConsoleScreenSelection(@NonNull final ConsoleScreenSelection ignoredO) {
        first = new Point();
        last = new Point();
        direct = this;
    }

    @NonNull
    public ConsoleScreenSelection getDirect() {
        if (direct == this)
            return this;
        if (isRectangular) {
            direct.first.x = Math.min(first.x, last.x);
            direct.first.y = Math.min(first.y, last.y);
            direct.last.x = Math.max(first.x, last.x);
            direct.last.y = Math.max(first.y, last.y);
            direct.isRectangular = true;
        } else if (last.y < first.y || last.y == first.y && last.x < first.x) {
            direct.first.set(last.x, last.y);
            direct.last.set(first.x, first.y);
            direct.isRectangular = false;
        } else {
            direct.first.set(first.x, first.y);
            direct.last.set(last.x, last.y);
        }
        return direct;
    }
}
