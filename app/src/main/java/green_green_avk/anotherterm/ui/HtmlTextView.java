package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.HtmlCompat;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.XmlToSpanned;

/**
 * It is better than WebView in two ways:
 * 1) it is less resource-hungry;
 * 2) current WebView component upgrades
 * does not support Android versions less than 5.
 */
public class HtmlTextView extends AppCompatTextView {
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
            setMovementMethod(UiUtils.getFixedLinkMovementMethod());
            setText(spannedText);
        }
    }

    @NonNull
    protected Spanned fromHtml(@NonNull final String v) {
        try {
            return HtmlCompat.fromHtml(v, HtmlCompat.FROM_HTML_MODE_LEGACY);
        } catch (final Throwable e) {
            return SpannedString.valueOf(getContext().getString(
                    R.string.msg_html_parse_error_s, e.getLocalizedMessage()));
        }
    }

    @NonNull
    protected Spanned fromXml(@NonNull final String v) {
        try {
            return XmlToSpanned.fromXml(v, getContext());
        } catch (final Throwable e) {
            return SpannedString.valueOf(getContext().getString(
                    R.string.msg_xml_parse_error_s, e.getLocalizedMessage()));
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void setHtmlText(@Nullable final String htmlText, final boolean async) {
        if (htmlText == null) return;
        if (async) new AsyncTask<String, Object, Spanned>() {
            @Override
            protected Spanned doInBackground(final String... args) {
                return fromHtml(args[0]);
            }

            @Override
            protected void onPostExecute(final Spanned spanned) {
                setSpannedText(spanned);
            }
        }.execute(htmlText);
        else setSpannedText(fromHtml(htmlText));
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
                return fromXml(args[0]);
            }

            @Override
            protected void onPostExecute(final Spanned spanned) {
                setSpannedText(spanned);
            }
        }.execute(xmlText);
        else setSpannedText(fromXml(xmlText));
    }

    public void setXmlText(@Nullable final String xmlText) {
        setXmlText(xmlText, async);
    }
}
