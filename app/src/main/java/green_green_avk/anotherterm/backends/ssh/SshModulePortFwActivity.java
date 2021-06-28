package green_green_avk.anotherterm.backends.ssh;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.Misc;

public final class SshModulePortFwActivity extends AppCompatActivity {
    static final String IFK_SSH_SESS_KEY =
            SshModulePortFwActivity.class.getName() + ".SSH_SESS_KEY";

    private static final int LOCAL = 0;
    private static final int REMOTE = 1;

    private static final String DEFAULT_HOST = "127.0.0.1";

    private long sshSessionKey = -1;

    private final List<SshModule.PortMapping> locals = new ArrayList<>();
    private final List<SshModule.PortMapping> remotes = new ArrayList<>();

    private static void parsePortMappings(@NonNull final List<SshModule.PortMapping> r,
                                          @NonNull final String[] v) {
        for (final String ve : v) {
            final SshModule.PortMapping re = new SshModule.PortMapping();
            parsePortMapping(re, ve);
            r.add(re);
        }
    }

    private static void parsePortMapping(@NonNull final SshModule.PortMapping r,
                                         @NonNull final String v) {
        final String[] ee = v.split(":", 3);
        r.srcPort = Integer.parseInt(ee[0]);
        r.host = ee[1];
        r.dstPort = Integer.parseInt(ee[2]);
    }

    private static void makeReadonly(@NonNull final EditText view) {
        view.setEnabled(false);
        view.setClickable(false);
        view.setLongClickable(false);
        view.setFocusable(false);
        view.setHint("");
    }

    private static final class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    private final class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                             final int viewType) {
            final View root = LayoutInflater.from(parent.getContext())
                    .inflate(viewType == LOCAL ? R.layout.portfwl_entry : R.layout.portfwr_entry,
                            parent, false);
            return new ViewHolder(root);
        }

        @Override
        public int getItemViewType(final int position) {
            return position < locals.size() ? LOCAL : REMOTE;
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder,
                                     final int position) {
            final EditText wSrcPort = holder.itemView.findViewById(R.id.src_port);
            final EditText wHost = holder.itemView.findViewById(R.id.host);
            final EditText wDstPort = holder.itemView.findViewById(R.id.dst_port);
            makeReadonly(wSrcPort);
            makeReadonly(wHost);
            makeReadonly(wDstPort);
            final SshModule.PortMapping elt = position < locals.size() ?
                    locals.get(position) : remotes.get(position - locals.size());
            wSrcPort.setText(Integer.toString(elt.srcPort));
            wHost.setText(elt.host);
            wDstPort.setText(Integer.toString(elt.dstPort));
            holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                getMenuInflater().inflate(R.menu.menu_portfw, menu);
                menu.findItem(R.id.action_delete).setOnMenuItemClickListener(item -> {
                    final SshModule.SshSessionSt sshSessionSt =
                            SshModule.sshSessionSts.get(sshSessionKey);
                    if (sshSessionSt == null) return true;
                    synchronized (sshSessionSt.lock) {
                        final Session session = sshSessionSt.session;
                        if (session == null) return true;
                        try {
                            if (position < locals.size())
                                session.delPortForwardingL(elt.srcPort);
                            else
                                Misc.runAsyncWeak(() -> {
                                    try {
                                        session.delPortForwardingR(elt.srcPort);
                                    } catch (final JSchException ignored) {
                                    }
                                }, onUpdate);
                        } catch (final JSchException | NumberFormatException ignored) {
                        }
                    }
                    update();
                    return true;
                });
            });
        }

        @Override
        public int getItemCount() {
            return locals.size() + remotes.size();
        }
    }

    private final Comparator<SshModule.PortMapping> listSortOrder =
            (o1, o2) -> Integer.compare(o1.srcPort, o2.srcPort);

    private void loadLists() {
        final SshModule.SshSessionSt sshSessionSt = SshModule.sshSessionSts.get(sshSessionKey);
        if (sshSessionSt == null) return;
        synchronized (sshSessionSt.lock) {
            final Session session = sshSessionSt.session;
            try {
                parsePortMappings(locals, session.getPortForwardingL());
                Collections.sort(locals, listSortOrder);
            } catch (final JSchException ignored) {
            }
            try {
                parsePortMappings(remotes, session.getPortForwardingR());
                Collections.sort(remotes, listSortOrder);
            } catch (final JSchException ignored) {
            }
        }
    }

    private void update() {
        locals.clear();
        remotes.clear();
        loadLists();
        wList.getAdapter().notifyDataSetChanged();
    }

    @Keep
    private final Runnable onUpdate = this::update;

    private RecyclerView wList = null;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(IFK_SSH_SESS_KEY)) {
            finish();
            return;
        }
        sshSessionKey = getIntent().getLongExtra(IFK_SSH_SESS_KEY, sshSessionKey);
        loadLists();
        setContentView(R.layout.ssh_module_portfw_activity);
        wList = findViewById(R.id.list);
        wList.setLayoutManager(new LinearLayoutManager(this));
        wList.setAdapter(new Adapter());
    }

    public void addPortForwardingLocal(final View view) {
        addPortForwarding(LOCAL);
    }

    public void addPortForwardingRemote(final View view) {
        addPortForwarding(REMOTE);
    }

    private void addPortForwarding(final int type) {
        final View root = LayoutInflater.from(this).inflate(type == LOCAL ?
                R.layout.portfwl_entry : R.layout.portfwr_entry, null);
        final EditText wSrcPort = root.findViewById(R.id.src_port);
        final EditText wHost = root.findViewById(R.id.host);
        final EditText wDstPort = root.findViewById(R.id.dst_port);
        wHost.setHint(DEFAULT_HOST);
        new AlertDialog.Builder(this).setView(root)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final SshModule.SshSessionSt sshSessionSt =
                            SshModule.sshSessionSts.get(sshSessionKey);
                    if (sshSessionSt == null) return;
                    synchronized (sshSessionSt.lock) {
                        final Session session = sshSessionSt.session;
                        if (session == null) return;
                        try {
                            final String strSrcPort = wSrcPort.getText().toString();
                            final int srcPort = (strSrcPort.isEmpty() ?
                                    0 : Integer.parseInt(strSrcPort)) & 0xFFFF;
                            String _host = wHost.getText().toString();
                            final String host;
                            if (_host.isEmpty()) host = DEFAULT_HOST;
                            else host = _host;
                            final int dstPort = Integer.parseInt(wDstPort.getText().toString())
                                    & 0xFFFF;
                            if (type == LOCAL)
                                session.setPortForwardingL(srcPort, host, dstPort);
                            else
                                Misc.runAsyncWeak(() -> {
                                    try {
                                        session.setPortForwardingR(srcPort, host, dstPort);
                                    } catch (final JSchException ignored) {
                                    }
                                }, onUpdate);
                        } catch (final JSchException | NumberFormatException ignored) {
                        }
                    }
                    update();
                }).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
        }).show();
    }
}
