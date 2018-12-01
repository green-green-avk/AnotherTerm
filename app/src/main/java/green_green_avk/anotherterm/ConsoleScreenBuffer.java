package green_green_avk.anotherterm;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;

import org.apache.commons.collections4.list.TreeList;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public final class ConsoleScreenBuffer {
    public static final int MAX_BUF_HEIGHT = 100000;
    public static final int MAX_ROW_LEN = 1024;
    public static final int DEF_CHAR_ATTRS = encodeAttrs(new ConsoleScreenCharAttrs());

    private int mWidth;
    private int mHeight;
    private int mBufHeight;

    private static final class Row {
        public final char[] text = new char[MAX_ROW_LEN];
        public final int[] attrs = new int[MAX_ROW_LEN];

        {
            Arrays.fill(text, ' ');
        }

        public Row clear(final int a) {
            Arrays.fill(text, ' ');
            Arrays.fill(attrs, a);
            return this;
        }

        @Override
        protected void finalize() throws Throwable {
            clear(DEF_CHAR_ATTRS);
            super.finalize();
        }
    }

    private static final class Buffer {
        private int mLimit = MAX_BUF_HEIGHT;
        private int mPoolSize = 0;
        private final TreeList<Row> mRows = new TreeList<>();

        public void setLimit(final int limit) {
            mLimit = limit;
            if (size() > limit) {
                for (final Row row : mRows.subList(limit, size())) row.clear(0);
                mPoolSize = mRows.size() - limit;
            }
        }

        public int size() {
            return mRows.size() - mPoolSize;
        }

        public void clear() {
            mPoolSize = mRows.size();
            for (final Row row : mRows) {
                row.clear(DEF_CHAR_ATTRS);
            }
        }

        public Row get(final int i) {
            return (i < 0 || i >= size()) ? null : mRows.get(i);
        }

        // [from, to)
        public Iterable<Row> get(final int from, final int to) {
            final int _from;
            final int _to;
            final boolean _reverse;
            if (from > to) {
                _from = to + 1;
                _to = from + 1;
                _reverse = true;
            } else {
                _from = from;
                _to = to;
                _reverse = false;
            }
            return new Iterable<Row>() {
                private final List<Row> ite = mRows.subList(
                        MathUtils.clamp(_from, 0, size()),
                        MathUtils.clamp(_to, 0, size())
                );

                @NonNull
                @Override
                public Iterator<Row> iterator() {
                    return _reverse
                            ? new Iterator<Row>() {
                        private int i = from;
                        private final ListIterator<Row> it = ite.listIterator(ite.size());

                        @Override
                        public boolean hasNext() {
                            return i > to;
                        }

                        @Override
                        public Row next() {
                            final Row r;
                            if (i >= mRows.size() || !it.hasPrevious()) r = null;
                            else r = it.previous();
                            --i;
                            return r;
                        }
                    }
                            : new Iterator<Row>() {
                        private int i = from;
                        private final Iterator<Row> it = ite.iterator();

                        @Override
                        public boolean hasNext() {
                            return i < to;
                        }

                        @Override
                        public Row next() {
                            final Row r;
                            if (i < 0 || !it.hasNext()) r = null;
                            else r = it.next();
                            ++i;
                            return r;
                        }
                    };
                }
            };
        }

        // [from, to]
        // to >= mLimit - remove rows
        public void scroll(int from, int to, int n, final int a) {
            if (n < 1) return;
            from = MathUtils.clamp(from, 0, mLimit - 1);
            to = MathUtils.clamp(to, 0, mLimit);
            if (from == to) return;
            n = MathUtils.clamp(n, 1, Math.abs(from - to) + 1);
            if (mRows.size() == mPoolSize) {
                if (mPoolSize == 0)
                    mRows.add(new Row().clear(a));
                else {
                    mRows.get(0).clear(a);
                    --mPoolSize;
                }
                --n;
            }
            for (; n > 0; --n) {
                final Row row;
                final int count = size();
                if (from >= count) {
                    if (mPoolSize > 0) {
                        row = mRows.remove(mRows.size() - 1);
                        --mPoolSize;
                    } else row = new Row();
                } else row = mRows.remove(from);
                row.clear(a);
                if (to >= count) {
                    mRows.add(row);
                    ++mPoolSize;
                } else mRows.add(to, row);
            }
        }

        public void scroll(final int n, final int a) {
            if (n < 0) scroll(0, mLimit, -n, a);
            else scroll(mLimit - 1, 0, n, a);
        }
    }

    private final Buffer mBuffer = new Buffer();

    public final Point mPos = new Point(0, 0);
    public final Point mPosSaved = new Point(0, 0);
    public final Rect mMargins = new Rect(0, 0, MAX_ROW_LEN - 1, MAX_BUF_HEIGHT - 1);

    public int defaultAttrs;
    public int currentAttrs;
    public boolean wrap = true;
    public boolean inverseScreen = false; // DECSCNM
    public boolean originMode = false;
    public String windowTitle = null;

    private static int encodeColor(final int c) {
        return (Color.red(c) << 4) & 0xF00 | Color.green(c) & 0xF0 | (Color.blue(c) >> 4) & 0xF;
    }

    private static int decodeColor(final int v) {
        return Color.rgb((v >> 4) & 0xF0, v & 0xF0, (v << 4) & 0xF0);
    }

    public static int encodeAttrs(@NonNull final ConsoleScreenCharAttrs a) {
        return (encodeColor(a.fgColor) << 20)
                | (encodeColor(a.bgColor) << 8)
                | (a.bold ? 1 : 0)
                | (a.italic ? 4 : 0)
                | (a.underline ? 8 : 0)
                | (a.blinking ? 16 : 0)
                | (a.inverse ? 64 : 0);
    }

    public static ConsoleScreenCharAttrs decodeAttrs(final int v) {
        ConsoleScreenCharAttrs a = new ConsoleScreenCharAttrs();
        decodeAttrs(v, a);
        return a;
    }

    public static void decodeAttrs(final int v, @NonNull final ConsoleScreenCharAttrs a) {
        a.reset();
        a.fgColor = decodeColor(v >> 20);
        a.bgColor = decodeColor(v >> 8);
        a.bold = (v & 1) != 0;
        a.italic = (v & 4) != 0;
        a.underline = (v & 8) != 0;
        a.blinking = (v & 16) != 0;
        a.inverse = (v & 64) != 0;
    }

    private int toBufY(final int y) {
        return mHeight - y - 1;
    }

    private int fromBufY(final int by) {
        return toBufY(by);
    }

    private boolean isInScreenY(final int y) {
        return y >= 0 && y < mHeight;
    }

    private Row getRow(final int y) {
        return mBuffer.get(toBufY(y));
    }

    private Iterable<Row> getRows(final int from, final int to) {
        return mBuffer.get(toBufY(from), toBufY(to));
    }

    private Row getRowForWrite(final int y) {
        if (!isInScreenY(y)) return null;
        return getRow(y);
    }

    private Iterable<Row> getRowsForWrite(final int from, final int to) {
        return getRows(from, to);
    }

    public interface OnScroll {
        void onScroll(@NonNull ConsoleScreenBuffer buf, int from, int to, int n);
    }

    private OnScroll mOnScroll = null;

    public void setOnScroll(@Nullable final OnScroll onScroll) {
        mOnScroll = onScroll;
    }

    private void callOnScroll(final int from, final int to, final int n) {
        if (mOnScroll != null) mOnScroll.onScroll(this, from, to, n);
    }

    // [from, to]
    private void scroll(final int from, final int to, final int n, final int a) {
        mBuffer.scroll(toBufY(from), toBufY(to), n, a);
        final int top = mHeight - mBufHeight;
        if (n > 0)
            callOnScroll(
                    MathUtils.clamp(from, top, mHeight - 1),
                    MathUtils.clamp(to, top, mHeight - 1),
                    n);
    }

    // the whole buffer in both directions (including history)
    private void scroll(final int n, final int a) {
        final boolean init = mBuffer.size() == 0; // more general calculation is not required here
        mBuffer.scroll(n, a);
        final int top = mHeight - mBufHeight;
        if ((!init) && (n != 0)) {
            if (n < 0) callOnScroll(mHeight - 1, top, -n);
            else callOnScroll(top, mHeight - 1, n);
        }
    }

    private int moveScrollPosY(final int from, final int len) {
        int r = from + len;
        final int top = Math.max(0, mMargins.top);
        final int bottom = Math.min(mHeight - 1, mMargins.bottom);
        if (from >= top && r < top) {
            scroll(mMargins.bottom, top, top - r, currentAttrs);
            r = top;
        } else if (from <= bottom && r > bottom) {
            scroll(mMargins.top, mMargins.bottom, r - bottom, currentAttrs);
            r = mMargins.bottom;
        }
        return MathUtils.clamp(r, 0, mHeight - 1);
    }

    public ConsoleScreenBuffer(final int w, final int h, final int bh) {
        this(w, h, bh, DEF_CHAR_ATTRS);
    }

    public ConsoleScreenBuffer(final int w, final int h, final int bh,
                               @NonNull final ConsoleScreenCharAttrs da) {
        this(w, h, bh, encodeAttrs(da));
    }

    public ConsoleScreenBuffer(final int w, final int h, final int bh, final int da) {
        if (w < 1 || w > MAX_ROW_LEN || h < 1 || h > bh || bh > MAX_BUF_HEIGHT)
            throw new IllegalArgumentException();
        mWidth = w;
        mHeight = h;
        mBufHeight = bh;
        defaultAttrs = da;
        currentAttrs = da;
        mBuffer.setLimit(mBufHeight);
        clear();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getMaxBufferHeight() {
        return mBufHeight;
    }

    public int getBufferHeight() {
        return mBuffer.size();
    }

    public int getScrollableHeight() {
        return MathUtils.clamp(mBuffer.size() - mHeight, 0, mBufHeight);
    }

    public int limitX(final int v) {
        return MathUtils.clamp(v, 0, mWidth - 1);
    }

    public int limitY(final int v) {
        return MathUtils.clamp(v, 0, mHeight - 1);
    }

    public void clear() {
        mBuffer.clear();
        scroll(mHeight, defaultAttrs);
    }

    public void resize(final int w, final int h) {
        resize(w, h, mBufHeight);
    }

    public void resize(final int w, final int h, final int bh) {
        if (w < 1 || w > MAX_ROW_LEN || h < 1 || h > bh || bh > MAX_BUF_HEIGHT) return;
        int dh = h - mHeight;
        final int dc = h - 1 - mPos.y;
        if (dc < 0) dh -= dc;
        mWidth = w;
        mHeight = h;
        mBufHeight = bh;
        mBuffer.setLimit(mBufHeight);
        scroll(dh, defaultAttrs);
        resetMargins();
        setAbsPos(mPos.x, mPos.y);
    }

    public CharSequence getChars(final int x, final int y, int len) {
        final Row row = getRow(y);
        if (row == null) return null;
        len = Math.min(len, mWidth - x);
        if (len <= 0) return null;
        return CharBuffer.wrap(row.text, x, len);
    }

    private int getSameAttrLen(@NonNull final int[] attrs, final int start, final int end) {
        final int v = attrs[start];
        int pos = start + 1;
        for (; pos < end; ++pos) {
            if (attrs[pos] != v) break;
        }
        return pos - start;
    }

    public CharSequence getCharsSameAttr(final int x, final int y, int endY) {
        final Row row = getRow(y);
        if (row == null) return null;
        endY = Math.min(endY, mWidth);
        if (x >= endY) return null;
        return CharBuffer.wrap(row.text, x, getSameAttrLen(row.attrs, x, endY));
    }

    public char getChar(final int x, final int y) {
        if (x < 0 || x >= mWidth) return ' ';
        final Row row = getRow(y);
        if (row == null) return ' ';
        return row.text[x];
    }

    public ConsoleScreenCharAttrs getAttrs(final int x, final int y) {
        final ConsoleScreenCharAttrs a = decodeAttrs(getAttrsN(x, y));
        if (inverseScreen) a.inverse = !a.inverse;
        return a;
    }

    public void getAttrs(final int x, final int y, @NonNull final ConsoleScreenCharAttrs a) {
        decodeAttrs(getAttrsN(x, y), a);
        if (inverseScreen) a.inverse = !a.inverse;
    }

    public int getAttrsN(final int x, final int y) {
        if (x < 0 || x >= mWidth) return defaultAttrs;
        final Row row = getRow(y);
        if (row == null) return defaultAttrs;
        return row.attrs[x];
    }

    public int getAbsPosX() {
        return limitX(mPos.x);
    }

    public int getAbsPosY() {
        return limitY(mPos.y);
    }

    public int getPosX() {
        return getAbsPosX();
    }

    public int getPosY() {
        if (originMode && isInScreenY(mMargins.top)) return getAbsPosY() - mMargins.top;
        return getAbsPosY();
    }

    public void setAbsPosX(final int x) {
        mPos.x = limitX(x);
    }

    public void setAbsPosY(final int y) {
        mPos.y = limitY(y);
    }

    public void setAbsPos(final int x, final int y) {
        setAbsPosX(x);
        setAbsPosY(y);
    }

    public void setPosX(final int x) {
        setAbsPosX(x);
    }

    public void setPosY(final int y) {
        if (originMode && isInScreenY(mMargins.top)) setAbsPosY(y + mMargins.top);
        else setAbsPosY(y);
    }

    public void setPos(final int x, final int y) {
        setPosX(x);
        setPosY(y);
    }

    public void moveAbsPosX(final int x) {
        mPos.x = limitX(limitX(mPos.x) + x);
    }

    public void moveAbsPosY(final int y) {
        mPos.y = limitY(mPos.y + y);
    }

    public void moveAbsPos(final int x, final int y) {
        moveAbsPosX(x);
        moveAbsPosY(y);
    }

    public void movePosX(final int x) {
        moveAbsPosX(x);
    }

    public void movePosY(int y) {
        if (mPos.y >= mMargins.top && mPos.y <= mMargins.bottom)
            y = MathUtils.clamp(mPos.y + y, mMargins.top, mMargins.bottom);
        mPos.y = limitY(y);
    }

    public void movePos(final int x, final int y) {
        movePosX(x);
        movePosY(y);
    }

    public void moveScrollPosY(int y) {
        mPos.y = moveScrollPosY(mPos.y, y);
    }

    public void savePos() {
        mPosSaved.set(mPos.x, mPos.y);
    }

    public void restorePos() {
        setPos(mPosSaved.x, mPosSaved.y);
    }

    public void setPos(@NonNull final ConsoleScreenBuffer csb) {
        mPos.set(csb.mPos.x, csb.mPos.y);
        mPosSaved.set(csb.mPosSaved.x, csb.mPosSaved.y);
    }

    public void setTBMargins(final int top, final int bottom) {
        if (top >= bottom || top < 0 || bottom >= mHeight) {
            resetMargins();
            return;
        }
        mMargins.top = top;
        mMargins.bottom = bottom;
    }

    public void resetMargins() {
        mMargins.set(0, Integer.MIN_VALUE / 2,
                MAX_ROW_LEN - 1, Integer.MAX_VALUE / 2);
    }

    public void setMargins(@NonNull final ConsoleScreenBuffer csb) {
        mMargins.set(csb.mMargins);
    }

    public void setDefaultAttrs(ConsoleScreenCharAttrs da) {
        defaultAttrs = encodeAttrs(da);
    }

    public void getCurrentAttrs(ConsoleScreenCharAttrs ca) {
        decodeAttrs(currentAttrs, ca);
    }

    public void setCurrentAttrs(ConsoleScreenCharAttrs ca) {
        currentAttrs = encodeAttrs(ca);
    }

    public void fillAll(final char value) {
        fill(0, 0, mWidth, mHeight, value);
    }

    public void fill(final int left, final int top, final int right, final int bottom,
                     final char value) {
        for (final Row row : getRowsForWrite(top, bottom)) {
            if (row != null) {
                Arrays.fill(row.text, left, right, value);
                Arrays.fill(row.attrs, left, right, currentAttrs);
            }
        }
    }

    public void eraseAll() {
        scroll(mHeight, currentAttrs);
    }

    public void eraseLines(final int from, final int to) {
        for (final Row row : getRowsForWrite(from, to)) {
            if (row != null) {
                Arrays.fill(row.attrs, currentAttrs);
                Arrays.fill(row.text, ' ');
            }
        }
    }

    public void eraseAbove() {
        eraseLines(0, mPos.y);
        eraseLineLeft();
    }

    public void eraseBelow() {
        eraseLineRight();
        eraseLines(mPos.y + 1, mHeight);
    }

    public void eraseLine(int from, int to, final int y) {
        if (to > mWidth) to = mWidth;
        if (from >= to) return;
        final Row row = getRowForWrite(y);
        if (row != null) {
            Arrays.fill(row.attrs, from, to, currentAttrs);
            Arrays.fill(row.text, from, to, ' ');
        }
    }

    public void eraseLineAll() {
        eraseLine(0, mWidth, mPos.y);
    }

    public void eraseLineLeft() {
        eraseLine(0, mPos.x + 1, mPos.y);
    }

    public void eraseLineRight() {
        eraseLine(mPos.x, mWidth, mPos.y);
    }

    public void insertLine(final int n) {
        insertLine(mPos.y, n);
    }

    public void insertLine(final int y, final int n) {
        if (!isInScreenY(y)) return;
        scroll(mMargins.bottom, y, n, currentAttrs);
    }

    public void deleteLine(final int n) {
        deleteLine(mPos.y, n);
    }

    public void deleteLine(final int y, final int n) {
        if (!isInScreenY(y)) return;
        scroll(y, mMargins.bottom, n, currentAttrs);
    }

    public void scrollUp(final int n) {
        scroll(MathUtils.clamp(mMargins.top, 0, mHeight - 1),
                MathUtils.clamp(mMargins.bottom, 0, mHeight - 1),
                n, currentAttrs);
    }

    public void scrollDown(final int n) {
        scroll(MathUtils.clamp(mMargins.bottom, 0, mHeight - 1),
                MathUtils.clamp(mMargins.top, 0, mHeight - 1),
                n, currentAttrs);
    }

    public void insertChars(final int n) {
        insertChars(mPos.x, mPos.y, n);
    }

    public void insertChars(final int x, final int y, int n) {
        final Row row = getRowForWrite(y);
        if (row == null) return;
        if (n > mWidth - x) n = mWidth - x;
        System.arraycopy(row.text, x, row.text, x + n, mWidth - x - n);
        Arrays.fill(row.text, x, x + n, ' ');
        System.arraycopy(row.attrs, x, row.attrs, x + n, mWidth - x - n);
        Arrays.fill(row.attrs, x, x + n, currentAttrs);
    }

    public void eraseChars(final int n) {
        eraseChars(mPos.x, mPos.y, n);
    }

    public void eraseChars(final int x, final int y, final int n) {
        final Row row = getRowForWrite(y);
        if (row == null) return;
        int e = x + n;
        if (e > mWidth) e = mWidth;
        Arrays.fill(row.text, x, e, ' ');
        Arrays.fill(row.attrs, x, e, currentAttrs);
    }

    public void deleteChars(final int n) {
        deleteChars(mPos.x, mPos.y, n);
    }

    public void deleteChars(final int x, final int y, int n) {
        final Row row = getRowForWrite(y);
        if (row == null) return;
        if (n > mWidth - x) n = mWidth - x;
        System.arraycopy(row.text, x + n, row.text, x, mWidth - x - n);
        Arrays.fill(row.text, mWidth - n, mWidth, ' ');
        System.arraycopy(row.attrs, x + n, row.attrs, x, mWidth - x - n);
        Arrays.fill(row.attrs, mWidth - n, mWidth, currentAttrs);
    }

    public int setChars(@NonNull final String s) {
        return setChars(mPos.x, mPos.y, s, mPos);
    }

    public int setChars(@NonNull final CharBuffer s) {
        return setChars(mPos.x, mPos.y, s, mPos);
    }

    public int setChars(final int x, final int y, @NonNull final String s, final Point endPos) {
        return setChars(x, y, CharBuffer.wrap(s), endPos);
    }

    public int setChars(int x, int y, @NonNull final CharBuffer s, @NonNull final Point endPos) {
        y = moveScrollPosY(y, x / mWidth);
        x %= mWidth;
        Row row = getRowForWrite(y);
        if (row == null) return 0;
        final CharBuffer buf = s.duplicate();
        int end = Math.min(buf.remaining() + x, mWidth);
        int len = end - x;
        buf.get(row.text, x, len);
        Arrays.fill(row.attrs, x, end, currentAttrs);
        if (wrap) {
            while (buf.remaining() > 0) {
                y = moveScrollPosY(y, 1);
                row = getRowForWrite(y);
                end = Math.min(buf.remaining(), mWidth);
                buf.get(row.text, 0, end);
                Arrays.fill(row.attrs, 0, end, currentAttrs);
                len += end;
            }
            x = end;
        } else {
            x = end;
            if (x >= mWidth) x = mWidth - 1;
        }
        endPos.x = x;
        endPos.y = y;
        return len;
    }

    public void setChar(final int x, final int y, final char c) {
        setChar(x, y, c, currentAttrs);
    }

    public void setChar(final int x, final int y, final char c,
                        @NonNull final ConsoleScreenCharAttrs a) {
        setChar(x, y, c, encodeAttrs(a));
    }

    public void setChar(int x, int y, final char c, final int a) {
        y = moveScrollPosY(y, x / mWidth);
        x %= mWidth;
        final Row row = getRowForWrite(y);
        if (row == null) return;
        row.text[x] = c;
        row.attrs[x] = a;
    }
}
