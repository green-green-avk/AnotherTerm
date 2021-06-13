package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Stack;

public final class XmlToAnsi implements Iterable<String> {
    @NonNull
    private final XmlPullParserFactory parserFactory;
    @NonNull
    private final String input;
    public int width = 80;
    public int indentStep = 4;

    public XmlToAnsi(@NonNull final String v) {
        try {
            parserFactory = XmlPullParserFactory.newInstance();
        } catch (final XmlPullParserException e) {
            throw new Error(e);
        }
        input = v;
    }

    @NonNull
    public String getInput() {
        return input;
    }

    private static final class OutputVec extends LinkedList<String> implements Appendable {

        @NonNull
        @Override
        public OutputVec append(@Nullable final CharSequence csq) {
            if (csq != null) add(csq.toString());
            return this;
        }

        @NonNull
        @Override
        public OutputVec append(@Nullable final CharSequence csq,
                                final int start, final int end) {
            if (csq != null) add(csq.subSequence(start, end).toString());
            return this;
        }

        @NonNull
        @Override
        public OutputVec append(final char c) {
            add(Character.toString(c));
            return this;
        }
    }

    private final class XmlIterator implements Iterator<String> {
        @NonNull
        private final XmlPullParser parser;

        private XmlIterator() {
            try {
                parser = parserFactory.newPullParser();
                parser.setInput(new StringReader(input));
            } catch (final XmlPullParserException e) {
                throw new RuntimeException(e);
            }
        }

        @NonNull
        private final OutputVec output = new OutputVec();

        private int indent = 0;
        private boolean isInP = false;
        private boolean isInPre = false;
        private final Stack<String> lts = new Stack<>();

        @NonNull
        private String makeLeftMargin(final int off) {
//            return "\u001B[" + (indent * indentStep + off + 1) + "G"; // not `less -R' friendly
            return StringUtils.repeat(' ', indent * indentStep + off);
        }

        private int currCol = 0;

        private void renderParagraphText(@NonNull final String v) {
            if (false/*isInPre*/) output.append(v);
            else {
                int ptr = 0;
                if (currCol == 0 && v.charAt(0) == ' ') ptr++;
                final int w = width - indent * indentStep;
                if (!isInP) output.append(makeLeftMargin(0));
                if (w <= currCol) {
                    output.append("\n").append(makeLeftMargin(0));
                    currCol = 0;
                }
                while (ptr < v.length()) {
                    final int cw = w - currCol;
                    if (v.length() - ptr <= cw) {
                        output.append(v, ptr, v.length());
                        currCol += v.length() - ptr;
                        ptr = v.length();
                    } else {
                        if (v.charAt(ptr + cw) == ' ') {
                            output.append(v, ptr, ptr + cw);
                            ptr += cw + 1;
                        } else {
                            final int p = v.substring(ptr, ptr + cw).lastIndexOf(' ');
                            if (p < 0) {
                                if (currCol == 0) {
                                    output.append(v, ptr, ptr + cw);
                                    ptr += cw;
                                }
                            } else {
                                output.append(v, ptr, ptr + p);
                                ptr += p + 1;
                            }
                        }
                        output.append("\n").append(makeLeftMargin(0));
                        currCol = 0;
                    }
                }
            }
            isInP = true;
        }

        private void renderParagraphInterval() {
            if (!isInP) return;
            output.append("\n\n");
            currCol = 0;
            isInP = false;
        }

        private int isBold = 0;

        private void beginBold() {
            if (isBold++ == 0) output.append("\u001B[1m");
        }

        private void endBold() {
            if (--isBold == 0) output.append("\u001B[22m");
        }

        private int isItalic = 0;

        private void beginItalic() {
            if (isItalic++ == 0) output.append("\u001B[3m");
        }

        private void endItalic() {
            if (--isItalic == 0) output.append("\u001B[23m");
        }

        private void beginTag() {
            switch (parser.getName().toLowerCase()) {
                case "kbd":
                    output.append("\u001B[4m");
                case "clipboard":
                case "code":
                case "b":
                    beginBold();
                    return;
                case "em":
                case "i":
                    beginItalic();
                    return;
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    renderParagraphInterval();
                    beginBold();
                    return;
                case "ul":
                case "ol": {
                    indent++;
                    String type = parser.getAttributeValue(null, "type");
                    type = (type != null) ? type.toLowerCase() : "";
                    lts.push(type);
                    return;
                }
                case "li":
                    renderParagraphInterval();
                    switch (lts.empty() ? "" : lts.peek()) {
                        case "none":
                            output.append(makeLeftMargin(0));
                            break;
                        default:
                            output.append(makeLeftMargin(-2)).append("* ");
                    }
                    isInP = true;
                    return;
                case "dt":
                    indent++;
                    renderParagraphInterval();
                    beginBold();
                    return;
                case "dd":
                    indent++;
                    renderParagraphInterval();
                    return;
                case "pre":
                    isInPre = true;
                case "p":
                    renderParagraphInterval();
                    return;
                case "br":
                    output.append("\n").append(makeLeftMargin(0));
                    currCol = 0;
                    return;
            }
        }

        private void endTag() {
            switch (parser.getName().toLowerCase()) {
                case "kbd":
                    output.append("\u001B[24m");
                case "clipboard":
                case "code":
                case "b":
                    endBold();
                    return;
                case "em":
                case "i":
                    endItalic();
                    return;
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    endBold();
                    renderParagraphInterval();
                    return;
                case "ul":
                case "ol":
                    if (!lts.empty()) lts.pop();
                    indent--;
                    return;
                case "dt":
                    indent--;
                    endBold();
                    renderParagraphInterval();
                    return;
                case "dd":
                    indent--;
                    renderParagraphInterval();
                    return;
                case "pre":
                    isInPre = false;
                case "li":
                case "p":
                    renderParagraphInterval();
                    return;
            }
        }

        private void parseNext() {
            try {
                while (output.isEmpty()) {
                    switch (parser.next()) {
                        case XmlPullParser.START_TAG:
                            beginTag();
                            break;
                        case XmlPullParser.END_TAG:
                            endTag();
                            break;
                        case XmlPullParser.TEXT:
                            // Could work improperly in the case when
                            // the first merged text node is a whitespace
//                            if (!parser.isWhitespace())
                            final String text = parser.getText();
                            if (isInP || !text.trim().isEmpty())
                                renderParagraphText(text);
                            break;
                        default:
                            return;
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } catch (final XmlPullParserException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            if (output.isEmpty()) parseNext();
            return !output.isEmpty();
        }

        @Override
        public String next() {
            if (!hasNext()) throw new NoSuchElementException();
            return output.remove();
        }
    }

    @NonNull
    @Override
    public Iterator<String> iterator() {
        return new XmlIterator();
    }
}
