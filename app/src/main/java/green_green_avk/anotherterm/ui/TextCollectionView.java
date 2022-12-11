package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.BackgroundImageSpan;
import green_green_avk.anotherterm.utils.ClipboardSpan;
import green_green_avk.anotherterm.utils.Misc;

public class TextCollectionView extends AppCompatTextView
        implements ParameterView<Collection<? extends CharSequence>> {
    public TextCollectionView(@NonNull final Context context) {
        super(context);
    }

    public TextCollectionView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public TextCollectionView(@NonNull final Context context, @Nullable final AttributeSet attrs,
                              final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @NonNull
    public CharSequence delimiter = ",";

    public void setText(@Nullable final Collection<? extends CharSequence> list) {
        if (list == null) {
            setText("");
            return;
        }
        setMovementMethod(UiUtils.getFixedLinkMovementMethod());
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final Iterator<? extends CharSequence> it = list.iterator(); it.hasNext(); ) {
            final CharSequence v = it.next();
            builder.append("X");
            final SpannableStringBuilder subBuilder = new SpannableStringBuilder(v);
            subBuilder.append('\u2398');
            subBuilder.setSpan(new InlineImageSpan(getContext(), R.drawable.ic_copy)
                            .useTextColor(),
                    subBuilder.length() - 1, subBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(
                    new BackgroundImageSpan(getContext(), R.drawable.bg_frame2)
                            .setContent(subBuilder), builder.length() - 1, builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ClipboardSpan(v),
                    builder.length() - 1, builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (it.hasNext())
                builder.append(delimiter);
        }
        setText(builder);
    }

    @Override
    @Nullable
    public Collection<? extends CharSequence> getValue() {
        return null;
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

    @Override
    public void setOnValueChanged(@Nullable final OnValueChanged<? super Collection<? extends CharSequence>> v) {
        // Never happens
    }
}
