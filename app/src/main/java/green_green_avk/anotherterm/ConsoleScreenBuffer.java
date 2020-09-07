package green_green_avk.anotherterm;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import org.apache.commons.collections4.list.TreeList;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import green_green_avk.anotherterm.utils.Unicode;

public final class ConsoleScreenBuffer {
    public static final int MAX_BUF_HEIGHT = 100000;
    private static final int MAX_ROW_MEM = 8192;
    public static final int MAX_ROW_LEN = 1024;
    private static final int MIN_ROW_LEN = 128;
    public static final int DEF_CHAR_ATTRS = encodeAttrs(new ConsoleScreenCharAttrs());

    private static final char[] EMPTY_BUF = new char[0];

    public static final class BufferTextRange {
        @NonNull
        public char[] text = EMPTY_BUF;
        public int start = 0;
        public byte startXOff = 0;
        public int length = 0;
        public byte endXOff = 0;

        public void reinit() {
            text = EMPTY_BUF;
            start = 0;
            startXOff = 0;
            length = 0;
            endXOff = 0;
        }

        @NonNull
        @Override
        public String toString() {
            return new String(text, start, length);
        }
    }

    public static final class BufferRun {
        private int attrsInd = 0;
        @NonNull
        public char[] text = EMPTY_BUF;
        public int start = 0;
        public int length = 0;
        public int attrs = DEF_CHAR_ATTRS;
        public byte glyphWidth = 1;

        public void reinit() {
            text = EMPTY_BUF;
            start = 0;
            length = 0;
            attrs = DEF_CHAR_ATTRS;
            glyphWidth = 1;
            attrsInd = 0;
        }
    }

    private int mWidth;
    private int mHeight;
    private int mBufHeight;

    private static final class Row {
        public char[] text = new char[MIN_ROW_LEN];
        public int[] attrs = new int[MIN_ROW_LEN];
        public int wrapPos = 0;

        {
            Arrays.fill(text, ' ');
        }

        public Row clear(final int a) {
            Arrays.fill(text, ' ');
            Arrays.fill(attrs, a);
            wrapPos = 0;
            return this;
        }

        private int extendText(final int plusSize) {
            final int size = Integer.highestOneBit(text.length + plusSize - 1) << 1;
            final char[] nb = Arrays.copyOf(text, Math.min(size, MAX_ROW_MEM));
            Arrays.fill(text, '\0');
            Arrays.fill(nb, text.length, nb.length, ' ');
            text = nb;
            return text.length - size;
        }

        private int extendAttrs(final int plusSize, final int a) {
            final int size = Integer.highestOneBit(attrs.length + plusSize - 1) << 1;
            final int[] nb = Arrays.copyOf(attrs, Math.min(size, MAX_ROW_MEM));
            Arrays.fill(attrs, 0);
            Arrays.fill(nb, attrs.length, nb.length, a);
            attrs = nb;
            return attrs.length - size;
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
        final int fgColor;
        final int bgColor;
        if (a.inverse) {
            fgColor = a.bgColor;
            bgColor = a.fgColorIndexed ?
                    ConsoleScreenCharAttrs.getBasicColor(a.fgColor) : a.fgColor;
        } else {
            if (a.fgColorIndexed) {
                if (a.bold)
                    fgColor = ConsoleScreenCharAttrs.getBasicColor(a.fgColor | 8);
                else if (a.faint)
                    fgColor = (ConsoleScreenCharAttrs.getBasicColor(a.fgColor) >> 1)
                            & 0xFF7F7F7F | 0xFF000000;
                else fgColor = ConsoleScreenCharAttrs.getBasicColor(a.fgColor);
            } else fgColor = a.fgColor;
            bgColor = a.bgColor;
        }
        return (encodeColor(fgColor) << 20)
                | (encodeColor(bgColor) << 8)
                | (a.bold ? 1 : 0)
                | (a.italic ? 4 : 0)
                | (a.underline ? 8 : 0)
                | (a.blinking ? 16 : 0)
                | (a.invisible ? 64 : 0)
                | (a.crossed ? 128 : 0);
    }

    public static ConsoleScreenCharAttrs decodeAttrs(final int v) {
        ConsoleScreenCharAttrs a = new ConsoleScreenCharAttrs();
        decodeAttrs(v, a);
        return a;
    }

