package green_green_avk.anotherterm.backends.ssh;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.ui.forms.EditTextValueBinder;
import green_green_avk.anotherterm.ui.forms.SubmitFormValidator;
import green_green_avk.anotherterm.ui.forms.ViewValueBinderException;
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

    private boolean x11 = false;
    @NonNull
    private String x11Host = "";
    private int x11Port = 0;

    private static void parsePortMappings(@NonNull final List<SshModule.PortMapping> r,
                                          @NonNull final String[] v) {
        for (final String ve : v) {
            final SshModule.PortMapping re = new SshModule.PortMapping();
            parsePortMapping(re, ve);
            r.add(re);
        }
    }

    private static final Pattern portMappingP_jsch =
            Pattern.compile("^([0-9]+):(.*):([0-9]+)$");

    private static void parsePortMapping(@NonNull final SshModule.PortMapping r,
                                         @NonNull final String v) {
        final Matcher m = portMappingP_jsch.matcher(v);
        try {
            if (!m.matches())
                throw new NumberFormatException("Bad port forwarding entry: " + v);
            r.srcPort = Integer.parseInt(m.group(1));
            r.host = m.group(2);
            r.dstPort = Integer.parseInt(m.group(3));
        } catch (final NumberFormatException e) {
            throw new Error("Malformed port forwarding info has been returned by jsch", e);
        }
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
                    Object res = null;
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
                                        return null;
                                    } catch (final JSchException e) {
                                        return e;
                                    }
                                }, onUpdate);
                        } catch (final JSchException e) {
                            res = e;
                        }
                    }
                    update(res);
                    return true;
                });
            });
            holder.itemView.setOnClickListener(View::showContextMenu);
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
        x11 = sshSessionSt.x11;
        x11Host = sshSessionSt.x11Host;
        x11Port = sshSessionSt.x11Port;
    }

    private void update(@Nullable final Object v) {
        if (v instanceof Throwable) {
            Toast.makeText(this, ((Throwable) v).getLocalizedMessage(), Toast.LENGTH_LONG)
                    .show();
        }
        locals.clear();
        remotes.clear();
        loadLists();
        wList.getAdapter().notifyDataSetChanged();
    }

    @Keep
    private final Misc.AsyncResult onUpdate = this::update;

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
        final TextView wX11 = findViewById(R.id.x11HostPort);
        wX11.setVisibility(x11 ? View.VISIBLE : View.GONE);
        wX11.setText(getString(R.string.desc_x11_host_s_port_d,
                x11Host, 6000 + x11Port));
    }

    public void warnImmutableSetting(final View view) {
        Toast.makeText(this,
                R.string.msg_setting_is_immutable_on_a_live_session,
                Toast.LENGTH_SHORT).show();
    }

    public void addPortForwardingLocal(final View view) {
        addPortForwarding(LOCAL);
    }

    public void addPortForwardingRemote(final View view) {
        addPortForwarding(REMOTE);
    }

    private abstract class PortValueBinder extends EditTextValueBinder<Integer> {
        protected int value = 0;
        @NonNull
        protected final SubmitFormValidator inputValidator;

        public PortValueBinder(@NonNull final EditText v, @NonNull final SubmitFormValidator ic) {
            super(v);
            inputValidator = ic;
            check();
        }

        @Nullable
        protected abstract String onEmpty();

        @Nullable
        private String _check(final Editable v) {
            if (v == null)
                return onEmpty();
            final String s = v.toString().trim();
            if (s.isEmpty())
                return onEmpty();
            final int vi;
            try {
                vi = Integer.parseInt(s);
            } catch (final NumberFormatException e) {
                return getString(R.string.msg_bad_port_number);
            }
            if (vi < 1 || vi > 0xFFFF)
                return getString(R.string.msg_bad_port_number);
            value = vi;
            return null;
        }

        @Nullable
        @Override
        protected String onCheck(final Editable v) {
            final String r = _check(v);
            inputValidator.updateMark(this, r != null);
            return r;
        }

        @Override
        protected void onUpdateUi() {
        }

        @Override
        public Integer get() {
            if (warn != null)
                throw new ViewValueBinderException(warn);
            return value;
        }
    }

    private void addPortForwarding(final int type) {
        final SubmitFormValidator inputValidator = new SubmitFormValidator();
        final View root = LayoutInflater.from(this).inflate(type == LOCAL ?
                R.layout.portfwl_entry : R.layout.portfwr_entry, null);
        final EditText wSrcPort = root.findViewById(R.id.src_port);
        final EditText wHost = root.findViewById(R.id.host);
        final EditText wDstPort = root.findViewById(R.id.dst_port);
        final PortValueBinder bSrcPort = new PortValueBinder(wSrcPort, inputValidator) {
            @Nullable
            @Override
            protected String onEmpty() {
                value = 0;
                return null;
            }
        };
        final PortValueBinder bDstPort = new PortValueBinder(wDstPort, inputValidator) {
            @Nullable
            @Override
            protected String onEmpty() {
                return getString(R.string.msg_port_cannot_be_empty);
            }
        };
        wHost.setHint(DEFAULT_HOST);
        final AlertDialog d = new AlertDialog.Builder(this).setView(root)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final SshModule.SshSessionSt sshSessionSt =
                            SshModule.sshSessionSts.get(sshSessionKey);
                    if (sshSessionSt == null) return;
                    Object res = null;
                    synchronized (sshSessionSt.lock) {
                        final Session session = sshSessionSt.session;
                        if (session == null) return;
                        try {
                            final int srcPort = bSrcPort.get();
                            String _host = wHost.getText().toString();
                            final String host;
                            if (_host.isEmpty()) host = DEFAULT_HOST;
                            else host = _host;
                            final int dstPort = bDstPort.get();
                            if (type == LOCAL)
                                session.setPortForwardingL(srcPort, host, dstPort);
                            else
                                Misc.runAsyncWeak(() -> {
                                    try {
                                        session.setPortForwardingR(srcPort, host, dstPort);
                                        return null;
                                    } catch (final JSchException e) {
                                        return e;
                                    }
                                }, onUpdate);
                        } catch (final JSchException | ViewValueBinderException e) {
                            res = e;
                        }
                    }
                    update(res);
                }).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                }).show();
        inputValidator.setSubmitButton(d.getButton(AlertDialog.BUTTON_POSITIVE));
    }
}
