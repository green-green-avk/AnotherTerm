package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.XmlToSpanned;

public class HtmlTextView extends androidx.appcompat.widget.AppCompatTextView {
    public boolean async = false;

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
            async = a.getBoolean(R.styleable.HtmlTextView_async, async);
            setHtmlText(a.getString(R.styleable.HtmlTextView_htmlText));
            setXmlText(a.getString(R.styleable.HtmlTextView_xmlText));
        } finally {
            a.recycle();
        }
    }

    public void setSpannedText(@Nullable final Spanned spannedText) {
        if (spannedText != null) {
            // https://blog.uncommon.is/a-better-way-to-handle-links-in-textview-27bb70b2d31c ?
            // https://github.com/saket/Better-Link-Movement-Method ?
            setMovementMethod(LinkMovementMethod.getInstance());
            setText(spannedText);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void setHtmlText(@Nullable final String htmlText, final boolean async) {
        if (htmlText == null) return;
        if (async) new AsyncTask<String, Object, Spanned>() {
            @Override
            protected Spanned doInBackground(final String... args) {
                return HtmlCompat.fromHtml(args[0], HtmlCompat.FROM_HTML_MODE_LEGACY);
            }

            @Override
            protected void onPostExecute(final Spanned spanned) {
                setSpannedText(spanned);
            }
        }.execute(htmlText);
        else setSpannedText(HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    public void setHtmlText(@Nullable final String htmlText) {
        setHtmlText(htmlText, async);
    }

    @SuppressLint("StaticFieldLeak")
    public void setXmlText(@Nullable final String xmlText, final boolean async) {
        if (xmlText == null) return;
        if (async) new AsyncTask<String, Object, Spanned>() {
            @Override
            protected Spanned doInBackground(final String... args) {
                return XmlToSpanned.fromXml(args[0], getContext());
            }

            @Override
            protected void onPostExecute(final Spanned spanned) {
                setSpannedText(spanned);
            }
        }.execute(xmlText);
        else setSpannedText(XmlToSpanned.fromXml(xmlText, getContext()));
    }

    public void setXmlText(@Nullable final String xmlText) {
        setXmlText(xmlText, async);
    }
}
