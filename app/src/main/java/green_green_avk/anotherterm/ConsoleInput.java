package green_green_avk.anotherterm;

import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.utils.BinderInputStream;
import green_green_avk.anotherterm.utils.BytesSink;
import green_green_avk.anotherterm.utils.Compat;
import green_green_avk.anotherterm.utils.DecTerminalCharsets;
import green_green_avk.anotherterm.utils.EscCsi;
import green_green_avk.anotherterm.utils.EscOsc;
import green_green_avk.anotherterm.utils.InputTokenizer;
import green_green_avk.anotherterm.utils.Misc;

public final class ConsoleInput implements BytesSink {
    private static final boolean LOG_UNKNOWN_ESC = false; // BuildConfig.DEBUG

    public ConsoleOutput consoleOutput = null;
    public EventBasedBackendModuleWrapper backendModule = null;
    public ConsoleScreenBuffer currScrBuf;
    private final ConsoleScreenBuffer mainScrBuf;
    private final ConsoleScreenBuffer altScrBuf;
    private final ConsoleScreenCharAttrs mCurrAttrs = new ConsoleScreenCharAttrs();
    private int numBellEvents = 0;
    private final BinderInputStream mInputBuf = new BinderInputStream(1024);
    private InputStreamReader mStrConv;
    private final InputTokenizer mInputTokenizer = new InputTokenizer();

    public boolean insertMode = false;
    public boolean cursorVisibility = true; // DECTECM
    public int defaultTabLength = 8;
    public final TreeSet<Integer> tabPositions = new TreeSet<>();
    public final char[][] decGxCharsets = new char[][]{null, null, null, null};
    public int decGlCharset = 0;

    private final ConsoleScreenCharAttrs mSavedCurrAttrs = new ConsoleScreenCharAttrs();
    private final char[][] mSavedDecGxCharsets = new char[][]{null, null, null, null};
    private int mSavedDecGlCharset = 0;

    public boolean numLed = false;
    public boolean capsLed = false;
    public boolean scrollLed = false;

    private final Deque<String> windowTitles = new LinkedList<>();

    {
        final ConsoleScreenBuffer.OnScroll h = new ConsoleScreenBuffer.OnScroll() {
            @Override
            public void onScroll(@NonNull final ConsoleScreenBuffer buf,
                                 final int from, final int to, final int n) {
                if (buf == currScrBuf) dispatchOnBufferScroll(from, to, n);
            }
        };
        mainScrBuf = new ConsoleScreenBuffer(80, 24, 10000);
        altScrBuf = new ConsoleScreenBuffer(80, 24, 24);
        mainScrBuf.setOnScroll(h);
        altScrBuf.setOnScroll(h);
        currScrBuf = mainScrBuf;
        setCharset(Charset.defaultCharset());
    }

    private void sendBack(final String v) {
        if (consoleOutput != null)
            consoleOutput.feed(v);
    }

    public void setCharset(@NonNull final Charset ch) {
        mStrConv = new InputStreamReader(mInputBuf, ch);
    }

    public interface OnInvalidateSink {
        void onInvalidateSink(@Nullable Rect rect);

        void onInvalidateSinkResize(int cols, int rows);
    }

    private final Set<OnInvalidateSink> mOnInvalidateSink =
            Collections.newSetFromMap(new WeakHashMap<>());

    public void addOnInvalidateSink(@NonNull final OnInvalidateSink h) {
        mOnInvalidateSink.add(h);
    }

    public void removeOnInvalidateSink(@NonNull final OnInvalidateSink h) {
        mOnInvalidateSink.remove(h);
    }

    public void invalidateSink() {
        for (final OnInvalidateSink h : mOnInvalidateSink)
            h.onInvalidateSink(null);
    }

    private void invalidateSinkResize(final int cols, final int rows) {
        for (final OnInvalidateSink h : mOnInvalidateSink)
            h.onInvalidateSinkResize(cols, rows);
    }

    public interface OnBufferScroll {
        void onBufferScroll(int from, int to, int n);
    }

    private Set<OnBufferScroll> mOnBufferScroll =
            Collections.newSetFromMap(new WeakHashMap<>());

