package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public final class TermKeyMapAdapter extends BaseAdapter {
    public interface OnSelectListener {
        void onSelect(boolean isBuiltIn, String name, TermKeyMapRules rules, String title);
    }

    private TermKeyMapManager.Meta[] mSortedDataIndex;
    private String mMarkedName = null;
    private boolean mEditorEnabled = false;
    private View.OnClickListener mOnClick = null;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener = null;
    private OnSelectListener mOnSelectListener = null;
    private boolean includeBuiltIns = false;
    @LayoutRes
    private int itemLayoutRes = R.layout.term_key_map_manager_entry;
    @LayoutRes
    private int dropDownItemLayoutRes = R.layout.term_key_map_manager_entry;
    private final Context context;

    private TermKeyMapManager.Meta zeroEntry = null;

    public TermKeyMapAdapter(@NonNull final Context context) {
        this.context = context.getApplicationContext();
        TermKeyMapManager.addOnChangeListener(this::updateSortIndex);
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

    public TermKeyMapAdapter setDropDownItemLayoutRes(@LayoutRes final int v) {
        dropDownItemLayoutRes = v;
        return this;
    }

    public TermKeyMapAdapter setMarked(@Nullable final String name) {
        mMarkedName = name;
        notifyDataSetChanged();
        return this;
    }

    public TermKeyMapAdapter setEditorEnabled(final boolean v) {
        mEditorEnabled = v;
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
    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) convertView = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        setupView(position, convertView);
        return convertView;
    }

    @Override
    public View getDropDownView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) convertView = LayoutInflater.from(parent.getContext())
                .inflate(dropDownItemLayoutRes, parent, false);
        setupView(position, convertView);
        return convertView;
    }

    private void setupView(final int position, @NonNull final View view) {
        if (mOnSelectListener != null)
            view.setOnClickListener(v -> {
                final TermKeyMapManager.Meta meta = getMeta(position);
                mOnSelectListener.onSelect(meta.isBuiltIn, meta.name, meta.getKeyMap(),
                        meta.getTitle(v.getContext()));
            });
        else if (mOnClick != null)
            view.setOnClickListener(mOnClick);
        if (mOnCreateContextMenuListener != null)
            view.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

        final TextView nameView;
        final View markView;
        final View editView;
        if (view instanceof TextView) {
            nameView = (TextView) view;
            markView = null;
            editView = null;
        } else {
            nameView = view.findViewById(R.id.name);
            markView = view.findViewById(R.id.mark);
            editView = view.findViewById(R.id.edit);
        }
        final TermKeyMapManager.Meta meta = getMeta(position);
        nameView.setText(meta.getTitle(view.getContext()));
        nameView.setTypeface(null, meta.isBuiltIn ? Typeface.ITALIC : Typeface.NORMAL);

        if (markView != null)
            markView.setVisibility(mMarkedName == null ? View.GONE :
                    mMarkedName.equals(meta.name) ? View.VISIBLE : View.INVISIBLE);

        if (editView != null)
            if (mEditorEnabled) {
                ((ImageView) editView).setImageState(
                        meta.isBuiltIn ? state_new : state_empty, true);
                editView.setOnClickListener(v ->
                        TermKeyMapEditorActivity.start(v.getContext(),
                                getMeta(position).name));
                editView.setVisibility(View.VISIBLE);
                view.setNextFocusRightId(R.id.edit);
                view.setNextFocusLeftId(R.id.edit);
                if (mOnCreateContextMenuListener == null)
                    view.setOnCreateContextMenuListener(meta.isBuiltIn ? null :
                            (menu, v, menuInfo) ->
                                    menu.add(R.string.action_delete).setOnMenuItemClickListener(
                                            item -> {
                                                TermKeyMapManager.remove(getMeta(position).name);
                                                return true;
                                            }
                                    )
                    );
            } else {
                if (mOnCreateContextMenuListener == null)
                    view.setOnCreateContextMenuListener(null);
                editView.setOnClickListener(null);
                view.setNextFocusRightId(View.NO_ID);
                view.setNextFocusLeftId(View.NO_ID);
                editView.setVisibility(View.GONE);
            }
    }

    private static final int[] state_empty = new int[]{};
    private static final int[] state_new = new int[]{R.attr.state_new};
}
