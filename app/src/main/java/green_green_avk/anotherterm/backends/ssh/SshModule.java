package green_green_avk.anotherterm.backends.ssh;

import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendInterruptedException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.utils.SshHostKeyRepository;

public final class SshModule extends BackendModule {

    private static final long CERT_FILE_SIZE_MAX = 1024 * 1024;

    @Keep
    public static final Meta meta = new Meta(SshModule.class, "ssh") {
        @Override
        @NonNull
        public Map<String, ?> getDefaultParameters() {
            final Map<String, Object> r = new HashMap<>();
            r.put("jsch.cfg.kex", JSch.getConfig("kex"));
            r.put("jsch.cfg.cipher.s2c", JSch.getConfig("cipher.s2c"));
            r.put("jsch.cfg.cipher.c2s", JSch.getConfig("cipher.c2s"));
            r.put("jsch.cfg.mac.s2c", JSch.getConfig("mac.s2c"));
            r.put("jsch.cfg.mac.c2s", JSch.getConfig("mac.c2s"));
            return r;
        }

        @Override
        @NonNull
        public Map<String, ?> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque()) throw new ParametersUriParseException();
            final Map<String, Object> params = new HashMap<>();
            for (final String k : uri.getQueryParameterNames()) {
                switch (k) {
                    case "X11":
                        params.put(k, uri.getBooleanQueryParameter(k, false));
                        break;
                    default:
                        // TODO: '+' decoding issue before Jelly Bean
                        params.put(k, uri.getQueryParameter(k));
                }
            }
            final String hostname = uri.getHost();
            if (hostname != null) {
                params.put("hostname", hostname);
                final String username = uri.getUserInfo();
                if (username != null) params.put("username", username);
                final int port = uri.getPort();
                if (port >= 0) params.put("port", port);
                final String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    params.put("path", path);
                    final StringBuilder b = new StringBuilder().append("cd \"")
                            .append(StringEscapeUtils.escapeXSI(path)).append("\"");
                    final String execute = (String) params.get("execute");
                    if (execute != null && !execute.isEmpty()) {
                        b.append(" && ( ").append(execute).append(" )");
                    } else {
                        b.append(" ; $SHELL -l");
                    }
                    params.put("execute", b.toString());
                }
            }
            return params;
        }

        @Override
        @NonNull
        public Uri toUri(@NonNull final Map<String, ?> params) {
            final String auth = String.format(Locale.ROOT, "%s@%s:%s",
                    URLEncoder.encode(params.get("username").toString()),
                    params.get("hostname").toString(),
                    params.get("port").toString());
            final Uri.Builder b = new Uri.Builder()
                    .scheme(getUriSchemes().iterator().next())
                    .encodedAuthority(auth);
            for (final String k : params.keySet()) {
                switch (k) {
                    case "username":
                    case "hostname":
                    case "port":
                        break;
                    case "path": {
                        final Object o = params.get(k);
                        if (o == null) break;
                        final String p = o.toString();
                        if (p.isEmpty()) break;
                        b.path(p);
                        break;
                    }
                    default: {
                        final Object o = params.get(k);
                        if (o == null) break;
                        b.appendQueryParameter(k, o.toString());
                    }
                }
            }
            return b.build();
        }
    };

    private String hostname;
    private int port = 22;
    private String username;
    private String kex = JSch.getConfig("kex");
    private String cipher_s2c = JSch.getConfig("cipher.s2c");
    private String cipher_c2s = JSch.getConfig("cipher.c2s");
    private String mac_s2c = JSch.getConfig("mac.s2c");
    private String mac_c2s = JSch.getConfig("mac.c2s");
    private boolean preferKeyAuth = false;
    private String terminalString = "xterm";
    private String execute = "";
    private int keepaliveInterval = 0;
    private boolean preferCompression = false;

    private boolean X11 = false;
    private String X11Host = "127.0.0.1";
    private int X11Port = 0;

    private final Set<PortMapping> localPortMappings = new HashSet<>();
    private final Set<PortMapping> remotePortMappings = new HashSet<>();

    private static final class PortMapping {
        private int srcPort = 0;
        private int dstPort = 0;
        private String host = "127.0.0.1";

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (!(obj instanceof PortMapping)) return false;
            final PortMapping o = (PortMapping) obj;
            return srcPort == o.srcPort && dstPort == o.dstPort && host.equals(o.host);
        }

        @Override
        public int hashCode() {
            return srcPort + dstPort + host.hashCode(); // no cache, not often accessible
        }
    }

    private static final Pattern portMappingP =
            Pattern.compile("^([0-9]+)(?:>([^:]*)(?::([0-9]+))?)?$");

    private static void parsePortMappings(@NonNull final Set<PortMapping> set,
                                          @NonNull String unparsed) {
        unparsed = unparsed.replaceAll("\\s", "");
        for (final String s : unparsed.split(";")) {
            if (s.isEmpty()) continue;
            final Matcher m = portMappingP.matcher(s);
            try {
                if (!m.matches()) throw new NumberFormatException();
                final PortMapping pm = new PortMapping();
                String t = m.group(1);
                pm.srcPort = Integer.parseInt(t);
                t = m.group(2);
                if (t != null) pm.host = t;
                t = m.group(3);
                pm.dstPort = (t != null) ? Integer.parseInt(t) : pm.srcPort;
                set.add(pm);
            } catch (final NumberFormatException e) {
                throw new BackendException(String.format("Port value `%s' seems malformed", s));
            }
        }
    }

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        hostname = pp.getString("hostname", null);
        if (hostname == null) throw new BackendException("`hostname' is not defined");

        port = pp.getInt("port", port);

        username = pp.getString("username", null);
        if (username == null) throw new BackendException("`username' is not defined");

        kex = pp.getString("jsch.cfg.kex", kex);
        cipher_s2c = pp.getString("jsch.cfg.cipher.s2c", cipher_s2c);
        cipher_c2s = pp.getString("jsch.cfg.cipher.c2s", cipher_c2s);
        mac_s2c = pp.getString("jsch.cfg.mac.s2c", mac_s2c);
        mac_c2s = pp.getString("jsch.cfg.mac.c2s", mac_c2s);
        preferKeyAuth = pp.getBoolean("prefer_key_auth", preferKeyAuth);

        terminalString = pp.getString("terminal_string", terminalString);

        execute = pp.getString("execute", execute);

        keepaliveInterval = pp.getInt("keepalive_interval", keepaliveInterval);

        preferCompression = pp.getBoolean("prefer_compression", preferCompression);

        X11 = pp.getBoolean("X11", X11);
        X11Host = pp.getString("X11_host", X11Host);
        X11Port = pp.getInt("X11_port", X11Port);

        parsePortMappings(localPortMappings, pp.getString("local_ports", ""));
        parsePortMappings(remotePortMappings, pp.getString("remote_ports", ""));
    }

    private final JSch jsch = new JSch();
    private Session session = null;
    private Channel channel = null;
    private OutputStream mOS_set = null;
    private OutputStream mOS_get_orig = null;

    private final OutputStream mOS_get = new OutputStream() {
        @Override
        public void close() {
            if (mOS_get_orig == null) return;
            try {
                mOS_get_orig.close();
            } catch (final SshHostKeyRepository.Exception e) {
                disconnect();
                throw new BackendException(e);
            } catch (final IOException e) {
                disconnect();
                throw new BackendException(e);
            }
        }

        @Override
        public void flush() {
            if (mOS_get_orig == null) return;
            try {
                mOS_get_orig.flush();
            } catch (final SshHostKeyRepository.Exception e) {
                disconnect();
                throw new BackendException(e);
            } catch (final IOException e) {
                disconnect();
                throw new BackendException(e);
            }
        }

        @Override
        public void write(final int b) {
            if (mOS_get_orig == null) return;
            try {
                mOS_get_orig.write(b);
            } catch (final SshHostKeyRepository.Exception e) {
                disconnect();
                throw new BackendException(e);
            } catch (final IOException e) {
                disconnect();
                throw new BackendException(e);
            }
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            if (mOS_get_orig == null) return;
            try {
                mOS_get_orig.write(b, off, len);
            } catch (final SshHostKeyRepository.Exception e) {
                disconnect();
                throw new BackendException(e);
            } catch (final IOException e) {
                disconnect();
                throw new BackendException(e);
            }
        }

        @Override
        public void write(final byte[] b) {
            if (mOS_get_orig == null) return;
            try {
                mOS_get_orig.write(b);
            } catch (final SshHostKeyRepository.Exception e) {
                disconnect();
                throw new BackendException(e);
            } catch (final IOException e) {
                disconnect();
                throw new BackendException(e);
            }
        }
    };

    private final UserInfo userInfo = new UserInfo() {
        private String password = null;
        private String passphrase = null;

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getPassphrase() {
            return passphrase;
        }

        @Override
        public boolean promptPassword(final String s) {
            try {
                password = ui.promptPassword(s);
            } catch (final InterruptedException e) {
                throw new BackendInterruptedException(e);
            }
            return password != null;
        }

        @Override
        public boolean promptPassphrase(final String s) {
            try {
                passphrase = ui.promptPassword(s);
            } catch (final InterruptedException e) {
                throw new BackendInterruptedException(e);
            }
            return passphrase != null;
        }

        @Override
        public boolean promptYesNo(final String s) {
            try {
                return ui.promptYesNo(s);
            } catch (final InterruptedException e) {
                throw new BackendInterruptedException(e);
            }
        }

        @Override
        public void showMessage(final String s) {
            ui.showMessage(s);
        }
    };

    // TODO: Nothing can be done with logging without per-instance implementation...
    // TODO: Need introduce appropriate JSch changes before.
    static {
        JSch.setLogger(new Logger() {
            private final SparseBooleanArray enabled = new SparseBooleanArray();

            {
                enabled.put(FATAL, true);
                enabled.put(ERROR, true);
            }

            @Override
            public boolean isEnabled(final int level) {
                return enabled.get(level, false);
            }

            @Override
            public void log(final int level, final String message) {
                Log.e("JSch Error", message);
            }
        });
    }

    {
        final IdentityRepository ir = jsch.getIdentityRepository();
        jsch.setIdentityRepository(new IdentityRepository() {
            private void prompt() {
                byte[] key;
                try {
                    while (true) {
                        try {
                            key = ui.promptContent("Server requests key identification",
                                    "*/*", CERT_FILE_SIZE_MAX);
                            break;
                        } catch (final IOException e) {
                            ui.showToast("Unable to load the key: " + e.getLocalizedMessage());
                        }
                    }
                } catch (final InterruptedException e) {
                    throw new BackendInterruptedException(e);
                }
                if (key == null) return;
                ir.add(key);
            }

            @Override
            public String getName() {
                return "Main Identity Repository";
            }

            @Override
            public int getStatus() {
                return ir.getStatus();
            }

            @Override
            public Vector getIdentities() {
                final Vector ii = ir.getIdentities();
                if (ii.size() > 0) return ii;
                prompt();
                return ir.getIdentities();
            }

            @Override
            public boolean add(final byte[] identity) {
                return ir.add(identity);
            }

            @Override
            public boolean remove(final byte[] blob) {
                return ir.remove(blob);
            }

            @Override
            public void removeAll() {
                ir.removeAll();
            }
        });
    }

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        mOS_set = new OutputStream() {
            public void write(final int b) throws IOException {
                stream.write(b);
            }

            public void write(final byte[] b) throws IOException {
                stream.write(b);
            }

            public void write(final byte[] b, final int off, final int len) throws IOException {
                stream.write(b, off, len);
            }

            public void flush() throws IOException {
                stream.flush();
            }

            public void close() throws IOException {
                stream.close();
                if (isReleaseWakeLockOnDisconnect()) releaseWakeLock();
            }
        };
    }

    @NonNull
    @Override
    public OutputStream getOutputStream() {
        return mOS_get;
    }

    @Override
    public void setOnMessageListener(@Nullable final OnMessageListener l) { // TODO: use it!
    }

    @Override
    public boolean isConnected() {
        return (channel != null) && channel.isConnected();
    }

    @Override
    public void connect() {
        try {
            session = jsch.getSession(username, hostname, port);
            session.setUserInfo(userInfo);
            session.setHostKeyRepository(new SshHostKeyRepository(context));
            session.setConfig("kex", kex);
            session.setConfig("cipher.s2c", cipher_s2c);
            session.setConfig("cipher.c2s", cipher_c2s);
            session.setConfig("mac.s2c", mac_s2c);
            session.setConfig("mac.c2s", mac_c2s);
            final String cfgComp = preferCompression ? "zlib,none" : "none,zlib";
            session.setConfig("compression.s2c", cfgComp);
            session.setConfig("compression.c2s", cfgComp);
            session.setConfig("StrictHostKeyChecking", "ask");
            session.setConfig("PreferredAuthentications", preferKeyAuth
                    ? "none,publickey,gssapi-with-mic,keyboard-interactive,password"
                    : "none,gssapi-with-mic,keyboard-interactive,password,publickey");
            session.setServerAliveInterval(keepaliveInterval);
            session.setServerAliveCountMax(10);
            if (X11) {
                session.setX11Host(X11Host);
                session.setX11Port(6000 + X11Port);
            }
            session.connect(5000);
            if (execute.isEmpty()) {
                channel = session.openChannel("shell");
                ((ChannelShell) channel).setPty(true);
                ((ChannelShell) channel).setPtyType(terminalString);
            } else {
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setPty(true);
                ((ChannelExec) channel).setPtyType(terminalString);
                ((ChannelExec) channel).setCommand(execute);
                ((ChannelExec) channel).setErrStream(mOS_set);
            }
            channel.setXForwarding(X11);
            channel.setOutputStream(mOS_set);
            mOS_get_orig = channel.getOutputStream();
            channel.connect(3000);
            for (final PortMapping pm : localPortMappings)
                session.setPortForwardingL(pm.srcPort, pm.host, pm.dstPort);
            for (final PortMapping pm : remotePortMappings)
                session.setPortForwardingR(pm.srcPort, pm.host, pm.dstPort);
        } catch (final JSchException e) {
            disconnect();
            throw new BackendException(e.getLocalizedMessage());
        } catch (final SshHostKeyRepository.Exception e) {
            disconnect();
            throw new BackendException(e);
        } catch (final IOException e) {
            disconnect();
            throw new BackendException(e);
        }
        if (isAcquireWakeLockOnConnect()) acquireWakeLock();
    }

    @Override
    public void disconnect() {
        try {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        } finally {
            if (isReleaseWakeLockOnDisconnect()) releaseWakeLock();
        }
    }

    @Override
    public void resize(final int col, final int row, final int wp, final int hp) {
        if (channel != null)
            if (channel instanceof ChannelExec)
                ((ChannelExec) channel).setPtySize(col, row, wp, hp);
            else
                ((ChannelShell) channel).setPtySize(col, row, wp, hp);
    }

    @NonNull
    @Override
    public String getConnDesc() {
        return String.format(Locale.getDefault(), "ssh://%s@%s:%d", username, hostname, port);
    }
}