    public static void decodeAttrs(final int v, @NonNull final ConsoleScreenCharAttrs a) {
        a.fgColorIndexed = false;
        a.fgColor = decodeColor(v >> 20);
        a.bgColor = decodeColor(v >> 8);
        a.bold = (v & 1) != 0;
        a.faint = false;
        a.italic = (v & 4) != 0;
        a.underline = (v & 8) != 0;
        a.blinking = (v & 16) != 0;
        a.inverse = false;
        a.invisible = (v & 64) != 0;
        a.crossed = (v & 128) != 0;
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
        if (dh < 0) {
            scroll(dh, defaultAttrs);
            mWidth = w;
            mHeight = h;
            mBufHeight = bh;
            mBuffer.setLimit(mBufHeight);
        } else {
            mWidth = w;
            mHeight = h;
            mBufHeight = bh;
            mBuffer.setLimit(mBufHeight);
            scroll(dh, defaultAttrs);
        }
        resetMargins();
        setAbsPos(mPos.x, mPos.y);
    }

    private static final int F_IND_MASK = 0xFFFFFF;
    private static final int F_XOFF_SHIFT = 24;

    private static int getCharIndexRealloc(@NonNull final char[] text, final int limit,
                                           final int dx, int startInd) {
        int px = startInd >> F_XOFF_SHIFT;
        startInd &= F_IND_MASK;
        while (true) {
            if (startInd >= limit) {
                return startInd * 2 - limit + dx - px; // as whitespaces for realloc
            }
            final int cp = Character.codePointAt(text, startInd);
            final int w = Unicode.wcwidth(cp); // 0 for C0 / C1
            if (px >= dx && w != 0)
                break;
            startInd += Character.charCount(cp);
            px += w;
        }
        return startInd | ((px - dx) << F_XOFF_SHIFT);
    }

    private static int getCharIndex(@NonNull final char[] text, final int limit,
                                    final int dx, final int startInd) {
        final int r = getCharIndexRealloc(text, limit, dx, startInd);
        if (r >> F_XOFF_SHIFT != 0) return r;
        return Math.min(r, limit);
    }

    private static int getCharIndex(@NonNull final char[] text, final int dx, final int startInd) {
        return getCharIndex(text, text.length, dx, startInd);
    }

    public static int getCharIndex(@NonNull final char[] text, final int dx, int startInd,
                                   final boolean next) {
        final int r = getCharIndex(text, dx, startInd);
        if (next || r >> F_XOFF_SHIFT == 0) return r & F_IND_MASK;
        return Unicode.stepBack(text, startInd, r & F_IND_MASK);
    }

    public static int getCharPos(@NonNull final char[] text, final int offset, final int length) {
        return Unicode.getScreenLength(text, offset, length);
    }

    public byte getGlyphWidth(final int x, final int y) {
        if (x < 0 || x > mWidth) return 0;
        final Row row = getRow(y);
        if (row == null) return 0;
        final int idx = getCharIndex(row.text, x, 0, false);
        if (idx >= row.text.length) return 1; // as whitespace
        return (byte) Unicode.wcwidth(Character.codePointAt(row.text, idx));
    }

    public int getChars(final int x, final int y, int len, @NonNull final BufferTextRange output) {
        final Row row = getRow(y);
        if (row == null) return -1;
        len = Math.min(len, mWidth - x);
        if (len <= 0) return -1;
        output.start = getCharIndex(row.text, x, 0);
        final int end = getCharIndex(row.text, len, output.start);
        output.text = row.text;
        output.startXOff = (byte) (output.start >> F_XOFF_SHIFT);
        output.start &= F_IND_MASK;
        if (output.startXOff != 0)
            output.start = Unicode.stepBack(output.text, 0, output.start);
        output.endXOff = (byte) (end >> F_XOFF_SHIFT);
        output.length = (end & F_IND_MASK) - output.start;
        return len;
    }

    private int getAttrs(@NonNull final int[] attrs, final int i) {
        if (i < attrs.length) return attrs[i];
        return attrs[attrs.length - 1];
    }

    private int getCharsRunLength(@NonNull final Row row, final int startX, final int endX,
                                  @NonNull final BufferRun output) {
        final char[] rText = row.text;
        final int[] rAttrs = row.attrs;
        int attrsInd = output.attrsInd;
        final int attrs = getAttrs(rAttrs, attrsInd);
        output.start += output.length;
        int ind = output.start;
        int x = startX;
        int cp;
        byte width;
        if (ind >= rText.length) {
            width = 1;
            x++;
            while (x < endX) {
                if (getAttrs(rAttrs, attrsInd) != attrs) break;
                x++;
                attrsInd++;
            }
        } else {
            cp = Character.codePointAt(rText, ind);
            width = (byte) Unicode.wcwidth(cp);
            ind += Character.charCount(cp);
            attrsInd++;
            x += width;
            while (x < endX) {
                if (getAttrs(rAttrs, attrsInd) != attrs) break;
                if (ind >= rText.length) break;
                cp = Character.codePointAt(rText, ind);
                final byte w = (byte) Unicode.wcwidth(cp);
                if (width == 0) width = w;
                if (w > 0 && w != width) break;
                ind += Character.charCount(cp);
                attrsInd++;
                x += w; // 0 for C0 / C1
            }
        }
        output.text = rText;
        output.attrsInd = attrsInd;
        output.length = ind - output.start;
        output.glyphWidth = width;
        output.attrs = inverseScreen ?
                (attrs >> 12) & 0x000FFF00 | (attrs << 12) & 0xFFF00000 | attrs & 0xFF
                : attrs;
        return x - startX;
    }