    public void addOnBufferScroll(@NonNull final OnBufferScroll h) {
        mOnBufferScroll.add(h);
    }

    public void removeOnBufferScroll(@NonNull final OnBufferScroll h) {
        mOnBufferScroll.remove(h);
    }

    private void dispatchOnBufferScroll(int from, int to, int n) {
        for (final OnBufferScroll h : mOnBufferScroll) {
            h.onBufferScroll(from, to, n);
        }
    }

    public void reset() {
        mCurrAttrs.reset();
        currScrBuf.clear();
        currScrBuf.setDefaultAttrs(mCurrAttrs);
        currScrBuf.setCurrentAttrs(mCurrAttrs);
    }

    public void resize(int w, int h) {
        final int ow = mainScrBuf.getWidth();
        final int oh = mainScrBuf.getHeight();
        mainScrBuf.resize(w, h);
        w = mainScrBuf.getWidth();
        h = mainScrBuf.getHeight();
        altScrBuf.resize(w, h, h);
        if (w == ow && h == oh) return;
        if (backendModule != null)
            backendModule.resize(w, h, w * 16, h * 16);
        invalidateSinkResize(w, h);
    }

    public boolean isAltBuf() {
        return currScrBuf == altScrBuf;
    }

    public int getBell() {
        final int r = numBellEvents;
        numBellEvents = 0;
        return r;
    }

    public void setWindowTitle(@NonNull final String v) {
        mainScrBuf.windowTitle = v;
        altScrBuf.windowTitle = v;
    }

    private void pushWindowTitle() {
        windowTitles.push(currScrBuf.windowTitle);
    }

    private void popWindowTitle() {
        if (!windowTitles.isEmpty()) setWindowTitle(windowTitles.pop());
    }

    private int zao(final int v) {
        return v < 1 ? 1 : v;
    }

    private void cr() {
        currScrBuf.setPosX(0);
    }

    private void lf() {
        currScrBuf.setCurrentAttrs(mCurrAttrs);
        currScrBuf.moveScrollPosY(1);
    }

    private void ind() {
        currScrBuf.setCurrentAttrs(mCurrAttrs);
        currScrBuf.moveScrollPosY(1);
    }

    private void ri() {
        currScrBuf.setCurrentAttrs(mCurrAttrs);
        currScrBuf.moveScrollPosY(-1);
    }

    private void bs() {
        currScrBuf.movePosX(-1);
    }

    private void tab(int v) {
        if (v == 0) return;
        int pos = currScrBuf.getPosX();
        final int d;
        final NavigableSet<Integer> tt;
        if (v > 0) {
            d = defaultTabLength;
            tt = tabPositions.tailSet(pos, false);
        } else {
            d = -defaultTabLength;
            tt = tabPositions.headSet(pos, false).descendingSet();
            v = -v;
        }
        // Standard Java and Android libraries does not have a proper algorithm for it...
        // As long as Apache Commons...
        // TODO: TreeList with random access by ordered position should be implemented.
        // I.e., the same tree with two descending criteria.
        if (v < tt.size()) {
            final Iterator<Integer> i = tt.iterator();
            while (v > 0) {
                pos = i.next();
                --v;
            }
        } else {
            if (tt.size() > 0) {
                pos = tt.last();
                v -= tt.size();
            }
            if (v != 0) {
                pos += d * v;
                pos -= pos % d;
            }
        }
        currScrBuf.setPosX(pos);
    }

    private void tabClear(final int pos) {
        if (pos < 0) {
            tabPositions.clear();
            return;
        }
        tabPositions.remove(pos);
    }

    private void tabSet(final int pos) {
        tabPositions.add(pos);
    }

    private void setDecCharset(final int i, final char c) {
        switch (c) {
            case '0':
                decGxCharsets[i] = DecTerminalCharsets.graphics;
                break;
            case 'A':
                decGxCharsets[i] = DecTerminalCharsets.UK;
                break;
            default:
                decGxCharsets[i] = null;
        }
    }

    private void saveCursor() {
        currScrBuf.savePos();
        mSavedCurrAttrs.set(mCurrAttrs);
        mSavedDecGlCharset = decGlCharset;
        System.arraycopy(decGxCharsets, 0, mSavedDecGxCharsets, 0,
                decGxCharsets.length);
    }

