package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jcraft.jsch.JSch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.SshHostKey;
import green_green_avk.anotherterm.utils.SshHostKeyRepository;

public class SshHostKeyRepositoryView extends RecyclerView {

    public SshHostKeyRepositoryView(@NonNull final Context context) {
        super(context);
        init();
    }

    public SshHostKeyRepositoryView(@NonNull final Context context,
                                    @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SshHostKeyRepositoryView(@NonNull final Context context,
                                    @Nullable final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        setLayoutManager(new LinearLayoutManager(getContext()));
        setAdapter(new Adapter(getContext()));
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull final View v) {
            super(v);
        }
    }

    protected static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        protected final SshHostKeyRepository repo;
        protected final JSch jSch = new JSch();
        protected List<SshHostKey> keys;

        public Adapter(@NonNull final Context ctx) {
            super();
            repo = new SshHostKeyRepository(ctx);
            refreshKeys();
        }

        protected void refreshKeys() {
            keys = new ArrayList<>(repo.getHostKeySet());
            Collections.sort(keys, new Comparator<SshHostKey>() {
                @Override
                public int compare(final SshHostKey o1, final SshHostKey o2) {
                    return (o1.getHost() + o1.getType() + o1.getFingerPrint(jSch))
                            .compareTo(o2.getHost() + o2.getType() + o2.getFingerPrint(jSch));
                }
            });
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ssh_host_key_repository_entry, parent, false);
            final View bMenu = v.findViewById(R.id.b_menu);
            UiUtils.setShowContextMenuOnClick(bMenu);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final SshHostKey key = keys.get(position);
            final TextView hostnameView = holder.itemView.findViewById(R.id.f_hostname);
            final TextView typeView = holder.itemView.findViewById(R.id.f_type);
            final TextView fingerprintView = holder.itemView.findViewById(R.id.f_fingerprint);
            hostnameView.setText(key.getHost());
            typeView.setText(key.getType());
            fingerprintView.setText(key.getFingerPrint(jSch));
            holder.itemView.findViewById(R.id.b_menu).setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(final ContextMenu menu, final View v,
                                                final ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add(0, R.string.action_delete, 0, R.string.action_delete)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(final MenuItem item) {
                                    switch (item.getItemId()) {
                                        case R.string.action_delete:
                                            repo.remove(key);
                                            refreshKeys();
                                            notifyDataSetChanged();
                                            return true;
                                    }
                                    return false;
                                }
                            });
                }
            });
        }

        @Override
        public int getItemCount() {
            return keys.size();
        }
    }
}
