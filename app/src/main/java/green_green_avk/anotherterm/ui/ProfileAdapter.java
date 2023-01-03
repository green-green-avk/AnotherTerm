package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.ReturnThis;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.ProfileManager;

public abstract class ProfileAdapter<T> extends UniAdapter {
    public interface OnClickListener {
        void onClick(@NonNull ProfileManager.Meta meta);
    }

    public interface OnCreateContextMenuListener {
        void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull ProfileManager.Meta meta,
                                 @Nullable ContextMenu.ContextMenuInfo menuInfo);
    }

    private ProfileManager.Meta[] mSortedDataIndex;
    private String mMarkedName = null;
    private boolean mEditorEnabled = false;
    private OnClickListener mOnClick = null;
    private OnCreateContextMenuListener mOnCreateContextMenuListener = null;
    private boolean includeBuiltIns = false;
    @LayoutRes
    private int itemLayoutRes = R.layout.profile_manager_entry;
    @LayoutRes
    private int dropDownItemLayoutRes = R.layout.profile_manager_entry;
    @NonNull
    protected final Context context;

    private ProfileManager.Meta zeroEntry = null;

    @NonNull
    protected abstract ProfileManager<T> getManager();

    public abstract void startEditor(@Nullable String name);

    private boolean isSortIndexReady = false;
    private final Runnable updateSortIndex = () -> {
        isSortIndexReady = false;
        notifyDataSetChanged();
    };

    public ProfileAdapter(@NonNull final Context context) {
        this.context = context;
        getManager().addOnChangeListener(updateSortIndex);
    }

    public void setZeroEntry(final ProfileManager.Meta entry) {
        zeroEntry = entry;
        notifyDataSetChanged();
    }

    private final Comparator<ProfileManager.Meta> defaultSortOrder =
            new Comparator<ProfileManager.Meta>() {
                @Override
                public int compare(final ProfileManager.Meta o1,
                                   final ProfileManager.Meta o2) {
                    if (o1.isBuiltIn && !o2.isBuiltIn)
                        return -1;
                    if (!o1.isBuiltIn && o2.isBuiltIn)
                        return 1;
                    if (o1.order < o2.order)
                        return -1;
                    if (o1.order > o2.order)
                        return 1;
                    // TODO: optimize
                    return o1.getTitle(context).compareTo(o2.getTitle(context));
                }
            };

    private void rebuildSortIndex() {
        if (isSortIndexReady)
            return;
        final Set<? extends ProfileManager.Meta> m;
        if (includeBuiltIns)
            m = getManager().enumerate();
        else
            m = getManager().enumerateCustom();
        final ProfileManager.Meta[] keys = m.toArray(new ProfileManager.Meta[0]);
        Arrays.sort(keys, defaultSortOrder);
        mSortedDataIndex = keys;
        isSortIndexReady = true;
    }

    public void updateSortIndex() {
        updateSortIndex.run();
    }

    @NonNull
    public String getName(final int i) {
        return getMeta(i).name;
    }

    @NonNull
    public ProfileManager.Meta getMeta(final int i) {
        rebuildSortIndex();
        if (zeroEntry != null) {
            if (i == 0)
                return zeroEntry;
            return mSortedDataIndex[i - 1];
        }
        return mSortedDataIndex[i];
    }

    public int getPosition(final String name) {
        rebuildSortIndex();
        final ProfileManager.Meta m = getManager().getMeta(name);
        if (zeroEntry != null && zeroEntry.equals(m))
            return 0;
        for (int i = 0; i < mSortedDataIndex.length; ++i)
            if (mSortedDataIndex[i].equals(m))
                return i + (zeroEntry != null ? 1 : 0);
        return -1;
    }

    @ReturnThis
    public ProfileAdapter<T> setOnClickListener(final OnClickListener v) {
        mOnClick = v;
        return this;
    }

    @ReturnThis
    public ProfileAdapter<T> setOnCreateContextMenuListener(final OnCreateContextMenuListener v) {
        mOnCreateContextMenuListener = v;
        return this;
    }

    @ReturnThis
    public ProfileAdapter<T> setIncludeBuiltIns(final boolean v) {
        includeBuiltIns = v;
        updateSortIndex.run();
        return this;
    }

    @ReturnThis
    public ProfileAdapter<T> setItemLayoutRes(@LayoutRes final int v) {
        itemLayoutRes = v;
        return this;
    }

    @ReturnThis
    public ProfileAdapter<T> setDropDownItemLayoutRes(@LayoutRes final int v) {
        dropDownItemLayoutRes = v;
        return this;
    }

    @ReturnThis
    public ProfileAdapter<T> setMarked(@Nullable final String name) {
        mMarkedName = name;
        notifyDataSetChanged();
        return this;
    }

    @ReturnThis
    public ProfileAdapter<T> setEditorEnabled(final boolean v) {
        mEditorEnabled = v;
        return this;
    }

    @Override
    public int onGetCount() {
        rebuildSortIndex();
        return mSortedDataIndex.length + (zeroEntry != null ? 1 : 0);
    }

    @Override
    @NonNull
    public Object onGetItem(final int position) {
        return getMeta(position);
    }

    @Override
    @NonNull
    protected View onCreateView(@NonNull final ViewGroup parent, final int type) {
        return LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
    }

    @Override
    @NonNull
    protected View onCreateDropDownView(@NonNull final ViewGroup parent, final int type) {
        return LayoutInflater.from(parent.getContext())
                .inflate(dropDownItemLayoutRes, parent, false);
    }

    private static final int[] state_empty = new int[]{};
    private static final int[] state_new = new int[]{R.attr.state_new};

    @Override
    protected void onBind(@NonNull final View view, final int position) {
        final ProfileManager.Meta meta = getMeta(position);

        if (mOnClick != null) {
            view.setOnClickListener(v -> mOnClick.onClick(meta));
        }
        if (mOnCreateContextMenuListener != null) {
            view.setOnCreateContextMenuListener((menu, v, menuInfo) ->
                    mOnCreateContextMenuListener.onCreateContextMenu(menu, meta,
                            menuInfo));
        }

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

        nameView.setText(meta.getTitle(view.getContext()));
        nameView.setTypeface(null, meta.isBuiltIn ? Typeface.ITALIC : Typeface.NORMAL);

        if (markView != null) {
            markView.setVisibility(mMarkedName == null ? View.GONE :
                    mMarkedName.equals(meta.name) ? View.VISIBLE : View.INVISIBLE);
        }

        if (editView != null) {
            if (mEditorEnabled) {
                ((ImageView) editView).setImageState(
                        meta.isBuiltIn ? state_new : state_empty, true);
                editView.setOnClickListener(v -> startEditor(meta.name));
                editView.setVisibility(View.VISIBLE);
                view.setNextFocusRightId(R.id.edit);
                view.setNextFocusLeftId(R.id.edit);
                if (mOnCreateContextMenuListener == null) {
                    view.setOnCreateContextMenuListener(meta.isBuiltIn ? null :
                            (menu, v, menuInfo) ->
                                    menu.add(R.string.action_delete).setOnMenuItemClickListener(
                                            item -> {
                                                getManager().remove(meta.name);
                                                return true;
                                            }
                                    )
                    );
                }
            } else {
                if (mOnCreateContextMenuListener == null) {
                    view.setOnCreateContextMenuListener(null);
                }
                editView.setOnClickListener(null);
                view.setNextFocusRightId(View.NO_ID);
                view.setNextFocusLeftId(View.NO_ID);
                editView.setVisibility(View.GONE);
            }
        }
    }
}