    private void restoreCursor() {
        System.arraycopy(mSavedDecGxCharsets, 0, decGxCharsets, 0,
                decGxCharsets.length);
        decGlCharset = mSavedDecGlCharset;
        mCurrAttrs.set(mSavedCurrAttrs);
        currScrBuf.restorePos();
    }

    @Override
    public void feed(@NonNull final ByteBuffer v) {
        if (v.remaining() == 0) return;
        mInputBuf.bind(v);
        try {
            while (mStrConv.ready()) {
                mInputTokenizer.tokenize(mStrConv);
                for (final InputTokenizer.Token t : mInputTokenizer) {
//                    Log.v("CtrlSeq/Note", t.type + ": " + t.value.toString());
                    switch (t.type) {
                        case OSC: {
                            final EscOsc osc = new EscOsc(t.value);
                            if (osc.args.length == 2) {
                                switch (osc.getIntArg(0, -1)) {
                                    case 0:
                                    case 2:
                                        setWindowTitle(osc.args[1]);
                                        break;
                                }
                                break;
                            }
                            if (LOG_UNKNOWN_ESC)
                                Log.w("CtrlSeq", "OSC: " + osc.body);
                            break;
                        }
                        case CSI:
                            parseCsi(new EscCsi(t.value));
                            break;
                        case ESC:
                            switch (t.value.charAt(1)) {
                                case 'D': // IND
                                    ind();
                                    break;
                                case 'E': // NEL
                                    ind();
                                    cr();
                                    break;
                                case 'H': // HTS
                                    tabSet(currScrBuf.getPosX());
                                    break;
                                case 'M': // RI (Reverse linefeed)
                                    ri();
                                    break;
                                case 'c':
                                    reset();
                                    break;
                                case '7':
                                    saveCursor();
                                    break;
                                case '8':
                                    restoreCursor();
                                    break;
                                case '>':
                                    if (consoleOutput != null)
                                        consoleOutput.appNumKeys = false;
                                    break;
                                case '=':
                                    if (consoleOutput != null)
                                        consoleOutput.appNumKeys = true;
                                    break;
                                case '(':
                                    setDecCharset(0, t.value.charAt(2));
                                    break;
                                case ')':
                                    setDecCharset(1, t.value.charAt(2));
                                    break;
                                case '*':
                                    setDecCharset(2, t.value.charAt(2));
                                    break;
                                case '+':
                                    setDecCharset(3, t.value.charAt(2));
                                    break;
                                case 'n':
                                    decGlCharset = 2;
                                    break;
                                case 'o':
                                    decGlCharset = 3;
                                    break;
                                case '#':
                                    switch (t.value.charAt(2)) {
                                        case '8':
                                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                                            currScrBuf.fillAll('E');
                                            break;
                                    }
                                    break;
                                default:
                                    if (LOG_UNKNOWN_ESC)
                                        Log.w("CtrlSeq", "ESC: " +
                                                Compat.subSequence(t.value, 1, t.value.remaining())
                                                        .toString());
                            }
                            break;
                        case CTL:
                            switch (t.value.charAt(0)) {
                                case '\r':
                                    cr();
                                    break;
                                case '\n':
                                case '\u000B': // VT
                                    lf();
                                    break;
                                case '\t':
                                    tab(1);
                                    break;
                                case '\b':
                                    bs();
                                    break;
                                case '\u0007':
                                    ++numBellEvents;
                                    break;
                                case '\u000E':
                                    decGlCharset = 1;
                                    break;
                                case '\u000F':
                                    decGlCharset = 0;
                                    break;
                                default:
                                    if (LOG_UNKNOWN_ESC)
                                        Log.w("CtrlSeq", "CTL: " +
                                                (int) t.value.charAt(0));
                            }
                            break;
                        case TEXT: {
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            final CharBuffer text = DecTerminalCharsets.translate(t.value,
                                    decGxCharsets[decGlCharset]);
                            if (insertMode) currScrBuf.insertChars(text.length());
                            currScrBuf.setChars(text);
                            break;
                        }
                    }
                }
            }
        } catch (final IOException e) {
            if (BuildConfig.DEBUG)
                Log.e("-", "Strange IO error", e);
        } finally {
            mInputBuf.release();
        }
    }

