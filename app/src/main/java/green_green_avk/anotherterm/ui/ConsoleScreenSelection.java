package green_green_avk.anotherterm.ui;

import android.graphics.Point;

import androidx.annotation.NonNull;

public class ConsoleScreenSelection {
    public final Point first;
    public final Point last;
    public boolean isRectangular = false;
    private final ConsoleScreenSelection inv;

    public ConsoleScreenSelection() {
        first = new Point();
        last = new Point();
        inv = new ConsoleScreenSelection(this);
    }

    private ConsoleScreenSelection(@NonNull final ConsoleScreenSelection o) {
        first = o.last;
        last = o.first;
        inv = o;
    }

    @NonNull
    public ConsoleScreenSelection getDirect() {
        if (last.y < first.y || last.y == first.y && last.x < first.x) {
            inv.isRectangular = isRectangular;
            return inv;
        } else {
            return this;
        }
    }
}
