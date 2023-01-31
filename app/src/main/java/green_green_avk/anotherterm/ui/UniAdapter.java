package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public abstract class UniAdapter {
    public static final int DEFAULT_TYPE = 0;

    @SuppressLint("NotifyDataSetChanged")
    protected final void notifyDataSetChanged() {
        if (adapter != null)
            adapter.notifyDataSetChanged();
        if (recyclerAdapter != null)
            recyclerAdapter.notifyDataSetChanged();
    }

    protected final void notifyItemRangeChanged(final int start, final int count) {
        if (adapter != null)
            adapter.notifyDataSetChanged();
        if (recyclerAdapter != null)
            recyclerAdapter.notifyItemRangeChanged(start, count);
    }

    protected final void notifyItemRangeInserted(final int start, final int count) {
        if (adapter != null)
            adapter.notifyDataSetChanged();
        if (recyclerAdapter != null)
            recyclerAdapter.notifyItemRangeInserted(start, count);
    }

    protected final void notifyItemRangeRemoved(final int start, final int count) {
        if (adapter != null)
            adapter.notifyDataSetChanged();
        if (recyclerAdapter != null)
            recyclerAdapter.notifyItemRangeRemoved(start, count);
    }

    protected abstract int onGetCount();

    @NonNull
    protected abstract View onCreateView(@NonNull ViewGroup parent, int type);

    @NonNull
    protected View onCreateDropDownView(@NonNull final ViewGroup parent, final int type) {
        return onCreateView(parent, type);
    }

    protected abstract void onBind(@NonNull View view, int position);

    protected int onGetTypeCount() {
        return 1;
    }

    protected int onGetType(final int position) {
        return DEFAULT_TYPE;
    }

    protected final boolean onHasStableIds() {
        return false;
    }

    protected int onGetItemId(final int position) {
        return position;
    }

    @Nullable
    protected Object onGetItem(final int position) {
        return null;
    }

    private final class AdapterImpl extends BaseAdapter {
        private final UniAdapter owner = UniAdapter.this;

        @Override
        public int getViewTypeCount() {
            return onGetTypeCount();
        }

        @Override
        public int getItemViewType(final int position) {
            return onGetType(position);
        }

        @Override
        public boolean hasStableIds() {
            return onHasStableIds();
        }

        @Override
        public int getCount() {
            return onGetCount();
        }

        @Override
        public Object getItem(final int position) {
            return onGetItem(position);
        }

        @Override
        public long getItemId(final int position) {
            return onGetItemId(position);
        }

        @Override
        public View getView(final int position, final View convertView,
                            final ViewGroup parent) {
            final View view = convertView == null ?
                    onCreateView(parent, onGetType(position))
                    : convertView;
            onBind(view, position);
            return view;
        }

        @Override
        public View getDropDownView(final int position, final View convertView,
                                    final ViewGroup parent) {
            final View view = convertView == null ?
                    onCreateDropDownView(parent, onGetType(position))
                    : convertView;
            onBind(view, position);
            return view;
        }
    }

    @Nullable
    private AdapterImpl adapter = null;

    @CheckResult
    @NonNull
    public final BaseAdapter getAdapter() {
        if (adapter == null)
            adapter = new AdapterImpl();
        return adapter;
    }

    @NonNull
    public static <T extends UniAdapter> T getSelf(@Nullable final Adapter adapter) {
        return (T) ((AdapterImpl) adapter).owner;
    }

    private static final class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public RecyclerViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    private final class RecyclerAdapterImpl extends RecyclerView.Adapter<RecyclerViewHolder> {
        private final UniAdapter owner = UniAdapter.this;

        @Override
        @NonNull
        public RecyclerViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                     final int viewType) {
            return new RecyclerViewHolder(onCreateView(parent,
                    viewType));
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerViewHolder holder,
                                     final int position) {
            onBind(holder.itemView, position);
        }

        @Override
        public int getItemViewType(final int position) {
            return onGetType(position);
        }

        @Override
        public long getItemId(final int position) {
            return hasStableIds() ? onGetItemId(position) : RecyclerView.NO_ID;
        }

        @Override
        public int getItemCount() {
            return onGetCount();
        }
    }

    @Nullable
    private RecyclerAdapterImpl recyclerAdapter = null;

    @CheckResult
    @NonNull
    public final RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getRecyclerAdapter() {
        if (recyclerAdapter == null) {
            recyclerAdapter = new RecyclerAdapterImpl();
            recyclerAdapter.setHasStableIds(onHasStableIds());
        }
        return recyclerAdapter;
    }

    @NonNull
    public static <T extends UniAdapter> T getSelf(@Nullable final RecyclerView.Adapter adapter) {
        return (T) ((RecyclerAdapterImpl) adapter).owner;
    }
}