    private void parseCsi(@NonNull final EscCsi csi) {
        if (csi.suffix.isEmpty())
            switch (csi.prefix) {
                case 0:
                    switch (csi.type) {
                        case 'c':
                            sendBack("\u001B[?6c"); // TODO: VT102 yet...
//                        Log.i("CtrlSeq", "Terminal type request");
                            return;
                        case 'n':
                            if (csi.args.length == 0) break;
                            switch (csi.args[0]) {
                                case "5":
                                    sendBack("\u001B[0n"); // Terminal OK
                                    return;
                                case "6":
                                    sendBack("\u001B[" + (currScrBuf.getPosY() + 1) + ";" +
                                            (currScrBuf.getPosX() + 1) + "R");
                                    return;
                            }
                            break;
                        case 'A':
                            currScrBuf.movePosY(-zao(csi.getIntArg(0, 1)));
                            return;
                        case 'B':
                        case 'e':
                            currScrBuf.movePosY(zao(csi.getIntArg(0, 1)));
                            return;
                        case 'C':
                        case 'a':
                            currScrBuf.movePosX(zao(csi.getIntArg(0, 1)));
                            return;
                        case 'D':
                            currScrBuf.movePosX(-zao(csi.getIntArg(0, 1)));
                            return;
                        case 'E':
                            currScrBuf.movePosY(zao(csi.getIntArg(0, 1)));
                            cr();
                            return;
                        case 'F':
                            currScrBuf.movePosY(-zao(csi.getIntArg(0, 1)));
                            cr();
                            return;
                        case 'G':
                        case '`':
                            currScrBuf.setPosX(csi.getIntArg(0, 1) - 1);
                            return;
                        case 'd':
                            currScrBuf.setPosY(csi.getIntArg(0, 1) - 1);
                            return;
                        case 'H':
                        case 'f':
                            currScrBuf.setPosY(csi.getIntArg(0, 1) - 1);
                            currScrBuf.setPosX(csi.getIntArg(1, 1) - 1);
                            return;
                        case 'I': // CHT
                            tab(csi.getIntArg(0, 1));
                            return;
                        case 'X':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.eraseChars(zao(csi.getIntArg(0, 1)));
                            return;
                        case 'Z': // CBT
                            tab(-csi.getIntArg(0, 1));
                            return;
                        case 'g': // TBC
                            switch (csi.getIntArg(0, 0)) {
                                case 0:
                                    tabClear(currScrBuf.getPosX());
                                    return;
                                case 3:
                                    tabClear(-1);
                                    return;
                            }
                            break;
                        case 'J':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            switch (csi.getIntArg(0, 0)) {
                                case 0:
                                    currScrBuf.eraseBelow();
                                    return;
                                case 1:
                                    currScrBuf.eraseAbove();
                                    return;
                                case 2:
                                    currScrBuf.eraseAll();
                                    return;
                            }
                            break;
                        case 'K':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            switch (csi.getIntArg(0, 0)) {
                                case 0:
                                    currScrBuf.eraseLineRight();
                                    return;
                                case 1:
                                    currScrBuf.eraseLineLeft();
                                    return;
                                case 2:
                                    currScrBuf.eraseLineAll();
                                    return;
                            }
                            break;
                        case 'm': {
                            int i = 0;
                            final ConsoleScreenCharAttrs aa = mCurrAttrs;
                            if (csi.args.length == 0) {
                                aa.reset();
                                return;
                            }
                            while (i < csi.args.length) {
                                final int a = csi.getIntArg(i++, 0);
                                switch (a) {
                                    case 0:
                                        aa.reset();
                                        break;
                                    case 1:
                                        aa.bold = true;
                                        break;
                                    case 2:
                                        aa.faint = true;
                                        break;
                                    case 3:
                                        aa.italic = true;
                                        break;
                                    case 4:
                                    case 21: // Actually doubly underlined.
                                        aa.underline = true;
                                        break;
                                    case 5:
                                    case 6:
                                        aa.blinking = true;
                                        break;
                                    case 7:
                                        aa.inverse = true;
                                        break;
                                    case 8:
                                        aa.invisible = true;
                                        break;
                                    case 9:
                                        aa.crossed = true;
                                        break;
                                    case 22:
                                        aa.bold = false;
                                        aa.faint = false;
                                        break;
                                    case 23:
                                        aa.italic = false;
                                        break;
                                    case 24:
                                        aa.underline = false;
                                        break;
                                    case 25:
                                        aa.blinking = false;
                                        break;
                                    case 27:
                                        aa.inverse = false;
                                        break;
                                    case 28:
                                        aa.invisible = false;
                                        break;
                                    case 29:
                                        aa.crossed = false;
                                        break;
                                    case 38:
                                        i = parseColors(csi, i, true);
                                        break;
                                    case 39:
                                        aa.resetFg();
                                        break;
                                    case 48:
                                        i = parseColors(csi, i, false);
                                        break;
                                    case 49:
                                        aa.resetBg();
                                        break;
                                    default:
                                        if ((a >= 30) && (a <= 37)) {
                                            final int v = a - 30;
                                            aa.fgColor = v;
                                            aa.fgColorIndexed = true;
                                            break;
                                        }
                                        if ((a >= 40) && (a <= 47)) {
                                            final int v = a - 40;
                                            aa.bgColor = ConsoleScreenCharAttrs
                                                    .getBasicColor(v);
                                            break;
                                        }
                                        if ((a >= 90) && (a <= 97)) {
                                            final int v = a - 90;
                                            aa.fgColor = v | 8;
                                            aa.fgColorIndexed = true;
                                            break;
                                        }
                                        if ((a >= 100) && (a <= 107)) {
                                            final int v = a - 100;
                                            aa.bgColor = ConsoleScreenCharAttrs
                                                    .getBasicColor(v | 8);
                                            break;
                                        }
                                        if (LOG_UNKNOWN_ESC)
                                            Log.w("CtrlSeq", "Attr: " + a +
                                                    " in " + csi.body);
                                }
                            }
                            return;
                        }
                        case 'L':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.insertLine(csi.getIntArg(0, 1));
                            return;
                        case 'M':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.deleteLine(csi.getIntArg(0, 1));
                            return;
                        case '@':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.insertChars(csi.getIntArg(0, 1));
                            return;
                        case 'P':
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.deleteChars(csi.getIntArg(0, 1));
                            return;
                        case 'S': // SU VT420
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.scrollUp(csi.getIntArg(0, 1));
                            return;
                        case 'T': // SD VT420
                            currScrBuf.setCurrentAttrs(mCurrAttrs);
                            currScrBuf.scrollDown(csi.getIntArg(0, 1));
                            return;
                        case 'h':
                        case 'l': {
                            final boolean value = csi.type == 'h';
                            switch (csi.getIntArg(0, 0)) {
                                case 4:
                                    insertMode = value;
                                    return;
                            }
                            break;
                        }
                        case 'q': { // DECLL
                            for (int i = 0; i < csi.args.length; ++i)
                                switch (csi.getIntArg(i, 0)) {
                                    case 0:
                                        numLed = false;
                                        capsLed = false;
                                        scrollLed = false;
                                        break;
                                    case 1:
                                        numLed = true;
                                        break;
                                    case 2:
                                        capsLed = true;
                                        break;
                                    case 3:
                                        scrollLed = true;
                                        break;
                                    case 21:
                                        numLed = false;
                                        break;
                                    case 22:
                                        capsLed = false;
                                        break;
                                    case 23:
                                        scrollLed = false;
                                        break;
                                }
                            return;
                        }
                        case 'r':
                            if (csi.args.length == 2)
                                currScrBuf.setTBMargins(csi.getIntArg(0, 1) - 1,
                                        csi.getIntArg(1, currScrBuf.getHeight()) - 1);
                            else currScrBuf.resetMargins();
                            currScrBuf.setPos(0, 0);
                            return;
                        case 's':
                            saveCursor();
                            return;
                        case 'u':
                            restoreCursor();
                            return;
                        case 't':
                            if (csi.args.length == 2) break;
                            switch (csi.getIntArg(0, 0)) {
                                case 22:
                                    switch (csi.getIntArg(1, 0)) {
                                        case 0:
                                        case 2:
                                            pushWindowTitle();
                                    }
                                    return;
                                case 23:
                                    switch (csi.getIntArg(1, 0)) {
                                        case 0:
                                        case 2:
                                            popWindowTitle();
                                    }
                                    return;
                            }
                            break;
                        default:
                    }
                    break;
                case '?':
                    switch (csi.type) {
                        case 'h':
                        case 'l': {
                            final int opt = csi.getIntArg(0, -1);
                            if (opt < 0) break;
                            decPrivateMode.set(opt, csi.type == 'h');
                            return;
                        }
                        case 'r': {
                            final int opt = csi.getIntArg(0, -1);
                            if (opt < 0) break;
                            decPrivateMode.restore(opt);
                            return;
                        }
                        case 's': {
                            final int opt = csi.getIntArg(0, -1);
                            if (opt < 0) break;
                            decPrivateMode.save(opt);
                            return;
                        }
                    }
                    break;
            }
        if (LOG_UNKNOWN_ESC)
            Log.w("CtrlSeq", "ESC[" + ((csi.prefix == 0) ? "" : csi.prefix) +
                    csi.body + csi.suffix + csi.type);
    }

