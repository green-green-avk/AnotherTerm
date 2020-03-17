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

    public MessageLogView(Context context) {
        super(context);
    }

    public MessageLogView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageLogView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View v) {
            super(v);
        }
    }

    public static class Adapter extends RecyclerView.Adapter<MessageLogView.ViewHolder> {

        protected final ArrayList<LogMessage> msgs;

        public Adapter(ArrayList<LogMessage> msgs) {
            this.msgs = msgs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_message_entry, parent, false);
            return new MessageLogView.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
