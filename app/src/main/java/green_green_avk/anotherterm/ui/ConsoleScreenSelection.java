package green_green_avk.anotherterm.ui;

import android.graphics.Point;

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

    private ConsoleScreenSelection(final ConsoleScreenSelection o) {
        first = o.last;
        last = o.first;
        inv = o;
    }

    public ConsoleScreenSelection getDirect() {
        if (last.y < first.y || last.y == first.y && last.x < first.x) {
            inv.isRectangular = isRectangular;
            return inv;
        } else {
            return this;
        }
    }
}