    private int parseColors(@NonNull final EscCsi csi, int i, final boolean isFg) {
        if (csi.args.length == i) return i;
        final int color;
        switch (csi.getIntArg(i, 0)) {
            case 2: // TrueColor
                if (csi.args.length - i < 4) return i;
                ++i;
                color = Color.rgb(
                        csi.getIntArg(i++, 0),
                        csi.getIntArg(i++, 0),
                        csi.getIntArg(i++, 0)
                ); // Bad taste but valid in Java.
                break;
            case 5: // 256
                if (csi.args.length - i < 2) return i;
                ++i;
                final int c = csi.getIntArg(i++, 0);
                if ((c & ~0xFF) != 0) return i - 2;
                if (c < 16) {
                    final int l = 127 | Misc.bitsAs(c, 8, 128);
                    color = Color.rgb(
                            Misc.bitsAs(c, 1, l),
                            Misc.bitsAs(c, 2, l),
                            Misc.bitsAs(c, 4, l)
                    );
                } else if (c < 232) {
                    color = Color.rgb(
                            (c / 36) * 51,
                            ((c / 6) % 6) * 51,
                            (c % 6) * 51
                    );
                } else {
                    final int l = 8 + 10 * (c - 232);
                    color = Color.rgb(l, l, l);
                }
                break;
            default:
                return i;
        }
        if (isFg) {
            mCurrAttrs.fgColor = color;
            mCurrAttrs.fgColorIndexed = false;
        } else mCurrAttrs.bgColor = color;
        return i;
    }

