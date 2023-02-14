package green_green_avk.anotherterm.utils;

import android.graphics.Point;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import java.nio.CharBuffer;

// There is no practical reason in native code for non-regexp search;
// ART and Dalvik are equally good with primitives.

public final class CharsFinder {

    public interface BufferView {
        @NonNull
        LineView get(int y);

        /**
         * @return top boundary (lowest index / inclusive)
         */
        int getTop();

        /**
         * @return bottom boundary (highest index / exclusive)
         */
        int getBottom();
    }

    public static final class LineView {
        @NonNull
        final CharBuffer text;
        final boolean isWrapped;

        public LineView(@NonNull final CharBuffer text, final boolean isWrapped) {
            this.text = text;
            this.isWrapped = isWrapped;
        }
    }

    public CharsFinder(@NonNull final BufferView buffer,
                       @NonNull final String pattern, final boolean noCase) {
        this.buffer = buffer;
        setPattern(pattern, noCase);
    }

    public void setPattern(@NonNull final String pattern, final boolean noCase) {
        this.pattern = noCase ? pattern.toLowerCase() : pattern;
        this.noCase = noCase;
    }

    @NonNull
    private final BufferView buffer;

    @NonNull
    private String pattern;
    private boolean noCase;

    private final Point start = new Point();
    private final Point ptr = new Point();
    private boolean noMatch = true;

    private char getChar(@NonNull final CharBuffer text, final int pos) {
        if (pos == text.length())
            return '\n';
        final char c = text.charAt(pos);
        return noCase ? Character.toLowerCase(c) : c;
    }

    @Nullable
    private LineView stepBw(@NonNull final Point p, @NonNull final LineView currLine) {
        p.x--;
        if (p.x < 0) {
            final int _pY = p.y;
            while (true) {
                p.y--;
                if (p.y < buffer.getTop()) {
                    p.x++;
                    p.y = _pY;
                    return null;
                }
                final LineView newLine = buffer.get(p.y);
                if (newLine.isWrapped && newLine.text.length() <= 0) continue;
                p.x = newLine.text.length();
                if (newLine.isWrapped) p.x--;
                return newLine;
            }
        }
        return currLine;
    }

    @Nullable
    private LineView stepFw(@NonNull final Point p, @NonNull final LineView currLine) {
        p.x++;
        int len = currLine.text.length();
        if (!currLine.isWrapped) len++;
        if (p.x >= len) {
            final int _pY = p.y;
            while (true) {
                p.y++;
                if (p.y >= buffer.getBottom()) {
                    if (p.x == currLine.text.length()) {
                        p.y = _pY;
                        return currLine;
                    }
                    p.x--;
                    p.y = _pY;
                    return null;
                }
                final LineView newLine = buffer.get(p.y);
                if (newLine.isWrapped && newLine.text.length() <= 0) continue;
                p.x = 0;
                return newLine;
            }
        }
        return currLine;
    }

    private boolean matchUp(@NonNull LineView currLine) {
        for (int i = pattern.length() - 1; i >= 0; i--) {
            currLine = stepBw(ptr, currLine);
            if (currLine == null) {
                noMatch = true;
                return true;
            }
            if (pattern.charAt(i) != getChar(currLine.text, ptr.x)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchDown(@NonNull LineView currLine) {
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) != getChar(currLine.text, ptr.x)) {
                return false;
            }
            currLine = stepFw(ptr, currLine);
            if (currLine == null) {
                noMatch = true;
                return true;
            }
        }
        return true;
    }

    @Nullable
    private LineView _setPos(final int x, final int y, final boolean after) {
        if (buffer.getTop() >= buffer.getBottom()) return null;
        final Point p = new Point();
        p.y = MathUtils.clamp(y, buffer.getTop(), buffer.getBottom() - 1);
        LineView currLine = buffer.get(p.y);
        p.x = MathUtils.clamp(x, 0, currLine.text.length() - 1);
        if (after)
            currLine = stepFw(p, currLine);
        if (currLine != null) {
            ptr.set(p.x, p.y);
            start.set(p.x, p.y);
        }
        return currLine;
    }

    /**
     * @param x char index in line
     * @param y line coordinate
     * @return false on error
     */
    public boolean setPos(final int x, final int y, final boolean after) {
        return _setPos(x, y, after) != null;
    }

    public boolean searchUp() {
        if (pattern.isEmpty() || buffer.getTop() >= buffer.getBottom()) return false;
        LineView currLine = buffer.get(ptr.y);
        noMatch = false;
        while (!matchUp(currLine)) {
            ptr.x = start.x;
            ptr.y = start.y;
            currLine = stepBw(ptr, currLine);
            if (currLine == null) {
                noMatch = true;
                return false;
            }
            start.x = ptr.x;
            start.y = ptr.y;
        }
        return !noMatch;
    }

    public boolean searchDown() {
        if (pattern.isEmpty() || buffer.getTop() >= buffer.getBottom()) return false;
        LineView currLine = buffer.get(ptr.y);
        noMatch = false;
        while (!matchDown(currLine)) {
            ptr.x = start.x;
            ptr.y = start.y;
            currLine = stepFw(ptr, currLine);
            if (currLine == null) {
                noMatch = true;
                return false;
            }
            start.x = ptr.x;
            start.y = ptr.y;
        }
        return !noMatch;
    }

    /**
     * @return beginning of the match in chars, inclusive
     */
    @CheckResult
    @NonNull
    public Point getFirst() {
        if (start.y < ptr.y) return start;
        if (start.y > ptr.y) return ptr;
        if (start.x <= ptr.x) return start;
        return ptr;
    }

    @CheckResult
    @NonNull
    private Point getLastRaw() {
        if (start.y < ptr.y) return ptr;
        if (start.y > ptr.y) return start;
        if (start.x <= ptr.x) return ptr;
        return start;
    }

    /**
     * @return ending of the match in chars, exclusive
     */
    @CheckResult
    @NonNull
    public Point getLast() {
        final Point last = new Point(getLastRaw());
        final LineView lw = stepBw(last, buffer.get(last.y));
        if (lw != null && last.x < lw.text.length())
            last.x++;
//        if (last.x <= 0)
//            throw new IllegalStateException();
        return last;
    }
}
