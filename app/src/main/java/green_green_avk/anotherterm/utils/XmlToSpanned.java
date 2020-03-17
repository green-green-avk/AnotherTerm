package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;
import java.util.regex.Pattern;

import green_green_avk.anotherterm.R;

public final class XmlToSpanned {
    private static final Pattern webProtoP = Pattern.compile("^(?:https?|ftp)");

    @NonNull
    private final XmlPullParserFactory parserFactory;
    @NonNull
    private final String input;

    public XmlToSpanned(@NonNull final String v) {
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

    private final class Maker {
        private final XmlPullParser parser;
        private final Context ctx;

        private Maker(@NonNull final Context ctx) {
            try {
                parser = parserFactory.newPullParser();
                parser.setInput(new StringReader(input));
            } catch (final XmlPullParserException e) {
                throw new Error(e);
            }
            this.ctx = ctx;
        }

        private final Stack<Object> lists = new Stack<>();
        private final Stack<Object> spans = new Stack<>();

        private void startNullSpan() {
            spans.push(null);
        }

        private void startSpan(@NonNull final Object span, final int pos) {
            output.setSpan(span, pos, pos, Spanned.SPAN_MARK_MARK);
            spans.push(span);
        }

        private Object endSpan(final int pos) {
            final Object span = spans.pop();
            if (span != null) {
                final int start = output.getSpanStart(span);
                if (pos > start) {
                    output.setSpan(span, start, pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (span instanceof URLSpan && webProtoP.matcher(((URLSpan) span).getURL()).find()) {
                        output.append("\uD83C\uDF10");
                        output.setSpan(new ImageSpan(ctx,
                                        R.drawable.ic_mark_web, DynamicDrawableSpan.ALIGN_BASELINE),
                                output.length() - 2, output.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else output.removeSpan(span);
            }
            return span;
        }

        private void startParagraph() {
            if (output.length() > 0 && output.charAt(output.length() - 1) != '\n')
                output.append("\n\n");
            else if (output.length() > 1 && output.charAt(output.length() - 2) != '\n')
                output.append('\n');
        }

        private int oneUp() {
            if (output.length() > 1 && output.charAt(output.length() - 1) == '\n'
                    && output.charAt(output.length() - 2) == '\n') return 1;
            else return 0;
        }

        private final Editable output = new SpannableStringBuilder();

        private void text() {
            final String v = parser.getText();
            if (v.isEmpty()) return;
            if (output.length() > 0 && output.charAt(output.length() - 1) != '\n'
                    || v.charAt(0) != ' ')
                output.append(v);
            else output.append(v, 1, v.length());
        }

        private void beginTag() {
            switch (parser.getName().toLowerCase()) {
                case "a": {
                    String href = parser.getAttributeValue(null, "href");
                    if (href == null) href = "";
                    startSpan(new URLSpan(href), output.length());
                    break;
                }
                case "b":
                    startSpan(new StyleSpan(Typeface.BOLD), output.length());
                    break;
                case "em":
                case "i":
                    startSpan(new StyleSpan(Typeface.ITALIC), output.length());
                    break;
                case "p":
                    startParagraph();
                    break;
                case "br":
                    output.append('\n');
                    break;
                case "h1":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length() - oneUp());
                    startSpan(new RelativeSizeSpan(2f), output.length() - oneUp());
                    break;
                case "h2":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length() - oneUp());
                    startSpan(new RelativeSizeSpan(1.8f), output.length() - oneUp());
                    break;
                case "h3":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length() - oneUp());
                    startSpan(new RelativeSizeSpan(1.6f), output.length() - oneUp());
                    break;
                case "h4":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length() - oneUp());
                    startSpan(new RelativeSizeSpan(1.4f), output.length() - oneUp());
                    break;
                case "h5":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length() - oneUp());
                    startSpan(new RelativeSizeSpan(1.2f), output.length() - oneUp());
                    break;
                case "h6":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length() - oneUp());
                    startSpan(new RelativeSizeSpan(1.1f), output.length() - oneUp());
                    break;
                case "code":
                    startSpan(new TypefaceSpan("monospace"), output.length());
                    startSpan(new RelativeSizeSpan(0.8f), output.length() - oneUp());
                    startSpan(new BackgroundColorSpan(0x40808080), output.length());
                    break;
                case "kbd":
                    startSpan(new BackgroundImageSpan(ctx, R.drawable.bg_frame2), output.length());
                    break;
                case "li": {
                    startParagraph();
                    Object list = null;
                    if (!lists.empty() && (list = lists.peek()) instanceof Integer) {
                        output.append(list.toString()).append(") ");
                        lists.pop();
                        lists.push((int) list + 1);
                        startNullSpan();
                    } else if (list instanceof Character && list.equals('*')) {
                        startSpan(new BulletSpan(15), output.length());
                    } else {
                        startNullSpan();
                    }
                    break;
                }
                case "dt":
                    startParagraph();
                    startSpan(new StyleSpan(Typeface.BOLD), output.length());
                    break;
                case "dd":
                    startParagraph();
                    startSpan(new LeadingMarginSpan.Standard(15), output.length());
                    break;
                case "dl": {
                    startParagraph();
                    startSpan(new LeadingMarginSpan.Standard(15), output.length());
                    lists.push(null);
                    break;
                }
                case "ul":
                    startParagraph();
                    startSpan(new LeadingMarginSpan.Standard(15), output.length());
                    if ("none".equals(parser.getAttributeValue(null, "type")))
                        lists.push(null);
                    else lists.push('*');
                    break;
                case "ol":
                    startParagraph();
                    startSpan(new LeadingMarginSpan.Standard(15), output.length());
                    lists.push(1);
                    break;
                case "blockquote":
                    startParagraph();
                    startSpan(
                            new CustomQuoteSpan(ctx.getResources().getColor(R.color.colorAccent)),
                            output.length());
                    break;
                case "clipboard":
                    startSpan(new ClipboardSpan(), output.length());
                    break;
            }
        }

