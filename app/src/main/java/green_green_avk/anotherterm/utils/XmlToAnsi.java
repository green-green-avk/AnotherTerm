package green_green_avk.anotherterm.utils;

import android.graphics.Color;

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

    private static String makeCsiColor(final int color, final boolean bg) {
        return "\u001B[" + (bg ? "4" : "3") + "8;2;"
                + Color.red(color) + ";"
                + Color.green(color) + ";"
                + Color.blue(color) + "m";
    }

    private final class XmlIterator implements Iterator<String> {
        @NonNull
        private final XmlPullParser parser;

        private XmlIterator() {
            try {
                parser = parserFactory.newPullParser();
                parser.setInput(new StringReader(input));
                parser.defineEntityReplacementText("nbsp", "\u00A0");
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

        private void appendResetAttrs() {
            output.append("\u001B[0m");
        }

        private void appendSetAttrs() {
            aBold.appendIfSet();
            aItalic.appendIfSet();
            aUnderline.appendIfSet();
            aFgColor.appendIfSet();
            aBgColor.appendIfSet();
        }

        private void appendLn() {
            appendResetAttrs();
            output.append("\n");
        }

        private void appendLeftMargin(final int off) {
//            return "\u001B[" + (indent * indentStep + off + 1) + "G"; // not `less -R' friendly
            final int l = indent * indentStep + off;
            if (l > 0)
                output.append(StringUtils.repeat(' ', l));
            appendSetAttrs();
        }

        private int currCol = 0;

        private void renderParagraphText(@NonNull final String v) {
            if (false/*isInPre*/) output.append(v);
            else {
                int ptr = 0;
                if (currCol == 0 && v.charAt(0) == ' ')
                    ptr++;
                final int w = width - indent * indentStep;
                if (!isInP)
                    appendLeftMargin(0);
                if (w <= currCol) {
                    appendLn();
                    appendLeftMargin(0);
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
                        appendLn();
                        appendLeftMargin(0);
                        currCol = 0;
                    }
                }
            }
            isInP = true;
        }

        private void renderBr() {
            appendLn();
            appendLeftMargin(0);
            currCol = 0;
//            isInP = true;
        }

        private void renderParagraphInterval() {
            if (!isInP) return;
            appendLn();
            output.append("\n");
            currCol = 0;
            isInP = false;
        }

        private abstract class AnsiAttr {
            abstract boolean isSet();

            abstract void append();

            final void appendIfSet() {
                if (isSet())
                    append();
            }

            protected final void apply() {
                if (isInP)
                    append();
            }
        }

        private abstract class AnsiAttrBit extends AnsiAttr {
            protected int depth = 0;

            @Override
            boolean isSet() {
                return depth > 0;
            }

            void begin() {
                depth++;
                apply();
            }

            void end() {
                depth--;
                apply();
            }
        }

        private abstract class AnsiAttrColor extends AnsiAttr {
            protected final Stack<Integer> stack = new Stack<>();

            @Override
            boolean isSet() {
                return !stack.empty();
            }

            void begin(final int color) {
                stack.push(color);
                apply();
            }

            void end() {
                if (isSet())
                    stack.pop();
                apply();
            }
        }

        private final AnsiAttrBit aBold = new AnsiAttrBit() {
            @Override
            void append() {
                if (isSet()) output.append("\u001B[1m");
                else output.append("\u001B[22m");
            }
        };

        private final AnsiAttrBit aItalic = new AnsiAttrBit() {
            @Override
            void append() {
                if (isSet()) output.append("\u001B[3m");
                else output.append("\u001B[23m");
            }
        };

        private final AnsiAttrBit aUnderline = new AnsiAttrBit() {
            @Override
            void append() {
                if (isSet()) output.append("\u001B[4m");
                else output.append("\u001B[24m");
            }
        };

        private final AnsiAttrColor aFgColor = new AnsiAttrColor() {
            @Override
            void append() {
                if (isSet()) output.append(makeCsiColor(stack.peek(), false));
                else output.append("\u001B[39m");
            }
        };

        private final AnsiAttrColor aBgColor = new AnsiAttrColor() {
            @Override
            void append() {
                if (isSet()) output.append(makeCsiColor(stack.peek(), true));
                else output.append("\u001B[49m");
            }
        };

        @Nullable
        private String href = null;

        private void beginTag() {
            switch (parser.getName().toLowerCase()) {
                case "a":
                    href = parser.getAttributeValue(null, "href");
                    return;
                case "footnote": {
                    final String ref = parser.getAttributeValue(null, "ref");
                    if (ref == null) return;
                    aBold.begin();
                    aFgColor.begin(Color.rgb(0x88, 0xCC, 0xFF));
                    renderParagraphText(Unicode.toSuperscript(ref));
                    aFgColor.end();
                    aBold.end();
                    return;
                }
                case "kbd":
                    aBold.begin();
                    aBgColor.begin(Color.rgb(0xFF, 0xFF, 0xFF));
                    aFgColor.begin(Color.rgb(0, 0, 0));
                    renderParagraphText("\u00A0");
                    return;
                case "clipboard":
                case "code":
                    aBgColor.begin(Color.rgb(0x30, 0x20, 0x10));
                    aFgColor.begin(Color.rgb(0xFF, 0xCC, 0x88));
                    return;
                case "b":
                    aBold.begin();
                    return;
                case "em":
                case "i":
                    aItalic.begin();
                    return;
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    renderParagraphInterval();
                    aBold.begin();
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
                            appendLeftMargin(0);
                            break;
                        default:
                            appendLeftMargin(-2);
                            output.append("* ");
                    }
                    isInP = true;
                    return;
                case "dt":
                    indent++;
                    renderParagraphInterval();
                    aBold.begin();
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
                    renderBr();
                    return;
            }
        }

        private void endTag() {
            switch (parser.getName().toLowerCase()) {
                case "a":
                    if (href != null && !href.startsWith("info:")) {
                        renderParagraphText(": ");
                        renderBr();
                        aBgColor.begin(Color.rgb(0x10, 0x20, 0x30));
                        aFgColor.begin(Color.rgb(0x88, 0xCC, 0xFF));
                        output.append("\u001B[?7s\u001B[?7h").append(href).append("\u001B[?7r");
                        aFgColor.end();
                        aBgColor.end();
                        appendResetAttrs();
                        output.append("\u001B[K");
                        renderBr();
                        href = null;
                    }
                    return;
                case "kbd":
                    renderParagraphText("\u00A0");
                    aFgColor.end();
                    aBgColor.end();
                    aBold.end();
                    return;
                case "clipboard":
                case "code":
                    aFgColor.end();
                    aBgColor.end();
                    return;
                case "b":
                    aBold.end();
                    return;
                case "em":
                case "i":
                    aItalic.end();
                    return;
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    aBold.end();
                    renderParagraphInterval();
                    return;
                case "ul":
                case "ol":
                    if (!lts.empty()) lts.pop();
                    indent--;
                    return;
                case "dt":
                    indent--;
                    aBold.end();
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
