package green_green_avk.anotherterm;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import org.apache.commons.collections4.list.TreeList;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.anotherterm.utils.Unicode;

public final class ConsoleScreenBuffer {
    public static final int MAX_BUF_HEIGHT = 100000;
    private static final int MAX_ROW_MEM = 8192;
    public static final int MAX_ROW_LEN = 1024;
    private static final int MIN_ROW_LEN = 128;
    public static final int DEF_FG_CHAR_ATTRS = encodeFgAttrs(new ConsoleScreenCharAttrs());
    public static final int DEF_BG_CHAR_ATTRS = encodeBgAttrs(new ConsoleScreenCharAttrs());

    private static final char[] EMPTY_BUF = new char[0];

    public static final class BufferTextRange {
        @NonNull
        public char[] text = EMPTY_BUF;
        public int start = 0;
        public byte startXOff = 0;
        public int length = 0;
        public byte endXOff = 0;

        public void trimStart() {
            int i = start;
            final int end = i + length;
            for (; i < end && text[i] == ' '; i++) ;
            length = end - i;
            start = i;
        }

        public void trimEnd() {
            int i = start + length - 1;
            for (; i >= start && text[i] == ' '; i--) ;
            length = i + 1 - start;
        }

        public void appendTo(@NonNull final StringBuilder sb) {
            sb.append(text, start, length);
        }

        @Override
        @NonNull
        public String toString() {
            return new String(text, start, length);
        }

        @NonNull
        public CharBuffer toBuffer() {
            return CharBuffer.wrap(text, start, length);
        }
    }

    public static final class BufferRun {
        private int fgAttrsOff;
        private int bgAttrsOff;
        @NonNull
        public char[] text;
        public int start;
        public int length;
        public int fgAttrs;
        public int bgAttrs;
        public byte glyphWidth;

        {
            init();
        }

        public void init() {
            text = EMPTY_BUF;
            start = 0;
            length = 0;
            fgAttrs = DEF_FG_CHAR_ATTRS;
            bgAttrs = DEF_BG_CHAR_ATTRS;
            glyphWidth = 1;
            fgAttrsOff = 0;
            bgAttrsOff = 0;
        }
    }

    private int mWidth;
    private int mHeight;
    private int mBufHeight;

    private static final class Row {
        public char[] text = new char[MIN_ROW_LEN];
        public char[] fgAttrs = new char[MIN_ROW_LEN];
        public char[] bgAttrs = new char[MIN_ROW_LEN];
        public int wrapPos = 0;

        public static int getAttrsLength(final int attrs) {
            return 1 + (attrs >>> 7 & 1);
        }

        public static int getAttrs(@NonNull final char[] attrs, final int i) {
            final int a = attrs[i];
            if ((a & 0x80) == 0x80)
                return a | attrs[i + 1] << 16;
            return a;
        }

        public static int getAttrsOffset(@NonNull final char[] attrs, final int start, int n) {
            int i = start;
            while (n > 0) {
                if (i >= attrs.length)
                    break;
                i += getAttrsLength(attrs[i]);
                n--;
            }
            return i;
        }

        public static int getAttrsOffsetRealloc(@NonNull final char[] attrs, final int start,
                                                int n, final int defAttrs) {
            int i = start;
            while (n > 0) {
                if (i >= attrs.length)
                    return i + n * getAttrsLength(defAttrs);
                i += getAttrsLength(attrs[i]);
                n--;
            }
            return i;
        }

        public static int getAttrsOffsetOrLast(@NonNull final char[] attrs, final int start,
                                               int n) {
            int i = start;
            while (n > 0) {
                final int d = getAttrsLength(attrs[i]);
                if (i + d >= attrs.length || attrs[i + d] == 0xC0) // Exclude the last no-op attribute.
                    break;
                i += d;
                n--;
            }
            return i;
        }

        public static void fillAttrs(@NonNull final char[] attrs, final int v) {
            fillAttrs(attrs, 0, attrs.length, v);
        }

        public static void fillAttrs(@NonNull final char[] attrs, final int start, final int end,
                                     final int v) {
            if ((v & 0x80) == 0x80) {
                attrs[start] = (char) v;
                attrs[start + 1] = (char) (v >>> 16);
                if ((end - start & 1) != 0) {
                    attrs[end - 1] = 0xC0; // Mark as no-op if a long attribute does not fit.
                    Misc.repeatFill(attrs, start, end - 1, 2);
                } else {
                    Misc.repeatFill(attrs, start, end, 2);
                }
            } else {
                Arrays.fill(attrs, start, end, (char) v);
            }
        }

        {
            Arrays.fill(text, ' ');
        }

        public Row clear(final int fg, final int bg) {
            Arrays.fill(text, ' ');
            fillAttrs(fgAttrs, fg);
            fillAttrs(bgAttrs, bg);
            wrapPos = 0;
            return this;
        }

        private static int adjustSize(final int size) {
            return Integer.highestOneBit(size - 1) << 1;
        }

