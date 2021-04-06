package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class InputTokenizer
        implements Iterator<InputTokenizer.Token>, Iterable<InputTokenizer.Token> {
    private static final int SEQ_MAXLEN = 256;

    public boolean _8BitMode = true;

    public static final class Token {
        public enum Type {
            TEXT, CTL, ESC, CSI, OSC, SS2, SS3
        }

        public Type type;
        public CharBuffer value;

        @NonNull
        public CharSequence getArg() {
            if (value.charAt(0) == '\u001B')
                return Compat.subSequence(value, 2, value.length());
            else
                return Compat.subSequence(value, 1, value.length());
        }
    }

    private final Token ownToken = new Token();

    private final CharBuffer mBuf = (CharBuffer) CharBuffer.allocate(8192).flip();
    private final char[] mBufArr = mBuf.array();
    private int mPos = 0;
    private int mEnd = 0;
    private CharBuffer mToken = null;
    private Token.Type mType = Token.Type.TEXT;
    private Boolean mGotNext = false;

    private void found(int pos) {
        ++pos;
        mToken = Compat.subSequence(mBuf, mPos, pos);
        mPos = pos;
    }

    private void notFound(final int pos) {
        if ((pos - mPos) > SEQ_MAXLEN) {
            mType = Token.Type.TEXT;
            setTextEnd(pos);
            return;
        }
        mToken = null;
    }

    private void getOsc(int pos) {
        for (; pos < mEnd; ++pos) {
            switch (mBufArr[pos]) {
                case '\u001B':
                    if (pos + 1 >= mEnd || mBufArr[pos + 1] != '\\') break;
                    ++pos;
                    found(pos); // ST
                    return;
                case '\u009C': // ST
                    if (!_8BitMode) break;
                case '\u0007':
                    found(pos);
                    return;
            }
        }
        notFound(pos);
    }

    private void getCsi(int pos) {
        for (; pos < mEnd; ++pos) {
            if (mBufArr[pos] >= 0x40 && mBufArr[pos] <= 0x7E) {
                found(pos);
                return;
            }
        }
        notFound(pos);
    }

    private void getEsc(int pos) {
        for (; pos < mEnd; ++pos) {
            if (mBufArr[pos] < 0x20 || mBufArr[pos] >= 0x30) {
                found(pos);
                return;
            }
        }
        notFound(pos);
    }

    private void getExtraSymbol(int pos) {
        if (pos >= mEnd) {
            mToken = null;
            return;
        }
        if (Character.isHighSurrogate(mBufArr[pos])) {
            ++pos;
            if (pos >= mEnd) {
                mToken = null;
                return;
            }
            if (!Character.isLowSurrogate(mBufArr[pos]))
                --pos;
        }
        found(pos);
    }

    private void setTextEnd(int pos) {
        if (pos > mPos && Character.isHighSurrogate(mBufArr[pos - 1]))
            --pos;
        mToken = Compat.subSequence(mBuf, mPos, pos);
        mPos = pos;
    }

    private void getNext() {
        if (mPos >= mEnd) {
            mToken = null;
            return;
        }
        int pos = mPos;
        if (_8BitMode)
            while (mBufArr[pos] > 0x1F && mBufArr[pos] < 0x7F || mBufArr[pos] > 0x9F) {
                if (++pos >= mEnd) {
                    mType = Token.Type.TEXT;
                    setTextEnd(mEnd);
                    return;
                }
            }
        else
            while (mBufArr[pos] > 0x1F && mBufArr[pos] != 0x7F) {
                if (++pos >= mEnd) {
                    mType = Token.Type.TEXT;
                    setTextEnd(mEnd);
                    return;
                }
            }
        if (pos > mPos) {
            mType = Token.Type.TEXT;
            mToken = Compat.subSequence(mBuf, mPos, pos);
            mPos = pos;
            return;
        }
        if (mBufArr[pos] != '\u001B') {
            if (_8BitMode) {
                switch (mBufArr[pos]) {
                    case '\u009B':
                        mType = Token.Type.CSI;
                        getCsi(pos + 1);
                        return;
                    case '\u009D':
                        mType = Token.Type.OSC;
                        getOsc(pos + 1);
                        return;
                    case '\u008E':
                        mType = Token.Type.SS2;
                        getExtraSymbol(pos + 1);
                        return;
                    case '\u008F':
                        mType = Token.Type.SS3;
                        getExtraSymbol(pos + 1);
                        return;
                }
            }
            mType = Token.Type.CTL;
            found(pos);
            return;
        }
        ++pos;
        if (pos >= mEnd) {
            mToken = null;
            return;
        }
        switch (mBufArr[pos]) {
            case '[':
                mType = Token.Type.CSI;
                getCsi(pos + 1);
                return;
            case ']':
                mType = Token.Type.OSC;
                getOsc(pos + 1);
                return;
            case 'N':
                mType = Token.Type.SS2;
                getExtraSymbol(pos + 1);
                return;
            case 'O':
                mType = Token.Type.SS3;
                getExtraSymbol(pos + 1);
                return;
        }
        if (mBufArr[pos] >= 0x40 && mBufArr[pos] < 0x60) {
            mType = Token.Type.CTL;
            found(pos);
            return;
        }
        mType = Token.Type.ESC;
        getEsc(pos);
    }

    @Override
    @NonNull
    public Token next() {
        if (!mGotNext) getNext();
        if (mToken == null) throw new NoSuchElementException();
        mGotNext = false;
        ownToken.type = mType;
        ownToken.value = mToken;
        return ownToken;
    }

    @Override
    public boolean hasNext() {
        if (!mGotNext) getNext();
        mGotNext = true;
        return mToken != null;
    }

    @Override
    @NonNull
    public Iterator<Token> iterator() {
        return this;
    }

    @NonNull
    public CharBuffer getLeftovers() {
        return mBuf;
    }

    public void tokenize(@NonNull final Readable v) throws IOException {
        mBuf.position(mPos);
        mBuf.compact();
        try {
            v.read(mBuf);
        } finally {
            mBuf.flip();
            mPos = 0;
            mEnd = mBuf.limit();
            mGotNext = false;
        }
    }
}
