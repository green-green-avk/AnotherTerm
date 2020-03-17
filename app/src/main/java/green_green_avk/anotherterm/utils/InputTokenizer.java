package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class InputTokenizer implements Iterator<InputTokenizer.Token>, Iterable<InputTokenizer.Token> {
    private static final int SEQ_MAXLEN = 256;

    public static final class Token {
        public enum Type {
            TEXT, CTL, ESC, CSI, OSC
        }

        public Type type;
        public CharBuffer value;
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
        mToken = mBuf.subSequence(mPos, pos);
        mPos = pos;
    }

    private void notFound(final int pos) {
        if ((pos - mPos) > SEQ_MAXLEN) {
            mType = Token.Type.TEXT;
            mToken = mBuf.subSequence(mPos, pos);
            mPos = pos;
            return;
        }
        mToken = null;
    }

    private void getOsc(int pos) {
        for (; pos < mEnd; ++pos) {
            switch (mBufArr[pos]) {
                case 27:
                    if (pos + 1 >= mEnd || mBufArr[pos + 1] != '\\') break;
                    ++pos;
                case 7:
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
            if (mBufArr[pos] < 32 || mBufArr[pos] >= 48) {
                found(pos);
                return;
            }
        }
        notFound(pos);
    }

    private void getNext() {
        if (mEnd <= mPos) {
            mToken = null;
            return;
        }
        int pos = mPos;
        while (mBufArr[pos] > 0x1F && mBufArr[pos] != 0x7F) {
            if (++pos >= mEnd) {
                mType = Token.Type.TEXT;
                mToken = mBuf.subSequence(mPos, mEnd);
                mPos = mEnd;
                return;
            }
        }
        if (pos > mPos) {
            mType = Token.Type.TEXT;
            mToken = mBuf.subSequence(mPos, pos);
            mPos = pos;
            return;
        }
        if (mBufArr[pos] != '\u001B') {
            mType = Token.Type.CTL;
            mToken = mBuf.subSequence(pos, pos + 1);
            mPos = pos + 1;
            return;
        }
        mType = Token.Type.ESC;
        ++pos;
        if (mEnd <= pos) {
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
            default:
                getEsc(pos);
                return;
        }
    }

    @NonNull
    @Override
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

    @NonNull
    @Override
    public Iterator<Token> iterator() {
        return this;
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