        private int extendText(final int plusSize) {
            final int size = adjustSize(text.length + plusSize);
            final char[] nb = Arrays.copyOf(text, Math.min(size, MAX_ROW_MEM));
            Arrays.fill(text, '\0');
            Arrays.fill(nb, text.length, nb.length, ' ');
            text = nb;
            return text.length - size;
        }

        // TODO: Refactor this crap!
        private static int getAttrsLength(@NonNull final char[] attrs) {
            int i = 0;
            while (i < attrs.length && attrs[i] != 0xC0) {
                i += getAttrsLength(attrs[i]);
            }
            return i;
        }

        private int extendFgAttrs(final int plusSize, final int a) {
            final char[] attrs = fgAttrs;
            final int size = adjustSize(attrs.length + plusSize);
            final char[] nb = Arrays.copyOf(attrs, Math.min(size, MAX_ROW_MEM << 1));
            final int oldSize = getAttrsLength(attrs);
            fillAttrs(attrs, 0);
            fillAttrs(nb, oldSize, nb.length, a);
            fgAttrs = nb;
            return attrs.length - size;
        }

        private int extendBgAttrs(final int plusSize, final int a) {
            final char[] attrs = bgAttrs;
            final int size = adjustSize(attrs.length + plusSize);
            final char[] nb = Arrays.copyOf(attrs, Math.min(size, MAX_ROW_MEM << 1));
            final int oldSize = getAttrsLength(attrs);
            fillAttrs(attrs, 0);
            fillAttrs(nb, oldSize, nb.length, a);
            bgAttrs = nb;
            return attrs.length - size;
        }

        @Override
        protected void finalize() throws Throwable {
            clear(DEF_FG_CHAR_ATTRS, DEF_BG_CHAR_ATTRS);
            super.finalize();
        }
    }

    private static final class Buffer {
        private int mLimit = MAX_BUF_HEIGHT;
        private int mPoolSize = 0;
        private final TreeList<Row> mRows = new TreeList<>();

        public void setLimit(final int limit) {
            mLimit = limit;
            crop(limit);
        }

        public int size() {
            return mRows.size() - mPoolSize;
        }

        public void clear() {
            mPoolSize = mRows.size();
            for (final Row row : mRows) {
                row.clear(DEF_FG_CHAR_ATTRS, DEF_BG_CHAR_ATTRS);
            }
        }

        public void crop(final int lines) {
            if (size() > lines) {
                for (final Row row : mRows.subList(lines, size())) {
                    row.clear(DEF_FG_CHAR_ATTRS, DEF_BG_CHAR_ATTRS);
                }
                mPoolSize = mRows.size() - lines;
            }
        }

