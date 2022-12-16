package green_green_avk.anotherterm.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.DragEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import green_green_avk.anotherterm.utils.Misc;

public class EditableTokenCollectionView extends AppCompatEditText
        implements ParameterView<Collection<? extends CharSequence>>, StringifiableCollectionView,
        CategorizedCollectionView<CharSequence>, DragAndDropCollectionView {
    public EditableTokenCollectionView(@NonNull final Context context) {
        super(context);
    }

    public EditableTokenCollectionView(@NonNull final Context context,
                                       @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public EditableTokenCollectionView(@NonNull final Context context,
                                       @Nullable final AttributeSet attrs,
                                       final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public char delimiter = ',';

    @NonNull
    protected String getDelimiterRegex() {
        return Pattern.quote(Character.toString(delimiter));
    }

    protected int findItemStart(@NonNull final CharSequence v, final int pos) {
        int i = pos - 1;
        for (; i >= 0; i--)
            if (v.charAt(i) == delimiter)
                break;
        return i + 1;
    }

    protected int findItemEnd(@NonNull final CharSequence v, final int pos) {
        int i = pos;
        for (; i < v.length(); i++)
            if (v.charAt(i) == delimiter)
                break;
        return i;
    }

    protected void markCategory(@NonNull final Spannable v, final int start, final int end,
                                @Nullable final ItemCategory category) {
        if (start >= end)
            return;
        final ForegroundColorSpan[] colorSpans =
                v.getSpans(start, end, ForegroundColorSpan.class);
        if (category != null) {
            @ColorInt final int color = getResources().getColor(category.colorRes);
            ForegroundColorSpan colorSpan = null;
            for (final ForegroundColorSpan span : colorSpans) {
                if (span.getForegroundColor() == color)
                    colorSpan = span;
                v.removeSpan(span);
            }
            if (colorSpan == null)
                colorSpan = new ForegroundColorSpan(color);
            v.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            for (final ForegroundColorSpan span : colorSpans) {
                v.removeSpan(span);
            }
        }
    }

    protected void markCategory(@NonNull final Spannable v, final int at,
                                @Nullable final ItemCategory category) {
        final int start = findItemStart(v, at);
        final int end = findItemEnd(v, at);
        markCategory(v, start, end, category);
    }

    protected void updateSpans(@NonNull final Spannable v, final int start, final int end) {
        int i = findItemStart(v, start);
        while (i <= end) {
            final int ei = findItemEnd(v, i);
            markCategory(v, i, ei,
                    getItemCategory(v.subSequence(i, ei)));
            i = ei + 1;
        }
    }

    {
        final Spannable text = getText();
        if (text != null) {
            updateSpans(text, 0, text.length());
        }
    }

    @Override
    protected void onTextChanged(final CharSequence text,
                                 final int start, final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (text instanceof Spannable) {
            updateSpans((Spannable) text, start, start + lengthAfter);
        }
        if (isSetByUser) {
            notifyValueChanged();
        }
    }

    protected void selectNearestDelimiter(final float x, final float y) {
        final Spannable text = getEditableText();
        if (text == null)
            return;
        final int pos = getOffsetForPosition(x, y);
        final int left = findItemStart(text, pos);
        final int right = findItemEnd(text, pos);
        if (pos - left <= right - pos) {
            Selection.setSelection(text, Math.max(0, left - 1), left);
        } else {
            Selection.setSelection(text, right,
                    Math.min(right + 1, text.length()));
        }
    }

    @Nullable
    protected static CharSequence getClipDataItemText(@Nullable final ClipData data,
                                                      final int index) {
        if (data == null || data.getItemCount() <= index)
            return null;
        final ClipData.Item item = data.getItemAt(index);
        return item == null ? null : item.getText();
    }

    protected boolean inDrag = false;

    @Override
    public boolean onDragEvent(final DragEvent event) {
        final ClipDescription desc = event.getClipDescription();
        if (inDrag || desc != null && dndMimeType != null && desc.hasMimeType(dndMimeType)) {
            final int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_LOCATION:
                    selectNearestDelimiter(event.getX(), event.getY());
                    return true;
                case DragEvent.ACTION_DROP: {
                    final Editable text = getEditableText();
                    if (text == null)
                        return true;
                    final CharSequence itemText =
                            getClipDataItemText(event.getClipData(), 0);
                    if (itemText == null)
                        return true;
                    selectNearestDelimiter(event.getX(), event.getY());
                    String replacement = itemText.toString();
                    if (Selection.getSelectionStart(text) != 0)
                        replacement = delimiter + replacement;
                    if (Selection.getSelectionEnd(text) != text.length())
                        replacement += delimiter;
                    UiUtils.replaceSelection(text, replacement);
                    return true;
                }
                case DragEvent.ACTION_DRAG_STARTED:
                    inDrag = true;
                    setActivated(true);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    setActivated(false);
                    inDrag = false;
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    requestFocus();
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                default:
                    return true;
            }
        }
        if (desc != null && desc.hasMimeType("text/*"))
            return super.onDragEvent(event);
        return true;
    }

    private boolean isSetByUser = true;

    @Override
    public void setText(final CharSequence text, final BufferType type) {
        isSetByUser = false;
        try {
            super.setText(text, type);
        } finally {
            isSetByUser = true;
        }
    }

    public void setText(@Nullable final Collection<? extends CharSequence> list) {
        if (list == null) {
            setText("");
            return;
        }
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final Iterator<? extends CharSequence> it = list.iterator(); it.hasNext(); ) {
            final CharSequence item = it.next();
            builder.append(item);
            if (it.hasNext())
                builder.append(delimiter);
        }
        setText(builder);
    }

    protected void adjustOutput(@NonNull final List<String> v) {
        for (final ListIterator<String> it = v.listIterator(); it.hasNext(); ) {
            final String ve = it.next().replaceAll("^\\s+|\\s+$", "");
            if (ve.isEmpty())
                it.remove();
            else
                it.set(ve);
        }
    }

    @Override
    @NonNull
    public Collection<? extends CharSequence> getValue() {
        final CharSequence text = getText();
        if (text == null)
            return new ArrayList<>();
        final List<String> r = new ArrayList<>(Arrays.asList(
                getText().toString().split(getDelimiterRegex())));
        adjustOutput(r);
        return r;
    }

    @Override
    @NonNull
    public String getValueAsString() {
        return String.join(String.valueOf(delimiter), getValue());
//        final CharSequence text = getText();
//        if (text == null)
//            return "";
//        final String dr = getDelimiterRegex();
//        return text.toString().replaceAll(
//                "^\\s*" + dr + "+\\s*|\\s*" + dr + "+\\s*$|\\s*(" + dr + ")(" + dr + "|\\s)+",
//                "$1");
    }

    @Override
    public void setValue(@Nullable final Collection<? extends CharSequence> v) {
        setText(v);
    }

    @Override
    public void setValueFrom(@Nullable final Object v) {
        if (v instanceof Collection)
            if (Misc.isOrdered((Collection<?>) v))
                ParameterView.super.setValueFrom(v);
            else
                ParameterView.super.setValueFrom(new TreeSet<Object>((Collection<?>) v));
        else if (v != null)
            setText(Arrays.asList(v.toString().split(",")));
        else
            setText("");
    }

    protected void notifyValueChanged() {
        if (onValueChanged != null)
            onValueChanged.onValueChanged(getValue());
    }

    protected OnValueChanged<? super Collection<? extends CharSequence>> onValueChanged = null;

    @Override
    public void setOnValueChanged(@Nullable final OnValueChanged<? super Collection<? extends CharSequence>> v) {
        onValueChanged = v;
    }

    @Nullable
    protected ItemCategory getItemCategory(@NonNull final CharSequence item) {
        if (getItemCategory != null)
            return getItemCategory.getItemCategory(item.toString());
        return null;
    }

    @Nullable
    protected GetItemCategory<? super CharSequence> getItemCategory = null;

    @Override
    public void setOnGetItemCategory(@Nullable final GetItemCategory<? super CharSequence> v) {
        getItemCategory = v;
    }

    @Nullable
    protected String dndMimeType = null;

    @Override
    public void setDragAndDropMimeType(@Nullable final String v) {
        dndMimeType = v;
    }
}
