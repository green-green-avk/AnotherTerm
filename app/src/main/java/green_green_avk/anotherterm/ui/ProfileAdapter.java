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

    @Nullable
    private ProfileManager.Meta[] mSortedIndex = null;
    @Nullable
    private String mMarkedName = null;
    private boolean mEditorEnabled = false;
    @Nullable
    private OnClickListener mOnClick = null;
    @Nullable
    private OnCreateContextMenuListener mOnCreateContextMenuListener = null;
    private boolean includeBuiltIns = false;
    @LayoutRes
    private int itemLayoutRes = R.layout.profile_manager_entry;
    @LayoutRes
    private int dropDownItemLayoutRes = R.layout.profile_manager_entry;
    @NonNull
    protected final Context context;

    @Nullable
    private ProfileManager.Meta zeroEntry = null;

    @NonNull
    protected abstract ProfileManager<T> getManager();

    public abstract void startEditor(@Nullable String name);

    protected final Runnable updateSortedIndex = () -> {
        mSortedIndex = null;
        notifyDataSetChanged();
    };

    public ProfileAdapter(@NonNull final Context context) {
        this.context = context;
        getManager().addOnChangeListener(updateSortedIndex);
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
                    return o1.getTitle(context).toString()
                            .compareTo(o2.getTitle(context).toString());
                }
            };

    @NonNull
    protected Comparator<ProfileManager.Meta> onGetSortOrder() {
        return defaultSortOrder;
    }

    @NonNull
    private ProfileManager.Meta[] getSortedIndex() {
        if (mSortedIndex == null) {
            final Set<? extends ProfileManager.Meta> m = includeBuiltIns ?
                    getManager().enumerate() : getManager().enumerateCustom();
            final ProfileManager.Meta[] keys = m.toArray(new ProfileManager.Meta[0]);
            Arrays.sort(keys, onGetSortOrder());
            mSortedIndex = keys;
        }
        return mSortedIndex;
    }

    @NonNull
    public final String getName(final int i) {
        return getMeta(i).name;
    }

    @NonNull
    public final ProfileManager.Meta getMeta(final int i) {
        if (zeroEntry != null) {
            return i == 0 ? zeroEntry : getSortedIndex()[i - 1];
        }
        return getSortedIndex()[i];
    }

    public final int getPosition(@Nullable final String name) {
        final ProfileManager.Meta meta = getManager().getMeta(name);
        if (meta == null)
            return -1;
        return getPosition(meta);
    }

    public final int getPosition(@NonNull final ProfileManager.Meta meta) {
        if (meta.equals(zeroEntry)) {
            return 0;
        }
        final ProfileManager.Meta[] sortedIndex = getSortedIndex();
        for (int i = 0; i < sortedIndex.length; ++i) {
            if (sortedIndex[i].equals(meta)) {
                return i + (zeroEntry != null ? 1 : 0);
            }
        }
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
        updateSortedIndex.run();
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

    /**
     * For weird cases when you prefer to use a custom view instead of any sort of
     * {@link android.widget.Spinner}.
     *
     * @param parent to inflate into
     * @return a view to add into the {@code parent} and bind
     * @see #bindEntryTo(View, ProfileManager.Meta)
     */
    @NonNull
    public View createEntryFor(@NonNull final ViewGroup parent) {
        return onCreateDropDownView(parent, DEFAULT_TYPE);
    }

    /**
     * For weird cases when you prefer to use a custom view instead of any sort of
     * {@link android.widget.Spinner}.
     *
     * @param view to bind
     * @param meta to bind
     * @see #createEntryFor(ViewGroup)
     */
    public void bindEntryTo(@NonNull final View view, @NonNull final ProfileManager.Meta meta) {
        onBind(view, meta);
    }

    @Override
    protected int onGetCount() {
        return getSortedIndex().length + (zeroEntry != null ? 1 : 0);
    }

    @Override
    @NonNull
    protected ProfileManager.Meta onGetItem(final int position) {
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
        onBind(view, getMeta(position));
    }

    protected void onBind(@NonNull final View view, @NonNull final ProfileManager.Meta meta) {
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
