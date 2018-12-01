package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public final class TermKeyMapAdapter extends BaseAdapter {
    public interface OnSelectListener {
        void onSelect(boolean isBuiltIn, String name, TermKeyMapRules rules, String title);
    }

    private TermKeyMapManager.Meta[] mSortedDataIndex;
    private String mMarkedName = null;
    private View.OnClickListener mOnClick = null;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener = null;
    private OnSelectListener mOnSelectListener = null;
    private boolean includeBuiltIns = false;
    @LayoutRes
    private int itemLayoutRes = R.layout.term_key_map_manager_entry;
    private final Context context;

    private TermKeyMapManager.Meta zeroEntry = null;

    private final Runnable onDataChanged = new Runnable() {
        @Override
        public void run() {
            updateSortIndex();
        }
    };

    public TermKeyMapAdapter(@NonNull final Context context) {
        this.context = context.getApplicationContext();
        TermKeyMapManager.addOnChangeListener(onDataChanged);
        updateSortIndex();
    }

    public void setZeroEntry(final TermKeyMapManager.Meta entry) {
        this.zeroEntry = entry;
    }

    private final Comparator<TermKeyMapManager.Meta> defaultSortOrder =
            new Comparator<TermKeyMapManager.Meta>() {
                @Override
                public int compare(final TermKeyMapManager.Meta o1,
                                   final TermKeyMapManager.Meta o2) {
                    if (o1.isBuiltIn && !o2.isBuiltIn) return -1;
                    if (!o1.isBuiltIn && o2.isBuiltIn) return 1;
                    if (o1.order < o2.order) return -1;
                    if (o1.order > o2.order) return 1;
                    // TODO: optimize
                    return o1.getTitle(context).compareToIgnoreCase(o2.getTitle(context));
                }
            };

    private boolean isSortIndexReady = false;

    private void rebuildSortIndex() {
        if (isSortIndexReady) return;
        final Set<TermKeyMapManager.Meta> m;
        if (includeBuiltIns) m = TermKeyMapManager.enumerate();
        else m = TermKeyMapManager.enumerateCustom();
        final TermKeyMapManager.Meta[] keys = m.toArray(new TermKeyMapManager.Meta[0]);
        Arrays.sort(keys, defaultSortOrder);
        mSortedDataIndex = keys;
        isSortIndexReady = true;
    }

    public void updateSortIndex() {
        isSortIndexReady = false;
        notifyDataSetChanged();
    }

    public String getName(final int i) {
        return getMeta(i).name;
    }

    public TermKeyMapManager.Meta getMeta(final int i) {
        rebuildSortIndex();
        if (zeroEntry != null) {
            if (i == 0) return zeroEntry;
            return mSortedDataIndex[i - 1];
        }
        return mSortedDataIndex[i];
    }

    public int getPosition(final String name) {
        rebuildSortIndex();
        final TermKeyMapManager.Meta m = TermKeyMapManager.getMeta(name);
        if (zeroEntry != null && zeroEntry.equals(m)) return 0;
        for (int i = 0; i < mSortedDataIndex.length; ++i) {
            if (mSortedDataIndex[i].equals(m)) return i + (zeroEntry != null ? 1 : 0);
        }
        return -1;
    }

    public TermKeyMapAdapter setOnClickListener(final View.OnClickListener v) {
        mOnClick = v;
        return this;
    }

    public TermKeyMapAdapter setOnCreateContextMenuListener(final View.OnCreateContextMenuListener v) {
        mOnCreateContextMenuListener = v;
        return this;
    }

    public TermKeyMapAdapter setOnSelectListener(final OnSelectListener v) {
        mOnSelectListener = v;
        return this;
    }

    public TermKeyMapAdapter setIncludeBuiltIns(final boolean v) {
        includeBuiltIns = v;
        updateSortIndex();
        return this;
    }

    public TermKeyMapAdapter setItemLayoutRes(@LayoutRes final int v) {
        itemLayoutRes = v;
        return this;
    }

    public TermKeyMapAdapter setMarked(@Nullable final String name) {
        mMarkedName = name;
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getCount() {
        rebuildSortIndex();
        return mSortedDataIndex.length + (zeroEntry != null ? 1 : 0);
    }

    @Override
    public Object getItem(final int position) {
        return getMeta(position);
    }

    @Override
    public long getItemId(final int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;
        if (convertView != null) view = convertView;
        else view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        if (mOnSelectListener != null)
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final TermKeyMapManager.Meta meta = getMeta(position);
                    mOnSelectListener.onSelect(meta.isBuiltIn, meta.name, meta.getKeyMap(),
                            meta.getTitle(v.getContext()));
                }
            });
        else if (mOnClick != null)
            view.setOnClickListener(mOnClick);
        if (mOnCreateContextMenuListener != null)
            view.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

        final TextView nameView;
        final View markView;
        if (view instanceof TextView) {
            nameView = (TextView) view;
            markView = null;
        } else {
            nameView = view.findViewById(R.id.name);
            markView = view.findViewById(R.id.mark);
        }
        final TermKeyMapManager.Meta meta = getMeta(position);
        nameView.setText(meta.getTitle(parent.getContext()));
        nameView.setTypeface(null, meta.isBuiltIn ? Typeface.ITALIC : Typeface.NORMAL);
        if (markView != null)
            markView.setVisibility(mMarkedName == null ? View.GONE :
                    mMarkedName.equals(meta.name) ? View.VISIBLE : View.INVISIBLE);

        return view;
    }
}
