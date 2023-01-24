package green_green_avk.anotherterm;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;

public final class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.ViewHolder> {
    private final ArrayList<Integer> sessionKeys = new ArrayList<>();

    private void rebuildList() {
        sessionKeys.clear();
        sessionKeys.addAll(ConsoleService.sessions.keySet());
        Collections.sort(sessionKeys);
    }

    private View.OnClickListener mOnClick = null;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener = null;

    @Keep // Must be kept to prevent its unexpected collection
    private final ConsoleService.Listener mDatasetListener = new ConsoleService.Listener() {
        @Override
        protected void onSessionsListChange() {
            rebuildList();
            notifyDataSetChanged();
        }

        @Override
        protected void onSessionChange(final int key) {
            if (!ConsoleService.isSessionTerminated(key))
                notifyItemChanged(sessionKeys.indexOf(key));
        }
    };

    public SessionsAdapter() {
        super();
        rebuildList();
        ConsoleService.addListener(mDatasetListener);
    }

    public int getKey(final int i) {
        return sessionKeys.get(i);
    }

    public void setOnClickListener(final View.OnClickListener v) {
        mOnClick = v;
    }

    public void setOnCreateContextMenuListener(final View.OnCreateContextMenuListener v) {
        mOnCreateContextMenuListener = v;
    }

    @NonNull
    @Override
    public SessionsAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent,
                                                         final int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sessions_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(mOnClick);
        holder.itemView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        final TextView titleView = holder.itemView.findViewById(R.id.title);
        final int key = sessionKeys.get(position);
        final Session session = ConsoleService.getSession(key);
        titleView.setText(session.getTitle());
        final ImageView thumbnailView = holder.itemView.findViewById(R.id.thumbnail);
        if (session.thumbnail != null) {
            thumbnailView.setImageBitmap(session.thumbnail);
        } else {
            thumbnailView.setImageResource(R.drawable.ic_thumbnail_empty);
        }
        if (session instanceof AnsiSession &&
                ((AnsiSession) session).uiState.background != null) {
            thumbnailView.setBackgroundDrawable(((AnsiSession) session).uiState.background
                    .getDrawable());
        } else {
            thumbnailView.setBackgroundResource(R.drawable.bg_screen1);
        }
        final TextView descriptionView = holder.itemView.findViewById(R.id.description);
        final TextView stateView = holder.itemView.findViewById(R.id.state);
        if (session instanceof AnsiSession) {
            final EventBasedBackendModuleWrapper tbe = ((AnsiSession) session).backend;
            descriptionView.setText(tbe.wrapped.getConnDesc());
            stateView.setText(tbe.isConnected() ? R.string.msg_connected :
                    tbe.isConnecting() ? R.string.msg_connecting___ :
                            R.string.msg_disconnected);
            if (tbe.wrapped.isWakeLockHeld()) {
                stateView.append(", ");
                final SpannableStringBuilder b = new SpannableStringBuilder().append(holder.itemView
                        .getContext().getString(R.string.label_wake_lock));
                b.setSpan(new ForegroundColorSpan(Color.RED), 0, b.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stateView.append(b);
            }
        } else {
            descriptionView.setText("");
            stateView.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return sessionKeys.size();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(final View v) {
            super(v);
        }
    }
}
