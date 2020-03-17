package green_green_avk.anotherterm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public final class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.ViewHolder> {
    private View.OnClickListener mOnClick = null;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener = null;

    private final ConsoleService.Listener mDatasetListener = new ConsoleService.Listener() {
        @Override
        protected void onSessionsListChange() {
            notifyDataSetChanged();
        }

        @Override
        protected void onSessionChange(int key) {
            notifyItemChanged(ConsoleService.sessionKeys.indexOf(key));
        }
    };

    public SessionsAdapter() {
        super();
        ConsoleService.addListener(mDatasetListener);
    }

    public int getKey(int i) {
        return ConsoleService.sessionKeys.get(i);
    }

    public void setOnClickListener(View.OnClickListener v) {
        mOnClick = v;
    }

    public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener v) {
        mOnCreateContextMenuListener = v;
    }

    @NonNull
    @Override
    public SessionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sessions_entry, parent, false);
        v.setOnClickListener(mOnClick);
        v.setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final TextView titleView = holder.itemView.findViewById(R.id.title);
        final int key = ConsoleService.sessionKeys.get(position);
        final Session session = ConsoleService.getSession(key);
        String title = session.input.currScrBuf.windowTitle;
        if (title == null) title = ConsoleService.getSessionTitle(key);
        titleView.setText(title);
        ImageView thumbnailView = holder.itemView.findViewById(R.id.thumbnail);
        if (session.thumbnail != null) {
            thumbnailView.setImageBitmap(session.thumbnail);
        } else {
            thumbnailView.setImageResource(R.drawable.list_item_background);
        }
        final TextView descriptionView = holder.itemView.findViewById(R.id.description);
        descriptionView.setText(session.backend.wrapped.getConnDesc());
        final TextView stateView = holder.itemView.findViewById(R.id.state);
        stateView.setText(session.backend.isConnected() ? R.string.msg_connected : R.string.msg_disconnected);
    }

    @Override
    public int getItemCount() {
        return ConsoleService.sessionKeys.size();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(final View v) {
            super(v);
        }
    }
}
