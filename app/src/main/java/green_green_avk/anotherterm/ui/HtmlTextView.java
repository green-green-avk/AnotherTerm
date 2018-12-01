package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.text.HtmlCompat;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.XmlToSpanned;

public class HtmlTextView extends android.support.v7.widget.AppCompatTextView {
    public HtmlTextView(final Context context) {
        this(context, null);
    }

    public HtmlTextView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.htmlTextViewStyle);
    }

    public HtmlTextView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AppHtmlTextView);
    }

    protected void init(final Context context, final AttributeSet attrs, final int defStyleAttr,
                        final int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.HtmlTextView, defStyleAttr, defStyleRes);
        try {
            setHtmlText(a.getString(R.styleable.HtmlTextView_htmlText));
            setXmlText(a.getString(R.styleable.HtmlTextView_xmlText));
        } finally {
            a.recycle();
        }
    }

    public void setHtmlText(final String htmlText) {
        if (htmlText != null) {
            // https://blog.uncommon.is/a-better-way-to-handle-links-in-textview-27bb70b2d31c ?
            // https://github.com/saket/Better-Link-Movement-Method ?
            setMovementMethod(LinkMovementMethod.getInstance());
            setText(HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
    }

    public void setXmlText(final String xmlText) {
        if (xmlText != null) {
            // https://blog.uncommon.is/a-better-way-to-handle-links-in-textview-27bb70b2d31c ?
            // https://github.com/saket/Better-Link-Movement-Method ?
            setMovementMethod(LinkMovementMethod.getInstance());
            setText(XmlToSpanned.fromXml(xmlText, getContext()));
        }
    }
}
