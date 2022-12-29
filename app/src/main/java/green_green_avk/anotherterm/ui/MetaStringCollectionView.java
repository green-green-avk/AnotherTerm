package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.ui.dragndrop.DragLinkMovementMethod;
import green_green_avk.anotherterm.ui.dragndrop.TextDragSpan;
import green_green_avk.anotherterm.utils.Misc;

public class MetaStringCollectionView extends AppCompatTextView
        implements ReadonlyParameterView<Collection<? extends CharSequence>>, MetaParameterView,
        CategorizedCollectionView<CharSequence>, DragAndDropCollectionView {
    public MetaStringCollectionView(@NonNull final Context context) {
        super(context);
    }

    public MetaStringCollectionView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public MetaStringCollectionView(@NonNull final Context context, @Nullable final AttributeSet attrs,
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
        setMovementMethod(DragLinkMovementMethod.getInstance());
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final Iterator<? extends CharSequence> it = list.iterator(); it.hasNext(); ) {
            final CharSequence item = it.next();
            builder.append("X");
            final SpannableStringBuilder subBuilder = new SpannableStringBuilder(item);
            final ItemCategory category = getItemCategory(item);
            if (category != null) {
                subBuilder.setSpan(new ForegroundColorSpan(getResources()
                                .getColor(category.colorRes)),
                        0, subBuilder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            subBuilder.append('\u2398');
            subBuilder.setSpan(new InlineImageSpan(getContext(), R.drawable.ic_copy)
                            .useTextColor(),
                    subBuilder.length() - 1, subBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(
                    new BackgroundImageSpan(getContext(), R.drawable.bg_frame2)
                            .setContent(subBuilder),
                    builder.length() - 1, builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ClipboardSpan(item),
                    builder.length() - 1, builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (dndMimeType != null) {
                builder.setSpan(new TextDragSpan(item, dndMimeType),
                        builder.length() - 1, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (it.hasNext())
                builder.append(delimiter);
        }
        setText(builder);
    }

    @Override
    public void setValue(@Nullable final Collection<? extends CharSequence> v) {
        setText(v);
    }

    @Override
    public void setValueFrom(@Nullable final Object v) {
        if (v instanceof Collection)
            if (Misc.isOrdered((Collection<?>) v))
                ReadonlyParameterView.super.setValueFrom(v);
            else
                ReadonlyParameterView.super.setValueFrom(new TreeSet<Object>((Collection<?>) v));
        else if (v != null)
            setText(Arrays.asList(v.toString().split(",")));
        else
            setText("");
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