    public int getCharsRun(final int x, final int y, int endX,
                           @NonNull final BufferRun output) {
        final Row row = getRow(y);
        if (row == null) return -1;
        endX = Math.min(endX, mWidth);
        if (x >= endX) return -1;
        return getCharsRunLength(row, x, endX, output);
    }

    public int initCharsRun(int x, final int y,
                            @NonNull final BufferRun output) {
        output.reinit();
        final Row row = getRow(y);
        if (row == null) return 0;
        x = Math.min(x, mWidth);
        int i = getCharIndex(row.text, x, 0);
        int r = 0;
        if (i >> F_XOFF_SHIFT != 0) {
            i = Unicode.stepBack(row.text, 0, i & F_IND_MASK);
            r = 1;
        }
        output.attrsInd = Character.codePointCount(row.text, 0, i);
        output.start = i;
        return r;
    }

    public boolean isLineWrapped(final int y) {
        final Row row = getRow(y);
        if (row == null) return false;
        return row.wrapPos == mWidth;
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

    public void moveScrollPosY(final int y) {
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
                pasteChars(row, left, right, value, right - left, 0, currentAttrs);
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
            pasteChars(row, from, to, null, to - from, 0, currentAttrs);
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
        pasteChars(row, x, x, null, n, n, currentAttrs);
    }

    public void eraseChars(final int n) {
        eraseChars(mPos.x, mPos.y, n);
    }

    public void eraseChars(final int x, final int y, final int n) {
        final Row row = getRowForWrite(y);
        if (row == null) return;
        int e = x + n;
        if (e > mWidth) e = mWidth;
        pasteChars(row, x, e, null, e - x, 0, currentAttrs);
    }

    public void deleteChars(final int n) {
        deleteChars(mPos.x, mPos.y, n);
    }

    public void deleteChars(final int x, final int y, int n) {
        final Row row = getRowForWrite(y);
        if (row == null) return;
        if (n > mWidth - x) n = mWidth - x;
        pasteChars(row, x, x + n, null, 0, -n, currentAttrs);
        pasteChars(row, mWidth - n, mWidth, null, n, 0, currentAttrs);
    }

    private static int getNextEndIndex(@NonNull final CharBuffer buf, final int dx, int i) {
        if (buf.hasArray()) {
            final char[] a = buf.array();
            final int offset = buf.arrayOffset() + buf.position();
            i = getCharIndex(a, buf.arrayOffset() + buf.limit(), dx, i + offset);
            return (i >> F_XOFF_SHIFT != 0
                    ? Unicode.stepBack(a, offset, i & F_IND_MASK)
                    : i) - offset;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private int codePointCountRealloc(@NonNull final char[] a, final int offset, final int count) {
        if (offset >= a.length) return offset + count - a.length; // as whitespaces
        if (offset + count > a.length) {
            return Character.codePointCount(a, offset, a.length - offset)
                    + offset + count - a.length; // as whitespaces
        } else {
            return Character.codePointCount(a, offset, count);
        }
    }

    private int codePointCount(@NonNull final char[] a, final int offset, final int count) {
        if (offset >= a.length) return 0;
        if (offset + count > a.length) {
            return Character.codePointCount(a, offset, a.length - offset);
        } else {
            return Character.codePointCount(a, offset, count);
        }
    }

    /**
     * It's... complicated.
     *
     * @param row
     * @param startX
     * @param endX
     * @param buf
     * @param bufLen
     * @param bufDX  - just a hint about screen position difference
     * @param attrs
     */
    private void pasteChars(@NonNull final Row row, final int startX, final int endX,
                            @Nullable final Object buf, final int bufLen, final int bufDX,
                            final int attrs) {
        final int width = Math.min(mWidth, mWidth - bufDX);
        int toStart = getCharIndexRealloc(row.text, row.text.length, startX, 0);
        int toEnd;
        if ((toStart & F_IND_MASK) < row.text.length)
            toEnd = getCharIndex(row.text, endX - startX, toStart);
        else toEnd = toStart;
        if (toStart >> F_XOFF_SHIFT != 0) {
            toStart &= F_IND_MASK;
            toStart = Unicode.stepBack(row.text, 0, toStart);
            row.text[toStart] = ' ';
            toStart++;
        }
        if (toEnd >> F_XOFF_SHIFT != 0) {
            toEnd &= F_IND_MASK;
            toEnd = Unicode.stepBack(row.text, 0, toEnd);
            row.text[toEnd] = ' ';
        }
        final int attrsStart = codePointCountRealloc(row.text, 0, toStart);
        final int attrsEndPrev = attrsStart +
                codePointCount(row.text, toStart, toEnd - toStart);
        int rowEnd;
        final int textNeedExtraLen;
        if (toStart < row.text.length) {
            rowEnd = getCharIndex(row.text, width - endX, toEnd);
            if (rowEnd >> F_XOFF_SHIFT != 0) {
                rowEnd &= F_IND_MASK;
                rowEnd = Unicode.stepBack(row.text, 0, rowEnd);
            }
            textNeedExtraLen = bufLen - toEnd + toStart + rowEnd - row.text.length;
        } else {
            rowEnd = row.text.length;
            textNeedExtraLen = bufLen + toStart - row.text.length;
        }
        if (textNeedExtraLen > 0) {
            rowEnd = row.text.length + textNeedExtraLen;
            if (row.extendText(textNeedExtraLen) < 0) {
                Log.w(this.getClass().getName(), "Memory limit exceeds. Bailing out.");
                return; // TODO: Implement some adaptive degradation for Zalgo lovers.
            }
        }
        if (toEnd - toStart != bufLen) {
            final int e = Math.max(toEnd, toStart + bufLen);
            if (rowEnd < e) rowEnd = e;
            else System.arraycopy(row.text, toEnd, row.text, toStart + bufLen,
                    rowEnd - e);
        }
        if (buf instanceof CharBuffer) {
            ((CharBuffer) buf).get(row.text, toStart, bufLen);
        } else if (buf instanceof Character) {
            Arrays.fill(row.text, toStart, toStart + bufLen, (Character) buf);
        } else {
            Arrays.fill(row.text, toStart, toStart + bufLen, ' ');
        }
        final int attrsEnd = attrsStart + Character.codePointCount(row.text, toStart, bufLen);
        final int attrsRowEnd = attrsEnd + Character.codePointCount(row.text,
                toStart + bufLen, rowEnd - (toStart + bufLen));
        if (attrsRowEnd > row.attrs.length)
            row.extendAttrs(attrsRowEnd - row.attrs.length, defaultAttrs); // It can't fail.
        if (attrsEnd != attrsEndPrev)
            System.arraycopy(row.attrs, attrsEndPrev, row.attrs, attrsEnd,
                    Math.max(attrsRowEnd - Math.max(attrsEnd, attrsEndPrev), 0));
        Arrays.fill(row.attrs, attrsStart, attrsEnd, attrs);
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
        Row row;
        if (wrap && x == mWidth) {
            row = getRowForWrite(y);
            if (row != null) row.wrapPos = mWidth;
        }
        y = moveScrollPosY(y, x / mWidth);
        x %= mWidth;
        row = getRowForWrite(y);
        if (row == null) return 0;
        final CharBuffer buf = s.duplicate();
        int endX = Unicode.getScreenLength(buf, buf.remaining()) + x;
        int len;
        if (endX > mWidth) {
            endX = mWidth;
            len = getNextEndIndex(buf, endX - x, 0);
        } else len = buf.remaining();
        int lenX = endX - x;
        pasteChars(row, x, endX, buf, len, 0, currentAttrs);
        row.wrapPos = 0;
        if (wrap) {
            while (buf.remaining() > 0) {
                row.wrapPos = mWidth;
                y = moveScrollPosY(y, 1);
                row = getRowForWrite(y);
                endX = Unicode.getScreenLength(buf, buf.remaining());
                if (endX > mWidth) {
                    endX = mWidth;
                    len = getNextEndIndex(buf, endX, 0);
                } else len = buf.remaining();
                pasteChars(row, 0, endX, buf, len, 0, currentAttrs);
                row.wrapPos = 0;
                lenX += endX;
            }
            x = endX;
        } else {
            x = endX;
            if (x >= mWidth) x = mWidth - 1;
        }
        endPos.x = x;
        endPos.y = y;
        return lenX;
    }
}
