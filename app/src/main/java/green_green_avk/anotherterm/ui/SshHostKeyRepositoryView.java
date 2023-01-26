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

import java.util.Arrays;
import java.util.Comparator;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.SshHostKey;
import green_green_avk.anotherterm.utils.SshHostKeyRepository;

public class SshHostKeyRepositoryView extends RecyclerView {
    public SshHostKeyRepositoryView(@NonNull final Context context) {
        super(context);
    }

    public SshHostKeyRepositoryView(@NonNull final Context context,
                                    @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public SshHostKeyRepositoryView(@NonNull final Context context,
                                    @Nullable final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        setAdapter(new Adapter(getContext()));
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull final View v) {
            super(v);
        }
    }

    protected static class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        protected final SshHostKeyRepository repo;
        @Nullable
        private SshHostKey[] keys = null;

        public Adapter(@NonNull final Context ctx) {
            super();
            repo = new SshHostKeyRepository(ctx);
        }

        @NonNull
        private static String getFingerPrintSortKey(@Nullable final SshHostKey key) {
            try {
                return key.getFingerPrint();
            } catch (final RuntimeException e) {
                return "";
            }
        }

        protected static final Comparator<SshHostKey> defaultKeysComparator = (o1, o2) ->
                (o1.getHost() + o1.getType() + getFingerPrintSortKey(o1))
                        .compareTo(o2.getHost() + o2.getType() + getFingerPrintSortKey(o2));

        @NonNull
        protected SshHostKey[] getKeys() {
            if (keys == null) {
                keys = repo.getHostKeySet().toArray(new SshHostKey[0]);
                Arrays.sort(keys, defaultKeysComparator);
            }
            return keys;
        }

        protected final Runnable updateKeys = () -> {
            keys = null;
            notifyDataSetChanged();
        };

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ssh_host_key_repository_entry,
                            parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final SshHostKey key = getKeys()[position];
            final TextView hostnameView = holder.itemView.findViewById(R.id.f_hostname);
            final TextView typeView = holder.itemView.findViewById(R.id.f_type);
            final TextView fingerprintView = holder.itemView.findViewById(R.id.f_fingerprint);
            hostnameView.setText(key.getHost());
            typeView.setText(key.getType());
            CharSequence fp;
            try {
                fp = key.getFingerPrint();
            } catch (final RuntimeException e) {
                fp = holder.itemView.getContext()
                        .getText(R.string.msg_desc_obj_cannot_obtain_fingerprint);
            }
            fingerprintView.setText(fp);
            holder.itemView.findViewById(R.id.action_delete).setOnClickListener(v -> {
                repo.remove(key);
                updateKeys.run();
            });
        }

        @Override
        public int getItemCount() {
            return getKeys().length;
        }
    }
}
