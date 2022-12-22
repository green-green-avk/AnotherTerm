package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.LogMessage;

public class MessageLogView extends LinearLayoutCompat {
    protected final RecyclerView list;
    protected final ViewGroup footer;

    public MessageLogView(final Context context) {
        super(context);
        setOrientation(LinearLayoutCompat.VERTICAL);
    }

    public MessageLogView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageLogView(final Context context, @Nullable final AttributeSet attrs,
                          final int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        list = new RecyclerView(getContext());
        list.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                1f
        ));
        addView(list);
        footer = (ViewGroup) LayoutInflater.from(getContext())
                .inflate(R.layout.message_log_buttons, this, false);
        footer.findViewById(R.id.copy).setOnClickListener(view -> {
            final String r;
            final Adapter a = getAdapter();
            if (a != null && !a.msgs.isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final LogMessage msg : a.msgs) {
                    sb.append(msg.timestamp).append(": ").append(msg.msg).append('\n');
                }
                r = sb.toString();
            } else
                r = null;
            UiUtils.toClipboard(getContext(), r);
        });
        addView(footer);
    }

    public View addButton(@LayoutRes final int layout,
                          @DrawableRes final int icon, @StringRes final int desc,
                          @Nullable final View.OnClickListener listener,
                          final int position) {
        final ImageButton button = (ImageButton) LayoutInflater.from(getContext())
                .inflate(layout, this, false);
        button.setImageResource(icon);
        button.setContentDescription(getContext().getString(desc));
        button.setOnClickListener(listener);
        footer.addView(button, position);
        return button;
    }

    public void setAdapter(@Nullable final Adapter adapter) {
        list.setAdapter(adapter);
    }

    @Nullable
    public Adapter getAdapter() {
        return (Adapter) list.getAdapter();
    }

    public void setLayoutManager(@Nullable final RecyclerView.LayoutManager layout) {
        list.setLayoutManager(layout);
    }

    @Nullable
    public RecyclerView.LayoutManager getLayoutManager() {
        return list.getLayoutManager();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(final View v) {
            super(v);
        }
    }

    public static class Adapter extends RecyclerView.Adapter<MessageLogView.ViewHolder> {
        @NonNull
        protected final List<LogMessage> msgs;

        /**
         * @param msgs messages to show. Should be a {@link  java.util.RandomAccess}
         */
        public Adapter(@NonNull final List<LogMessage> msgs) {
            this.msgs = msgs;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_log_entry, parent, false);
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