    private class DecPrivateMode {
        private final SparseBooleanArray current = new SparseBooleanArray();
        private final SparseBooleanArray saved = new SparseBooleanArray();

        private void apply(final int opt, final boolean value) {
            switch (opt) {
                case 1:
                    if (consoleOutput != null)
                        consoleOutput.appCursorKeys = value;
                    return;
                case 3: // DECCOLM
                    resize(value ? 132 : 80, currScrBuf.getHeight());
                    currScrBuf.eraseAll();
                    currScrBuf.setAbsPos(0, 0);
                    return;
                case 5:
                    currScrBuf.inverseScreen = value; // DECSCNM
                    return;
                case 6: // DECOM
                    currScrBuf.originMode = value;
                    currScrBuf.setPos(0, 0);
                    return;
                case 7:
                    mainScrBuf.wrap = value;
                    altScrBuf.wrap = value;
                    return;
                case 8:
                    if (consoleOutput != null)
                        consoleOutput.keyAutorepeat = value;
                    return;
                case 9:
                    if (consoleOutput != null)
                        consoleOutput.mouseTracking = value ?
                                ConsoleOutput.MouseTracking.X10
                                : ConsoleOutput.MouseTracking.NONE;
                    return;
                case 25:
                    cursorVisibility = value;
                    return;
                case 66:
                    if (consoleOutput != null)
                        consoleOutput.appNumKeys = value;
                    return;
                case 67:
                    if (consoleOutput != null)
                        consoleOutput.appDECBKM = value;
                    return;
                case 1000:
                    if (consoleOutput != null)
                        consoleOutput.mouseTracking = value ?
                                ConsoleOutput.MouseTracking.X11
                                : ConsoleOutput.MouseTracking.NONE;
                    return;
                case 1001:
                    if (consoleOutput != null)
                        consoleOutput.mouseTracking = value ?
                                ConsoleOutput.MouseTracking.HIGHLIGHT
                                : ConsoleOutput.MouseTracking.NONE;
                    return;
                case 1002:
                    if (consoleOutput != null)
                        consoleOutput.mouseTracking = value ?
                                ConsoleOutput.MouseTracking.BUTTON_EVENT
                                : ConsoleOutput.MouseTracking.NONE;
                    return;
                case 1003:
                    if (consoleOutput != null)
                        consoleOutput.mouseTracking = value ?
                                ConsoleOutput.MouseTracking.ANY_EVENT
                                : ConsoleOutput.MouseTracking.NONE;
                    return;
                case 1004:
                    if (consoleOutput != null)
                        consoleOutput.mouseFocusInOut = value;
                    return;
                case 1005:
                    if (consoleOutput != null)
                        consoleOutput.mouseProtocol = value ?
                                ConsoleOutput.MouseProtocol.UTF8
                                : ConsoleOutput.MouseProtocol.NORMAL;
                    return;
                case 1006:
                    if (consoleOutput != null)
                        consoleOutput.mouseProtocol = value ?
                                ConsoleOutput.MouseProtocol.SGR
                                : ConsoleOutput.MouseProtocol.NORMAL;
                    return;
                case 1015:
                    if (consoleOutput != null)
                        consoleOutput.mouseProtocol = value ?
                                ConsoleOutput.MouseProtocol.URXVT
                                : ConsoleOutput.MouseProtocol.NORMAL;
                    return;
                case 47:
                case 1047:
                    if (value) {
                        altScrBuf.setPos(currScrBuf);
                        currScrBuf = altScrBuf;
                        currScrBuf.clear();
                    } else {
                        mainScrBuf.setPos(currScrBuf);
                        currScrBuf = mainScrBuf;
                    }
                    currScrBuf.getCurrentAttrs(mCurrAttrs);
                    return;
                case 1048:
                    if (value) {
                        saveCursor();
                    } else {
                        restoreCursor();
                    }
                    currScrBuf.getCurrentAttrs(mCurrAttrs);
                    return;
                case 1049:
                    if (value) {
                        saveCursor();
                        altScrBuf.setPos(currScrBuf);
                        currScrBuf = altScrBuf;
                        currScrBuf.clear();
                    } else {
                        mainScrBuf.setPos(currScrBuf);
                        currScrBuf = mainScrBuf;
                        restoreCursor();
                    }
                    currScrBuf.getCurrentAttrs(mCurrAttrs);
                    return;
                case 2004:
                    if (consoleOutput != null)
                        consoleOutput.bracketedPasteMode = value;
                    return;
            }
            if (LOG_UNKNOWN_ESC)
                Log.w("CtrlSeq", "DecPrivateMode: " + opt + " = " + value);
        }

        public void set(final int opt, final boolean value) {
            current.put(opt, value);
            apply(opt, value);
        }

        public void save(final int opt) {
            final int i = current.indexOfKey(opt);
            if (i < 0) return;
            saved.put(opt, current.valueAt(i));
        }

        public void restore(final int opt) {
            final int i = saved.indexOfKey(opt);
            if (i < 0) return;
            final boolean v = saved.valueAt(i);
            current.put(opt, v);
            apply(opt, v);
        }

        public Iterable<Integer> getState() {
            return Misc.getTrueKeysIterable(current);
        }
    }

    private final DecPrivateMode decPrivateMode = new DecPrivateMode();

    { // Some specific values
        decPrivateMode.set(7, true); // Wrap
        decPrivateMode.save(7);
    }
}