        private void endTag() {
            switch (parser.getName().toLowerCase()) {
                case "a":
                case "b":
                case "em":
                case "i":
                case "kbd":
                    endSpan(output.length());
                    break;
                case "p":
                    startParagraph();
                    break;
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    startParagraph();
                    endSpan(output.length() - oneUp());
                    endSpan(output.length() - oneUp());
                    break;
                case "code":
                    endSpan(output.length());
                    endSpan(output.length());
                    endSpan(output.length());
                    break;
                case "li":
                case "dt":
                case "dd":
                    startParagraph();
                    endSpan(output.length());
                    break;
                case "dl":
                case "ul":
                case "ol": {
                    startParagraph();
                    endSpan(output.length());
                    lists.pop();
                    break;
                }
                case "blockquote": {
                    startParagraph();
                    endSpan(output.length());
                    break;
                }
                case "clipboard": {
                    final ClipboardSpan span = (ClipboardSpan) endSpan(output.length());
                    final String content = output.subSequence(output.getSpanStart(span),
                            output.getSpanEnd(span)).toString();
                    span.setContent(content);
                    output.append('\u2398');
                    output.setSpan(new ImageSpan(ctx,
                                    R.drawable.ic_mark_copy, DynamicDrawableSpan.ALIGN_BASELINE),
                            output.length() - 1, output.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
            }
        }

        private Editable make() {
            try {
                while (true) {
                    switch (parser.next()) {
                        case XmlPullParser.START_TAG:
                            beginTag();
                            break;
                        case XmlPullParser.END_TAG:
                            endTag();
                            break;
                        case XmlPullParser.TEXT:
                            text();
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            return output;
                        default:
                            throw new Error("Bad state");
                    }
                }
            } catch (final IOException e) {
                throw new Error(e);
            } catch (final XmlPullParserException e) {
                throw new Error(e);
            }
        }
    }

    public Editable make(@NonNull final Context ctx) {
        return new Maker(ctx).make();
    }

    public static Editable fromXml(@NonNull final String v, @NonNull final Context ctx) {
        return new XmlToSpanned(v).make(ctx);
    }
}
