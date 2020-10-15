package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.LogMessage;

public class MessageLogView extends RecyclerView {

    public MessageLogView(final Context context) {
        super(context);
    }

    public MessageLogView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageLogView(final Context context, @Nullable final AttributeSet attrs,
                          final int defStyle) {
        super(context, attrs, defStyle);
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(final View v) {
            super(v);
        }
    }

    public static class Adapter extends RecyclerView.Adapter<MessageLogView.ViewHolder> {
        @NonNull
        protected final ArrayList<LogMessage> msgs;

        public Adapter(@NonNull final ArrayList<LogMessage> msgs) {
            this.msgs = msgs;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_message_entry, parent, false);
            return new MessageLogView.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final TextView timestamp = holder.itemView.findViewById(R.id.f_timestamp);
            final TextView message = holder.itemView.findViewById(R.id.f_message);
            final LogMessage msg = msgs.get(position);
            timestamp.setText(msg.timestamp.toString());
            message.setText(msg.msg);
        }

        @Override
        public int getItemCount() {
            return msgs.size();
        }
    }
}