        public void optimize() {
            if (mPoolSize > 0) {
                // ??? mRows.subList(size(), mRows.size()).clear();
                // Seems more efficient:
                for (int i = mPoolSize; i > 0; i--) {
                    mRows.remove(mRows.size() - 1);
                }
                mPoolSize = 0;
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
                            if (i >= mRows.size() || !it.hasPrevious())
                                r = null;
                            else
                                r = it.previous();
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
                            if (i < 0 || !it.hasNext())
                                r = null;
                            else
                                r = it.next();
                            ++i;
                            return r;
                        }
                    };
                }
            };
        }

        // [from, to]
        // to >= mLimit - remove rows
        public void scroll(int from, int to, int n, final int fga, final int bga) {
            if (n < 1)
                return;
            from = MathUtils.clamp(from, 0, mLimit - 1);
            to = MathUtils.clamp(to, 0, mLimit);
            if (from == to)
                return;
            n = MathUtils.clamp(n, 1, Math.abs(from - to) + 1);
            if (mRows.size() == mPoolSize) {
                if (mPoolSize == 0)
                    mRows.add(new Row().clear(fga, bga));
                else {
                    mRows.get(0).clear(fga, bga);
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
                    } else
                        row = new Row();
                } else
                    row = mRows.remove(from);
                row.clear(fga, bga);
                if (to >= count) {
                    mRows.add(row);
                    ++mPoolSize;
                } else
                    mRows.add(to, row);
            }
        }

        public void scroll(final int n, final int fga, final int bga) {
            if (n < 0)
                scroll(0, mLimit, -n, fga, bga);
            else
                scroll(mLimit - 1, 0, n, fga, bga);
        }
    }

    private final Buffer mBuffer = new Buffer();

    public final Point mPos = new Point(0, 0);
    public final Point mPosSaved = new Point(0, 0);
    public final Rect mMargins = new Rect(0, 0, MAX_ROW_LEN - 1, MAX_BUF_HEIGHT - 1);

    public int defaultFgAttrs;
    public int defaultBgAttrs;
    public int currentFgAttrs;
    public int currentBgAttrs;
    public boolean wrap = true;
    public boolean screenInverse = false; // DECSCNM
    public boolean originMode = false;
    public String windowTitle = null;

    private static int encodeColor(@NonNull final ConsoleScreenCharAttrs.Color v) {
        return v.value << 8
                | (v.type == ConsoleScreenCharAttrs.Color.Type.TRUE ? 0x80 : 0)
                | (v.type == ConsoleScreenCharAttrs.Color.Type._8BIT ? 0x40 : 0);
    }

    public static void decodeColor(@NonNull final ConsoleScreenCharAttrs.Color out, final int v) {
        if ((v & 0x80) != 0) {
            out.type = ConsoleScreenCharAttrs.Color.Type.TRUE;
            out.value = v >>> 8 | 0xFF000000;
        } else if ((v & 0x40) != 0) {
            out.type = ConsoleScreenCharAttrs.Color.Type._8BIT;
            out.value = v >>> 8 & 0xFF;
        } else {
            out.type = ConsoleScreenCharAttrs.Color.Type.BASIC;
            out.value = v >>> 8 & 0xFF;
        }
    }

    public static int encodeFgAttrs(@NonNull final ConsoleScreenCharAttrs v) {
        return encodeColor(v.fgColor)
                | (v.bold ? 0x01 : 0)
                | (v.faint ? 0x02 : 0)
                | (v.italic ? 0x04 : 0)
                | (v.blinking ? 0x08 : 0)
                | (v.invisible ? 0x10 : 0);
    }

    public static void decodeFgAttrs(@NonNull final ConsoleScreenCharAttrs out, final int v) {
        out.bold = (v & 0x01) != 0;
        out.faint = (v & 0x02) != 0;
        out.italic = (v & 0x04) != 0;
        out.blinking = (v & 0x08) != 0;
        out.invisible = (v & 0x10) != 0;
        decodeColor(out.fgColor, v);
    }

    public static int encodeBgAttrs(@NonNull final ConsoleScreenCharAttrs v) {
        return encodeColor(v.bgColor)
                | (v.underline ? 1 : 0)
                | (v.crossed ? 2 : 0)
                | (v.inverse ? 4 : 0);
    }

    public static void decodeBgAttrs(@NonNull final ConsoleScreenCharAttrs out, final int v) {
        out.underline = (v & 0x01) != 0;
        out.crossed = (v & 0x02) != 0;
        out.inverse = (v & 0x04) != 0;
        decodeColor(out.bgColor, v);
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
        if (mOnScroll != null)
            mOnScroll.onScroll(this, from, to, n);
    }

    // [from, to]
    private void scroll(final int from, final int to, final int n, final int fga, final int bga) {
        mBuffer.scroll(toBufY(from), toBufY(to), n, fga, bga);
        final int top = mHeight - mBufHeight;
        if (n > 0)
            callOnScroll(
                    MathUtils.clamp(from, top, mHeight - 1),
                    MathUtils.clamp(to, top, mHeight - 1),
                    n);
    }

    // the whole buffer in both directions (including history)
    private void scroll(final int n, final int fga, final int bga) {
        final boolean init = mBuffer.size() == 0; // more general calculation is not required here
        mBuffer.scroll(n, fga, bga);
        final int top = mHeight - mBufHeight;
        if ((!init) && (n != 0)) {
            if (n < 0)
                callOnScroll(mHeight - 1, top, -n);
            else
                callOnScroll(top, mHeight - 1, n);
        }
    }

    private int moveScrollPosY(final int from, final int len) {
        int r = from + len;
        final int top = Math.max(0, mMargins.top);
        final int bottom = Math.min(mHeight - 1, mMargins.bottom);
        if (from >= top && r < top) {
            scroll(mMargins.bottom, top, top - r,
                    currentFgAttrs, currentBgAttrs);
            r = top;
        } else if (from <= bottom && r > bottom) {
            scroll(mMargins.top, mMargins.bottom, r - bottom,
                    currentFgAttrs, currentBgAttrs);
            r = mMargins.bottom;
        }
        return MathUtils.clamp(r, 0, mHeight - 1);
    }

    public ConsoleScreenBuffer(
            @IntRange(from = 1, to = MAX_ROW_LEN) final int w,
            @IntRange(from = 1, to = MAX_BUF_HEIGHT) final int h,
            @IntRange(from = 1, to = MAX_BUF_HEIGHT) final int bh
    ) {
        this(w, h, bh, DEF_FG_CHAR_ATTRS, DEF_BG_CHAR_ATTRS);
    }

    public ConsoleScreenBuffer(
            @IntRange(from = 1, to = MAX_ROW_LEN) final int w,
            @IntRange(from = 1, to = MAX_BUF_HEIGHT) final int h,
            @IntRange(from = 1, to = MAX_BUF_HEIGHT) final int bh,
            @NonNull final ConsoleScreenCharAttrs da
    ) {
        this(w, h, bh, encodeFgAttrs(da), encodeBgAttrs(da));
    }

    public ConsoleScreenBuffer(
            @IntRange(from = 1, to = MAX_ROW_LEN) final int w,
            @IntRange(from = 1, to = MAX_BUF_HEIGHT) final int h,
            @IntRange(from = 1, to = MAX_BUF_HEIGHT) final int bh,
            final int dfga, final int dbga
    ) {
        if (w < 1 || w > MAX_ROW_LEN || h < 1 || h > bh || bh > MAX_BUF_HEIGHT)
            throw new IllegalArgumentException();
        mWidth = w;
        mHeight = h;
        mBufHeight = bh;
        defaultFgAttrs = dfga;
        currentFgAttrs = dfga;
        defaultBgAttrs = dbga;
        currentBgAttrs = dbga;
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

    public void setMaxBufferHeight(final int bh) {
        resize(mWidth, mHeight, bh);
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
        scroll(mHeight, defaultFgAttrs, defaultBgAttrs);
    }

    public void resize(final int w, final int h) {
        resize(w, h, mBufHeight);
    }

    public void resize(int w, int h, int bh) {
        w = MathUtils.clamp(w, 1, MAX_ROW_LEN);
        h = MathUtils.clamp(h, 1, MAX_BUF_HEIGHT);
        bh = MathUtils.clamp(bh, h, MAX_BUF_HEIGHT);
        _resize(w, h, bh);
    }

    public void resizeStrict(final int w, final int h) {
        resizeStrict(w, h, mBufHeight);
    }

    public void resizeStrict(final int w, final int h, final int bh) {
        if (w < 1 || w > MAX_ROW_LEN || h < 1 || h > bh || bh > MAX_BUF_HEIGHT)
            throw new IllegalArgumentException();
        _resize(w, h, bh);
    }

    private void _resize(final int w, final int h) {
        _resize(w, h, mBufHeight);
    }

    private void _resize(final int w, final int h, final int bh) {
        int dh = h - mHeight;
        final int dc = h - 1 - mPos.y;
        if (dc < 0) dh -= dc;
        if (dh < 0) {
            scroll(dh, defaultFgAttrs, defaultBgAttrs);
            mWidth = w;
            mHeight = h;
            mBufHeight = bh;
            mBuffer.setLimit(mBufHeight);
        } else {
            mWidth = w;
            mHeight = h;
            mBufHeight = bh;
            mBuffer.setLimit(mBufHeight);
            scroll(dh, defaultFgAttrs, defaultBgAttrs);
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
        return startInd | px - dx << F_XOFF_SHIFT;
    }

    private static int getCharIndexRealloc(@NonNull final CharSequence text, final int limit,
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
        return startInd | px - dx << F_XOFF_SHIFT;
    }

    private static int getCharIndex(@NonNull final CharSequence text, final int limit,
                                    final int dx, final int startInd) {
        final int r = getCharIndexRealloc(text, limit, dx, startInd);
        if (r >> F_XOFF_SHIFT != 0)
            return r;
        return Math.min(r, limit);
    }

    private static int getCharIndex(@NonNull final char[] text, final int limit,
                                    final int dx, final int startInd) {
        final int r = getCharIndexRealloc(text, limit, dx, startInd);
        if (r >> F_XOFF_SHIFT != 0)
            return r;
        return Math.min(r, limit);
    }

    private static int getCharIndex(@NonNull final char[] text, final int dx, final int startInd) {
        return getCharIndex(text, text.length, dx, startInd);
    }

    public static int getCharIndex(@NonNull final char[] text, final int dx, final int startInd,
                                   final boolean next) {
        final int r = getCharIndex(text, dx, startInd);
        if (next || r >> F_XOFF_SHIFT == 0)
            return r & F_IND_MASK;
        return stepGlyphBack(text, startInd, r & F_IND_MASK);
    }

    public int getCharIndex(final int y, final int dx, final int startInd,
                            final boolean next) {
        final char[] text = getChars(y);
        if (text == null)
            return startInd + dx;
        return getCharIndex(text, dx, startInd, next);
    }

    public static int getCharPos(@NonNull final char[] text, final int offset, final int length) {
        return Unicode.getScreenLength(text, offset, length);
    }

    public int getCharPos(final int y, final int offset, final int length) {
        final char[] text = getChars(y);
        if (text == null)
            return length;
        return getCharPos(text, offset, length);
    }

    public byte getGlyphWidth(final int x, final int y) {
        if (x < 0 || x > mWidth)
            return 0;
        final Row row = getRow(y);
        if (row == null)
            return 0;
        final int idx = getCharIndex(row.text, x, 0, false);
        if (idx >= row.text.length)
            return 1; // as whitespace
        return (byte) Unicode.wcwidth(Character.codePointAt(row.text, idx));
    }

    public int getChars(final int x, final int y, int len, @NonNull final BufferTextRange output) {
        final Row row = getRow(y);
        if (row == null)
            return -1;
        len = Math.min(len, mWidth - x);
        if (len <= 0)
            return -1;
        output.start = getCharIndex(row.text, x, 0);
        final int end = getCharIndex(row.text, len, output.start);
        output.text = row.text;
        output.startXOff = (byte) (output.start >> F_XOFF_SHIFT);
        output.start &= F_IND_MASK;
        if (output.startXOff != 0)
            output.start = stepGlyphBack(output.text, 0, output.start);
        output.endXOff = (byte) (end >> F_XOFF_SHIFT);
        output.length = (end & F_IND_MASK) - output.start;
        return len;
    }

    @Nullable
    public char[] getChars(final int y) {
        final Row row = getRow(y);
        if (row == null)
            return null;
        return row.text;
    }

    private int getCharsRunLength(@NonNull final Row row, final int startX, final int endX,
                                  @NonNull final BufferRun output) {
        final char[] rText = row.text;
        final char[] rFgAttrs = row.fgAttrs;
        final char[] rBgAttrs = row.bgAttrs;
        int fgAttrsOff = output.fgAttrsOff;
        int bgAttrsOff = output.bgAttrsOff;
        final int fgAttrs = Row.getAttrs(rFgAttrs, fgAttrsOff);
        final int bgAttrs = Row.getAttrs(rBgAttrs, bgAttrsOff);
        output.start += output.length;
        int ind = output.start;
        int x = startX;
        int cp;
        byte width;
        if (ind >= rText.length) {
            width = 1;
            x++;
            while (x < endX) {
                if (Row.getAttrs(rFgAttrs, fgAttrsOff) != fgAttrs ||
                        Row.getAttrs(rBgAttrs, bgAttrsOff) != bgAttrs)
                    break;
                x++;
                fgAttrsOff = Row.getAttrsOffsetOrLast(rFgAttrs, fgAttrsOff, 1);
                bgAttrsOff = Row.getAttrsOffsetOrLast(rBgAttrs, bgAttrsOff, 1);
            }
        } else {
            cp = Character.codePointAt(rText, ind);
            width = (byte) Unicode.wcwidth(cp);
            ind += Character.charCount(cp);
            fgAttrsOff = Row.getAttrsOffsetOrLast(rFgAttrs, fgAttrsOff, 1);
            if (width != 0)
                bgAttrsOff = Row.getAttrsOffsetOrLast(rBgAttrs, bgAttrsOff, 1);
            x += width;
            while (x < endX) {
                if (Row.getAttrs(rFgAttrs, fgAttrsOff) != fgAttrs ||
                        Row.getAttrs(rBgAttrs, bgAttrsOff) != bgAttrs)
                    break;
                if (ind >= rText.length)
                    break;
                cp = Character.codePointAt(rText, ind);
                final byte w = (byte) Unicode.wcwidth(cp);
                if (width == 0)
                    width = w;
                if (w > 0 && w != width)
                    break;
                ind += Character.charCount(cp);
                fgAttrsOff = Row.getAttrsOffsetOrLast(rFgAttrs, fgAttrsOff, 1);
                if (w != 0)
                    bgAttrsOff = Row.getAttrsOffsetOrLast(rBgAttrs, bgAttrsOff, 1);
                x += w; // 0 for C0 / C1
            }
        }
        output.text = rText;
        output.fgAttrsOff = fgAttrsOff;
        output.bgAttrsOff = bgAttrsOff;
        output.length = ind - output.start;
        output.glyphWidth = width;
        output.fgAttrs = fgAttrs;
        output.bgAttrs = bgAttrs;
        return x - startX;
    }

    public int getCharsRun(final int x, final int y, int endX,
                           @NonNull final BufferRun output) {
        final Row row = getRow(y);
        if (row == null)
            return -1;
        endX = Math.min(endX, mWidth);
        if (x >= endX)
            return -1;
        return getCharsRunLength(row, x, endX, output);
    }

    public int initCharsRun(int x, final int y,
                            @NonNull final BufferRun output) {
        output.init();
        final Row row = getRow(y);
        if (row == null)
            return 0;
        x = Math.min(x, mWidth);
        int i = getCharIndex(row.text, x, 0);
        int r = 0;
        if (i >> F_XOFF_SHIFT != 0) {
            i = stepGlyphBack(row.text, 0, i & F_IND_MASK);
            r = 1;
        }
        output.fgAttrsOff = Row.getAttrsOffsetOrLast(row.fgAttrs, 0,
                Character.codePointCount(row.text, 0, i));
        output.bgAttrsOff = Row.getAttrsOffsetOrLast(row.bgAttrs, 0,
                Unicode.getGlyphCount(row.text, 0, i));
        output.start = i;
        return r;
    }

    public boolean isLineWrapped(final int y) {
        final Row row = getRow(y);
        return row != null && row.wrapPos == mWidth;
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
        if (originMode && isInScreenY(mMargins.top))
            return getAbsPosY() - mMargins.top;
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
        if (originMode && isInScreenY(mMargins.top))
            setAbsPosY(y + mMargins.top);
        else
            setAbsPosY(y);
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

    public void setDefaultAttrs(final ConsoleScreenCharAttrs da) {
        defaultFgAttrs = encodeFgAttrs(da);
        defaultBgAttrs = encodeBgAttrs(da);
    }

    public void getCurrentAttrs(final ConsoleScreenCharAttrs ca) {
        decodeFgAttrs(ca, currentFgAttrs);
        decodeBgAttrs(ca, currentBgAttrs);
    }

    public void setCurrentAttrs(final ConsoleScreenCharAttrs ca) {
        currentFgAttrs = encodeFgAttrs(ca);
        currentBgAttrs = encodeBgAttrs(ca);
    }

    /**
     * Single width symbols only.
     */
    public void fillAll(final char value) {
        fill(0, 0, mWidth, mHeight, value);
    }

    /**
     * Single width symbols only.
     */
    public void fill(final int left, final int top, final int right, final int bottom,
                     final char value) {
        for (final Row row : getRowsForWrite(top, bottom)) {
            if (row != null) {
                pasteChars(row, left, right,
                        value, right - left, 0,
                        currentFgAttrs, currentBgAttrs);
            }
        }
    }

    public void eraseAll() { // Current screen
        eraseLines(0, mHeight);
    }

    public void eraseAllSaved() { // History
        mBuffer.crop(mHeight);
    }

    public void eraseLines(final int from, final int to) {
        for (final Row row : getRowsForWrite(from, to)) {
            if (row != null) {
                Row.fillAttrs(row.bgAttrs, currentBgAttrs);
                Row.fillAttrs(row.fgAttrs, currentFgAttrs);
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

    public void eraseLine(final int from, int to, final int y) {
        if (to > mWidth)
            to = mWidth;
        if (from >= to)
            return;
        final Row row = getRowForWrite(y);
        if (row != null) {
            pasteChars(row, from, to,
                    null, to - from, 0,
                    currentFgAttrs, currentBgAttrs);
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
        if (!isInScreenY(y))
            return;
        scroll(mMargins.bottom, y, n, currentFgAttrs, currentBgAttrs);
    }

    public void deleteLine(final int n) {
        deleteLine(mPos.y, n);
    }

    public void deleteLine(final int y, final int n) {
        if (!isInScreenY(y))
            return;
        scroll(y, mMargins.bottom, n, currentFgAttrs, currentBgAttrs);
    }

    public void scrollUp(final int n) {
        scroll(MathUtils.clamp(mMargins.top, 0, mHeight - 1),
                MathUtils.clamp(mMargins.bottom, 0, mHeight - 1),
                n, currentFgAttrs, currentBgAttrs);
    }

    public void scrollDown(final int n) {
        scroll(MathUtils.clamp(mMargins.bottom, 0, mHeight - 1),
                MathUtils.clamp(mMargins.top, 0, mHeight - 1),
                n, currentFgAttrs, currentBgAttrs);
    }

    public void insertChars(final int n) {
        insertChars(mPos.x, mPos.y, n);
    }

    public void insertChars(final int x, final int y, int n) {
        final Row row = getRowForWrite(y);
        if (row == null)
            return;
        if (n > mWidth - x)
            n = mWidth - x;
        pasteChars(row, x, x,
                null, n, n,
                currentFgAttrs, currentBgAttrs);
    }

    public void eraseChars(final int n) {
        eraseChars(mPos.x, mPos.y, n);
    }

    public void eraseChars(final int x, final int y, final int n) {
        final Row row = getRowForWrite(y);
        if (row == null)
            return;
        int e = x + n;
        if (e > mWidth)
            e = mWidth;
        pasteChars(row, x, e,
                null, e - x, 0,
                currentFgAttrs, currentBgAttrs);
    }

    public void deleteChars(final int n) {
        deleteChars(mPos.x, mPos.y, n);
    }

    public void deleteChars(final int x, final int y, int n) {
        final Row row = getRowForWrite(y);
        if (row == null)
            return;
        if (n > mWidth - x)
            n = mWidth - x;
        pasteChars(row, x, x + n,
                null, 0, -n,
                currentFgAttrs, currentBgAttrs);
        pasteChars(row, mWidth - n, mWidth,
                null, n, 0,
                currentFgAttrs, currentBgAttrs);
    }

    private static int getNextEndIndex(@NonNull final CharBuffer buf, final int dx, int i) {
        // Speed up.
        if (buf.hasArray()) {
            final char[] a = buf.array();
            final int offset = buf.arrayOffset() + buf.position();
            i = getCharIndex(a, buf.arrayOffset() + buf.limit(), dx, i + offset);
            return (i >> F_XOFF_SHIFT != 0
                    ? stepGlyphBack(a, offset, i & F_IND_MASK)
                    : i) - offset;
        }
        i = getCharIndex(buf, buf.limit(), dx, i + buf.position());
        return (i >> F_XOFF_SHIFT != 0
                ? stepGlyphBack(buf, buf.position(), i & F_IND_MASK)
                : i) - buf.position();
    }

    private static int codePointCountRealloc(@NonNull final char[] a,
                                             final int offset, final int count) {
        if (offset >= a.length)
            return offset + count - a.length; // as whitespaces
        if (offset + count > a.length) {
            return Character.codePointCount(a, offset, a.length - offset)
                    + offset + count - a.length; // as whitespaces
        } else {
            return Character.codePointCount(a, offset, count);
        }
    }

    private static int codePointCount(@NonNull final char[] a, final int offset, final int count) {
        if (offset >= a.length)
            return 0;
        if (offset + count > a.length) {
            return Character.codePointCount(a, offset, a.length - offset);
        } else {
            return Character.codePointCount(a, offset, count);
        }
    }

    private static int glyphCountRealloc(@NonNull final char[] a,
                                         final int offset, final int count) {
        if (offset >= a.length)
            return offset + count - a.length; // as whitespaces
        if (offset + count > a.length) {
            return Unicode.getGlyphCount(a, offset, a.length - offset)
                    + offset + count - a.length; // as whitespaces
        } else {
            return Unicode.getGlyphCount(a, offset, count);
        }
    }

    private static int glyphCount(@NonNull final char[] a, final int offset, final int count) {
        if (offset >= a.length)
            return 0;
        if (offset + count > a.length) {
            return Unicode.getGlyphCount(a, offset, a.length - offset);
        } else {
            return Unicode.getGlyphCount(a, offset, count);
        }
    }

    private static int stepGlyphBack(@NonNull final char[] buf,
                                     final int startLimit, final int ptr) {
        int p = ptr;
        while (p > startLimit) {
            p = Unicode.stepBack(buf, startLimit, p);
            if (Unicode.wcwidth(Character.codePointAt(buf, p)) != 0)
                break;
        }
        return p;
    }

    private static int stepGlyphBack(@NonNull final CharSequence buf,
                                     final int startLimit, final int ptr) {
        int p = ptr;
        while (p > startLimit) {
            p = Unicode.stepBack(buf, startLimit, p);
            if (Unicode.wcwidth(Character.codePointAt(buf, p)) != 0)
                break;
        }
        return p;
    }

    /**
     * <i>Efficient ≠ clean in Java! Take this. Or use C++.</i>
     *
     * @param row     to paste into
     * @param startX  of area to paste over
     * @param endX    of area to paste over
     * @param buf     to paste
     *                (its {@link CharBuffer#position} will be incremented by {@code bufLen})
     * @param bufLen  how many chars to insert
     * @param bufDX   difference in columns between the substitution and the initial area
     *                (purely for speed up)
     * @param fgAttrs foreground attributes
     * @param bgAttrs background attributes
     */
    private void pasteChars(@NonNull final Row row, final int startX, final int endX,
                            @Nullable final Object buf, final int bufLen, final int bufDX,
                            final int fgAttrs, final int bgAttrs) {
        final int width = Math.min(mWidth, mWidth - bufDX);
        int toStart = getCharIndexRealloc(row.text, row.text.length, startX, 0);
        int toEnd;
        if ((toStart & F_IND_MASK) < row.text.length)
            toEnd = getCharIndex(row.text, endX - startX, toStart);
        else
            toEnd = toStart;
        final int startPadding = toStart >> F_XOFF_SHIFT;
        if (startPadding != 0) {
            toStart &= F_IND_MASK;
            toStart = stepGlyphBack(row.text, 0, toStart);
            // TODO: Max glyph width is 2 at the moment, attention needed.
        }
        final int endPadding = toEnd >> F_XOFF_SHIFT;
        if (endPadding != 0) {
            toEnd &= F_IND_MASK;
        }
        final int paddedBufLen = bufLen + startPadding + endPadding;
        final int fgAttrsStart = Row.getAttrsOffsetRealloc(row.fgAttrs, 0,
                codePointCountRealloc(row.text, 0, toStart),
                defaultFgAttrs);
        final int bgAttrsStart = Row.getAttrsOffsetRealloc(row.bgAttrs, 0,
                glyphCountRealloc(row.text, 0, toStart),
                defaultBgAttrs);
        final int fgAttrsEndPrev = Row.getAttrsOffset(row.fgAttrs, fgAttrsStart,
                codePointCount(row.text, toStart, toEnd - toStart));
        final int bgAttrsEndPrev = Row.getAttrsOffset(row.bgAttrs, bgAttrsStart,
                glyphCount(row.text, toStart, toEnd - toStart));
        int rowEnd;
        final int textNeedExtraLen;
        if (toStart < row.text.length) {
            rowEnd = getCharIndex(row.text, width - endX - endPadding, toEnd);
            if (rowEnd >> F_XOFF_SHIFT != 0) {
                rowEnd &= F_IND_MASK;
                rowEnd = stepGlyphBack(row.text, 0, rowEnd);
            }
            rowEnd += Math.max(paddedBufLen - toEnd + toStart, 0);
            textNeedExtraLen = rowEnd - row.text.length;
        } else {
            rowEnd = row.text.length;
            textNeedExtraLen = paddedBufLen + toStart - row.text.length;
        }
        if (textNeedExtraLen > 0) {
            rowEnd = row.text.length + textNeedExtraLen;
            if (row.extendText(textNeedExtraLen) < 0) {
                Log.w(this.getClass().getName(), "Memory limit exceeds. Bailing out.");
                return; // TODO: Implement some adaptive degradation for Zalgo lovers.
            }
        }
        if (toEnd - toStart != paddedBufLen) {
            final int e = Math.max(toEnd, toStart + paddedBufLen);
            if (rowEnd < e)
                rowEnd = e;
            else
                System.arraycopy(row.text, toEnd,
                        row.text, toStart + paddedBufLen,
                        rowEnd - e);
        }
        if (startPadding != 0)
            Arrays.fill(row.text, toStart, toStart + startPadding, ' ');
        if (buf instanceof CharBuffer) {
            ((CharBuffer) buf).get(row.text, toStart + startPadding, bufLen);
        } else if (buf instanceof Character) {
            Arrays.fill(row.text, toStart + startPadding,
                    toStart + startPadding + bufLen, (Character) buf);
        } else {
            Arrays.fill(row.text, toStart + startPadding,
                    toStart + startPadding + bufLen, ' ');
        }
        if (endPadding != 0)
            Arrays.fill(row.text, toStart + startPadding + bufLen,
                    toStart + paddedBufLen, ' ');
        final int fgAttrsEnd = fgAttrsStart + Character.codePointCount(row.text,
                toStart, bufLen) * Row.getAttrsLength(fgAttrs)
                + startPadding + endPadding;
        final int bgAttrsEnd = bgAttrsStart + Unicode.getGlyphCount(row.text,
                toStart, bufLen) * Row.getAttrsLength(bgAttrs)
                + startPadding + endPadding;
        final int fgAttrsRowEndPrev = Row.getAttrsOffsetRealloc(row.fgAttrs,
                fgAttrsEndPrev, Character.codePointCount(row.text,
                        toStart + paddedBufLen, rowEnd - (toStart + paddedBufLen)),
                defaultFgAttrs);
        final int bgAttrsRowEndPrev = Row.getAttrsOffsetRealloc(row.bgAttrs,
                bgAttrsEndPrev, Unicode.getGlyphCount(row.text,
                        toStart + paddedBufLen, rowEnd - (toStart + paddedBufLen)),
                defaultBgAttrs);
        final int fgAttrsRowEnd = fgAttrsRowEndPrev + fgAttrsEnd - fgAttrsEndPrev;
        final int bgAttrsRowEnd = bgAttrsRowEndPrev + bgAttrsEnd - bgAttrsEndPrev;
        if (fgAttrsRowEnd > row.fgAttrs.length)
            row.extendFgAttrs(fgAttrsRowEnd - row.fgAttrs.length, defaultFgAttrs); // It can't fail.
        if (bgAttrsRowEnd > row.bgAttrs.length)
            row.extendBgAttrs(bgAttrsRowEnd - row.bgAttrs.length, defaultBgAttrs); // It can't fail.
        if (fgAttrsEnd != fgAttrsEndPrev)
            System.arraycopy(row.fgAttrs, fgAttrsEndPrev,
                    row.fgAttrs, fgAttrsEnd,
                    Math.max(fgAttrsRowEnd - Math.max(fgAttrsEnd, fgAttrsEndPrev), 0));
        if (bgAttrsEnd != bgAttrsEndPrev)
            System.arraycopy(row.bgAttrs, bgAttrsEndPrev,
                    row.bgAttrs, bgAttrsEnd,
                    Math.max(bgAttrsRowEnd - Math.max(bgAttrsEnd, bgAttrsEndPrev), 0));
        Row.fillAttrs(row.fgAttrs, fgAttrsStart, fgAttrsEnd, fgAttrs);
        Row.fillAttrs(row.bgAttrs, bgAttrsStart, bgAttrsEnd, bgAttrs);
    }

    public int insertChars(@NonNull final CharSequence s) {
        return setChars(mPos.x, mPos.y, s, true, mPos);
    }

    public int insertChars(@NonNull final CharBuffer s) {
        return setChars(mPos.x, mPos.y, s, true, mPos);
    }

    public int setChars(@NonNull final CharSequence s) {
        return setChars(mPos.x, mPos.y, s, false, mPos);
    }

    public int setChars(@NonNull final CharBuffer s) {
        return setChars(mPos.x, mPos.y, s, false, mPos);
    }

    public int setChars(final int x, final int y, @NonNull final CharSequence s,
                        final boolean insert, @NonNull final Point endPos) {
        if (s instanceof CharBuffer)
            return setChars(x, y, (CharBuffer) s, insert, endPos);
        return setChars(x, y, CharBuffer.wrap(s), insert, endPos);
    }

    public int setChars(int x, int y, @NonNull final CharBuffer s,
                        final boolean insert, @NonNull final Point endPos) {
        Row row;
        if (wrap && x == mWidth) {
            row = getRowForWrite(y);
            if (row != null)
                row.wrapPos = mWidth;
        }
        y = moveScrollPosY(y, x / mWidth);
        x %= mWidth;
        row = getRowForWrite(y);
        if (row == null)
            return 0;
        final CharBuffer buf = s.duplicate();
        int endX = Unicode.getScreenLength(buf, buf.remaining()) + x;
        int len;
        boolean _i = false;
        if (endX > mWidth) {
            endX = mWidth;
            len = getNextEndIndex(buf, endX - x, 0);
        } else {
            len = buf.remaining();
            _i = insert;
        }
        int lenX = endX - x;
        if (_i) {
            pasteChars(row, x, x, buf, len, lenX,
                    currentFgAttrs, currentBgAttrs);
        } else {
            pasteChars(row, x, endX, buf, len, 0,
                    currentFgAttrs, currentBgAttrs);
        }
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
                } else {
                    len = buf.remaining();
                    _i = insert;
                }
                if (_i) {
                    pasteChars(row, 0, 0, buf, len, endX,
                            currentFgAttrs, currentBgAttrs);
                } else {
                    pasteChars(row, 0, endX, buf, len, 0,
                            currentFgAttrs, currentBgAttrs);
                }
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

    public void optimize() {
        mBuffer.optimize();
    }
}
