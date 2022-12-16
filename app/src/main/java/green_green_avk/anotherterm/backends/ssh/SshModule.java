package green_green_avk.anotherterm.backends.ssh;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchErrorException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import org.apache.commons.text.StringEscapeUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendInterruptedException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.backends.BackendUiPasswordStorage;
import green_green_avk.anotherterm.ui.BackendUiDialogs;
import green_green_avk.anotherterm.ui.ContentRequester;
import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.Password;
import green_green_avk.anotherterm.utils.SshHostKeyRepository;

public final class SshModule extends BackendModule {

    private static final long CERT_FILE_SIZE_MAX = 1024 * 1024;

    private static boolean fromJSchBoolOpt(@Nullable final String v) {
        if (v == null)
            return false;
        switch (v.charAt(0)) {
            case 'Y':
            case 'y':
            case 'T':
            case 't':
            case '1':
                return true;
        }
        return false;
    }

    @NonNull
    private static String toJSchBoolOpt(final boolean v) {
        return v ? "yes" : "no";
    }

    private static final Map<String, Meta.ParameterMeta<?>> parametersMeta;

    static {
        final Map<String, Meta.ParameterMeta<?>> r = new HashMap<>();
        r.put("jsch.cfg.kex",
                new Meta.ParameterMeta<>("jsch.cfg.kex",
                        JSch.implementedKexSet,
                        JSch.supportedKexSet));
        r.put("jsch.cfg.cipher.s2c",
                new Meta.ParameterMeta<>("jsch.cfg.cipher",
                        JSch.implementedCipherSet,
                        JSch.supportedCipherSet));
        r.put("jsch.cfg.cipher.c2s",
                new Meta.ParameterMeta<>("jsch.cfg.cipher",
                        JSch.implementedCipherSet,
                        JSch.supportedCipherSet));
        r.put("jsch.cfg.mac.s2c",
                new Meta.ParameterMeta<>("jsch.cfg.mac",
                        JSch.implementedMacSet,
                        JSch.supportedMacSet));
        r.put("jsch.cfg.mac.c2s",
                new Meta.ParameterMeta<>("jsch.cfg.mac",
                        JSch.implementedMacSet,
                        JSch.supportedMacSet));
        r.put("jsch.cfg.PubkeyAcceptedAlgorithms",
                new Meta.ParameterMeta<>("jsch.cfg.PubkeyAcceptedAlgorithms",
                        JSch.implementedKeySet,
                        JSch.supportedKeySet));
        r.put("jsch.auth.order", new Meta.ParameterMeta<>("jsch.auth.order",
                JSch.implementedAuthTypes,
                JSch.supportedAuthTypes));
        parametersMeta = Collections.unmodifiableMap(r);
    }

    @Keep
    public static final Meta meta = new Meta(SshModule.class, "ssh") {
        @Override
        @NonNull
        public Map<String, ParameterMeta<?>> getParametersMeta() {
            return parametersMeta;
        }

        @Override
        @NonNull
        public Map<String, ?> getDefaultParameters() {
            final Map<String, Object> r = new HashMap<>();
            r.put("jsch.cfg.kex", JSch.getConfig("kex"));
            r.put("jsch.cfg.cipher.s2c", JSch.getConfig("cipher.s2c"));
            r.put("jsch.cfg.cipher.c2s", JSch.getConfig("cipher.c2s"));
            r.put("jsch.cfg.mac.s2c", JSch.getConfig("mac.s2c"));
            r.put("jsch.cfg.mac.c2s", JSch.getConfig("mac.c2s"));
            r.put("jsch.cfg.PubkeyAcceptedAlgorithms",
                    JSch.getConfig("PubkeyAcceptedAlgorithms"));
            r.put("jsch.cfg.enable_server_sig_algs",
                    fromJSchBoolOpt(JSch.getConfig("enable_server_sig_algs")));
            return r;
        }

        @Override
        @NonNull
        public Map<String, ?> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque())
                throw new ParametersUriParseException();
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
                if (username != null)
                    params.put("username", username);
                final int port = uri.getPort();
                if (port >= 0)
                    params.put("port", port);
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
                        if (o == null)
                            break;
                        final String p = o.toString();
                        if (p.isEmpty())
                            break;
                        b.path(p);
                        break;
                    }
                    default: {
                        final Object o = params.get(k);
                        if (o == null)
                            break;
                        b.appendQueryParameter(k, o.toString());
                    }
                }
            }
            return b.build();
        }

        @Override
        public int getDisconnectionReasonTypes() {
            return DisconnectionReason.PROCESS_EXIT;
        }
    };

    // For UI reference

    private static final AtomicLong currSshSessionKey = new AtomicLong(0);

    private static long obtainSshSessionKey() {
        return currSshSessionKey.getAndIncrement();
    }

    static final Map<Long, SshSessionSt> sshSessionSts =
            Collections.synchronizedMap(new WeakHashMap<>());

    // ===

    static final class SshSessionSt {
        private String hostname = null;
        private int port = 22;
        private String username = null;
        private String kex = JSch.getConfig("kex");
        private String cipher_s2c = JSch.getConfig("cipher.s2c");
        private String cipher_c2s = JSch.getConfig("cipher.c2s");
        private String mac_s2c = JSch.getConfig("mac.s2c");
        private String mac_c2s = JSch.getConfig("mac.c2s");
        private String pubkey_accepted_algorithms = JSch.getConfig("PubkeyAcceptedAlgorithms");
        private boolean enable_server_sig_algs =
                fromJSchBoolOpt(JSch.getConfig("enable_server_sig_algs"));
        private boolean preferKeyAuth = false;
        private Uri authKeyUri = null;
        private int keepaliveInterval = 0;
        private boolean preferCompression = false;

        boolean x11 = false;
        @NonNull
        String x11Host = "127.0.0.1";
        int x11Port = 0;

        private final Set<PortMapping> localPortMappings = new HashSet<>();
        private final Set<PortMapping> remotePortMappings = new HashSet<>();

        private final JSch jsch = new JSch();
        private Context context = null;
        volatile Session session = null;
        final Object lock = new Object();
        private final AtomicLong refs = new AtomicLong(0);
        private final BackendUiDialogs ui = new BackendUiDialogs();

        private long key = -1; // For UI reference
    }

    static final class PortMapping {
        int srcPort = 0;
        int dstPort = 0;
        String host = "127.0.0.1";

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (!(obj instanceof PortMapping))
                return false;
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
            if (s.isEmpty())
                continue;
            final Matcher m = portMappingP.matcher(s);
            try {
                if (!m.matches())
                    throw new NumberFormatException();
                final PortMapping pm = new PortMapping();
                String t = m.group(1);
                pm.srcPort = Integer.parseInt(t);
                t = m.group(2);
                if (t != null)
                    pm.host = t;
                t = m.group(3);
                pm.dstPort = (t != null) ? Integer.parseInt(t) : pm.srcPort;
                set.add(pm);
            } catch (final NumberFormatException e) {
                throw new BackendException(String.format("Port value `%s' seems malformed", s));
            }
        }
    }

    @NonNull
    private final SshSessionSt sshSessionSt;

    private boolean silentlyUseSavedPasswords = false;
    private boolean savePasswords = silentlyUseSavedPasswords;
    @NonNull
    private String terminalString = "xterm";
    @NonNull
    private String execute = "";
    private boolean x11 = false; // per-channel usage

    public SshModule() {
        sshSessionSt = new SshSessionSt();
    }

    private SshModule(@NonNull final SshModule that) {
        sshSessionSt = that.sshSessionSt;
        terminalString = that.terminalString;
        execute = that.execute;
        x11 = that.x11;
    }

    @Override
    public void setContext(@NonNull final Context context) {
        super.setContext(context);
        sshSessionSt.context = context.getApplicationContext();
    }

    @Override
    public void setUi(final BackendUiInteraction ui) {
        super.setUi(ui);
        if (ui instanceof BackendUiDialogs) {
            ((BackendUiDialogs) ui).parent = sshSessionSt.ui;
        }
    }

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        sshSessionSt.hostname = pp.getString("hostname", null);
        if (sshSessionSt.hostname == null)
            throw new BackendException("`hostname' is not defined");

        sshSessionSt.port = pp.getInt("port", sshSessionSt.port);

        sshSessionSt.username = pp.getString("username", null);
        if (sshSessionSt.username == null)
            throw new BackendException("`username' is not defined");

        sshSessionSt.kex = pp.getString("jsch.cfg.kex", sshSessionSt.kex);
        sshSessionSt.cipher_s2c = pp.getString("jsch.cfg.cipher.s2c", sshSessionSt.cipher_s2c);
        sshSessionSt.cipher_c2s = pp.getString("jsch.cfg.cipher.c2s", sshSessionSt.cipher_c2s);
        sshSessionSt.mac_s2c = pp.getString("jsch.cfg.mac.s2c", sshSessionSt.mac_s2c);
        sshSessionSt.mac_c2s = pp.getString("jsch.cfg.mac.c2s", sshSessionSt.mac_c2s);
        sshSessionSt.pubkey_accepted_algorithms =
                pp.getString("jsch.cfg.PubkeyAcceptedAlgorithms",
                        sshSessionSt.pubkey_accepted_algorithms);
        sshSessionSt.enable_server_sig_algs =
                pp.getBoolean("jsch.cfg.enable_server_sig_algs",
                        sshSessionSt.enable_server_sig_algs);
        sshSessionSt.preferKeyAuth = pp.getBoolean("prefer_key_auth",
                sshSessionSt.preferKeyAuth);
        final String authKeyUriStr = pp.getString("auth_key_uri", null);
        if (authKeyUriStr != null)
            sshSessionSt.authKeyUri = Uri.parse(authKeyUriStr);

        silentlyUseSavedPasswords = pp.getBoolean("silently_use_saved_passwords",
                silentlyUseSavedPasswords);
        savePasswords = silentlyUseSavedPasswords;
        terminalString = pp.getString("terminal_string", terminalString);
        execute = pp.getString("execute", execute);

        sshSessionSt.keepaliveInterval = pp.getInt("keepalive_interval",
                sshSessionSt.keepaliveInterval);

        sshSessionSt.preferCompression = pp.getBoolean("prefer_compression",
                sshSessionSt.preferCompression);

        sshSessionSt.x11 = pp.getBoolean("X11", sshSessionSt.x11);
        x11 = sshSessionSt.x11;
        sshSessionSt.x11Host = pp.getString("X11_host", sshSessionSt.x11Host);
        sshSessionSt.x11Port = pp.getInt("X11_port", sshSessionSt.x11Port);

        parsePortMappings(sshSessionSt.localPortMappings,
                pp.getString("local_ports", ""));
        parsePortMappings(sshSessionSt.remotePortMappings,
                pp.getString("remote_ports", ""));
    }

    private volatile Channel channel = null;
    private volatile Integer channelExitStatus = null;

    private static String ifHas(final String o, final String v) {
        return o == null || o.isEmpty() ? "" : v;
    }

    private static final String[] connectionOpenFailureReasons = new String[]{
            "SSH_OPEN_ADMINISTRATIVELY_PROHIBITED",
            "SSH_OPEN_CONNECT_FAILED",
            "SSH_OPEN_UNKNOWN_CHANNEL_TYPE",
            "SSH_OPEN_RESOURCE_SHORTAGE"
    };

    private static String getConnectionOpenFailureReasonStr(final int v) {
        try {
            return connectionOpenFailureReasons[v - 1];
        } catch (final ArrayIndexOutOfBoundsException e) {
            return "<unknown>";
        }
    }

    private final Runnable mOnDisconnect = () -> {
        final Channel ch = channel;
        if (ch == null)
            return;
        final Channel.ExitStatus status = ch.getExitStatus();
        if (status instanceof Channel.ProcessExitStatus) {
            channelExitStatus = ((Channel.ProcessExitStatus) status).value;
            reportState(new DisconnectStateMessage("Remote process exited with status " +
                    ((Channel.ProcessExitStatus) status).value));
        } else if (status instanceof Channel.ProcessSignalExitStatus) {
            final Channel.ProcessSignalExitStatus st =
                    (Channel.ProcessSignalExitStatus) status;
            reportState(new DisconnectStateMessage("Remote process exited with signal " +
                    st.signalName + (st.coreDumped ? " <core dumped>" : "") +
                    ifHas(st.errorMessage, "\n" + st.errorMessage +
                            ifHas(st.languageTag, " [" + st.languageTag + "]"))));
        } else if (status == Channel.CLOSED_EXIT_STATUS) {
            reportState(new DisconnectStateMessage(
                    "SSH channel closed without any exit status"));
        } else if (status instanceof Channel.ConnectionOpenFailureExitStatus) {
            final Channel.ConnectionOpenFailureExitStatus st =
                    (Channel.ConnectionOpenFailureExitStatus) status;
            reportState(new DisconnectStateMessage("SSH channel open failed with status " +
                    st.reason + ": " + getConnectionOpenFailureReasonStr(st.reason) +
                    ifHas(st.description, "\n" + st.description +
                            ifHas(st.languageTag, " [" + st.languageTag + "]"))));
        } else if (status == Channel.EOF_EXIT_STATUS) {
            reportState(new DisconnectStateMessage(
                    "SSH channel terminated due to a protocol error after a remote EOF received"));
        } else {
            reportState(new DisconnectStateMessage(
                    "SSH channel terminated due to a protocol error"));
        }
        if (isReleaseWakeLockOnDisconnect())
            releaseWakeLock();
    };

    private OutputStream mOS_set = null;
    private OutputStream mOS_get_orig = null;

    private final OutputStream mOS_get = new OutputStream() {
        @Override
        public void close() {
            if (mOS_get_orig == null)
                return;
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
            if (mOS_get_orig == null)
                return;
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
            if (mOS_get_orig == null)
                return;
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
            if (mOS_get_orig == null)
                return;
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
            if (mOS_get_orig == null)
                return;
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

    private interface UserInfoKi extends UserInfo, UIKeyboardInteractive {
    }

    private final UserInfoKi userInfo = new UserInfoKi() {
        private CharSequence buildMessage(final int message, final Object... args) {
            try {
                switch (message) {
                    case Message.SIMPLE_MESSAGE:
                        return (CharSequence) args[0];
                    case Message.PASSWORD_FOR_HOST:
                        return sshSessionSt.context.getString(
                                R.string.msg_password_for_host_s,
                                args);
                    case Message.PASSWORD_FOR_HOST_CHANGE:
                        return sshSessionSt.context.getString(
                                R.string.msg_password_for_host_change_s_s,
                                args[0], args[1]);
                    case Message.PASSPHRASE_FOR_KEY:
                        return sshSessionSt.context.getString(
                                R.string.msg_passphrase_for_key_s,
                                args);
                    case Message.REMOTE_IDENTITY_NEW_ASK_PROCEED:
                        return sshSessionSt.context.getString(
                                R.string.msg_ssh_remote_identity_new_ask_proceed,
                                args);
                    case Message.REMOTE_IDENTITY_CHANGED:
                        return sshSessionSt.context.getString(
                                R.string.msg_ssh_remote_identity_changed,
                                args);
                    case Message.REMOTE_IDENTITY_CHANGED_ASK_PROCEED:
                        return sshSessionSt.context.getString(
                                R.string.msg_ssh_remote_identity_changed_ask_proceed,
                                args);
                    default:
                        throw new BackendException("Incoherent message from SSH library...");
                }
            } catch (final ClassCastException | IllegalFormatException e) {
                throw new BackendException(e);
            }
        }

        @Override
        public void erase(final CharSequence v) {
            if (v != null)
                ui.erase(v);
        }

        @Override
        public void erase(final CharSequence[] v) {
            if (v != null)
                for (final CharSequence c : v)
                    erase(c);
        }

        private static final String modulePrefix = "ssh@";

        @Override
        public void onAuthResult(final int result,
                                 final String id, final SensitiveStringProvider v) {
            if (savePasswords && ui instanceof BackendUiPasswordStorage) {
                switch (result) {
                    case Result.SUCCESS: {
                        final Password pwd = Password.adopt(v.get());
                        ((BackendUiPasswordStorage) ui).putPassword(modulePrefix + id, pwd);
                        pwd.erase();
                        break;
                    }
                    case Result.FAILURE: {
                        final Password pwd = Password.adopt(v.get());
                        ((BackendUiPasswordStorage) ui).erasePassword(modulePrefix + id, pwd);
                        pwd.erase();
                        break;
                    }
                }
            }
        }

        @Override
        public CharSequence[] promptKeyboardInteractive(final String destination,
                                                        final String name,
                                                        final String instruction,
                                                        final String[] prompt,
                                                        final boolean[] echo) {
            final List<BackendUiInteraction.CustomFieldOpts> fieldsOpts =
                    new ArrayList<>(4 + prompt.length);
            fieldsOpts.add(new BackendUiInteraction.CustomFieldOpts(sshSessionSt.context
                    .getText(R.string.note_ssh_trying_keyboard_interactive___),
                    BackendUiInteraction.CustomFieldAction.label));
            fieldsOpts.add(new BackendUiInteraction.CustomFieldOpts(destination,
                    BackendUiInteraction.CustomFieldAction.label));
            fieldsOpts.add(new BackendUiInteraction.CustomFieldOpts(name,
                    BackendUiInteraction.CustomFieldAction.label));
            fieldsOpts.add(new BackendUiInteraction.CustomFieldOpts(instruction,
                    BackendUiInteraction.CustomFieldAction.label));
            final CharSequence[] result = new CharSequence[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                final int idx = i;
                fieldsOpts.add(new BackendUiInteraction.CustomFieldOpts(prompt[i],
                        new BackendUiInteraction.CustomTextInputAction() {
                            @Override
                            public void onSubmit(@NonNull final BackendUiInteraction.CustomPrompt prompt,
                                                 @NonNull final CharSequence v) {
                                result[idx] = v;
                            }

                            @Override
                            @NonNull
                            public Type getType() {
                                return echo[idx] ? Type.NORMAL : Type.PASSWORD;
                            }
                        }));
            }
            final boolean r;
            try {
                r = ui.promptFields(fieldsOpts);
            } catch (final InterruptedException e) {
                throw new BackendInterruptedException(e);
            }
            if (!r) {
                erase(result);
                return null;
            }
            return result;
        }

        @Override
        public CharSequence promptPassword(final String id,
                                           final int message, final Object... args) {
            final CharSequence messageStr = buildMessage(message, args);
            final Password savedPassword;
            if (ui instanceof BackendUiPasswordStorage) {
                savedPassword = ((BackendUiPasswordStorage) ui)
                        .getPassword(modulePrefix + id);
            } else {
                savedPassword = null;
            }
            if (silentlyUseSavedPasswords && savedPassword != null) {
                return savedPassword;
            }
            final List<BackendUiInteraction.CustomFieldOpts> extras = new ArrayList<>(2);
            extras.add(new BackendUiInteraction.CustomFieldOpts(
                    sshSessionSt.context.getString(
                            R.string.label_ssh_save_password),
                    new BackendUiInteraction.CustomCheckboxAction() {
                        @Override
                        @NonNull
                        public Boolean onInit() {
                            return savePasswords;
                        }

                        @Override
                        public void onSubmit(@NonNull final BackendUiInteraction.CustomPrompt prompt,
                                             @NonNull final Boolean v) {
                            savePasswords = v;
                        }
                    }));
            final boolean[] useSaved = new boolean[]{false};
            if (savedPassword != null) {
                extras.add(new BackendUiInteraction.CustomFieldOpts(
                        sshSessionSt.context.getString(
                                R.string.label_ssh_use_saved_password),
                        (BackendUiInteraction.CustomButtonAction) prompt -> {
                            useSaved[0] = true;
                            prompt.submit();
                        }
                ));
            }
            final CharSequence r;
            try {
                r = ui.promptPassword(messageStr, extras);
            } catch (final InterruptedException e) {
                throw new BackendInterruptedException(e);
            }
            if (useSaved[0]) {
                erase(r);
                return savedPassword;
            } else {
                if (savedPassword != null)
                    savedPassword.erase();
                return r;
            }
        }

        @Override
        public boolean promptYesNo(final String id,
                                   final int message, final Object... args) {
            final CharSequence messageStr = buildMessage(message, args);
            try {
                return ui.promptYesNo(messageStr);
            } catch (final InterruptedException e) {
                throw new BackendInterruptedException(e);
            }
        }

        @Override
        public void showMessage(final String id, final int message, final Object... args) {
            ui.showMessage(buildMessage(message, args));
        }

        @Override
        public void showMessage(final CharSequence s) {
            ui.showMessage(s);
        }
    };

    // TODO: per-session?
    static {
        if (BuildConfig.DEBUG)
            JSch.setLogger(new Logger() {
                @Override
                public boolean isEnabled(final int level) {
                    return level >= WARN;
                }

                @Override
                public void log(final int level, final String message) {
                    if (level >= ERROR) {
                        Log.e("JSch", message);
                    } else {
                        Log.w("JSch", message);
                    }
                }
            });
    }

    private void onPublicKeyAuth() {
        final SshSessionSt st = sshSessionSt;
        final JSch jsch = st.jsch; // JSch instance is per-session

        if (!jsch.getIdentityRepository().getIdentities().isEmpty())
            return;

        byte[] key = null;
        final Uri uri = st.authKeyUri;
        String reason = null;
        String keyDesc = "";
        try {
            if (uri != null) {
                keyDesc = uri.toString();
                final BlockingSync<Object> result = new BlockingSync<>();
                ContentRequester.requestPersistent(result,
                        ContentRequester.Type.BYTES,
                        CERT_FILE_SIZE_MAX,
                        st.context,
                        uri);
                final Object r = result.get();
                if (r instanceof Throwable) {
                    final String details = r instanceof SecurityException ?
                            st.context.getString(
                                    R.string.msg_desc_permission_revoked) :
                            r instanceof FileNotFoundException ?
                                    st.context.getString(
                                            R.string.msg_desc_file_not_found) :
                                    ((Throwable) r).getLocalizedMessage();
                    reason = st.context.getString(
                            R.string.msg_unable_to_load_associated_key__s,
                            details);
                } else if (r instanceof byte[]) {
                    key = (byte[]) r;
                }
            }
            while (true) {
                if (key != null) {
                    try {
                        jsch.addIdentity(keyDesc, key, null, null);
                        return;
                    } catch (final JSchException e) {
                        reason = st.context.getString(
                                R.string.msg_desc_malformed__s,
                                e.getLocalizedMessage());
                    }
                }
                keyDesc = st.context.getString(R.string.msg_desc_obj_selected_key);
                try {
                    final String message = reason != null ?
                            st.context.getString(
                                    R.string.msg_server_requests_key_identification_s,
                                    reason) :
                            st.context.getString(
                                    R.string.msg_server_requests_key_identification);
                    key = st.ui.promptContent(Html.fromHtml(message),
                            "*/*", CERT_FILE_SIZE_MAX);
                    if (key == null) {
                        return;
                    }
                } catch (final IOException e) {
                    reason = st.context.getString(
                            R.string.msg_unable_to_load_associated_key__s,
                            e.getLocalizedMessage());
                }
            }
        } catch (final InterruptedException e) {
            throw new BackendInterruptedException(e);
        }
    }

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        mOS_set = stream;
    }

    @Override
    @NonNull
    public OutputStream getOutputStream() {
        return mOS_get;
    }

    private OnMessageListener onMessageListener = null;

    @Override
    public void setOnMessageListener(@Nullable final OnMessageListener l) {
        onMessageListener = l;
    }

    private void reportState(@NonNull final StateMessage m) {
        if (onMessageListener != null)
            onMessageListener.onMessage(m);
    }

    @Override
    public boolean isConnected() {
        final Channel ch = channel;
        return (ch != null) && ch.isConnected();
    }

    @Override
    public void connect() {
        if (channel != null)
            return;
        channelExitStatus = null;
        final Channel ch;
        sshSessionSt.refs.getAndIncrement();
        try {
            synchronized (sshSessionSt.lock) {
                if (sshSessionSt.session == null) {
                    final Session s = sshSessionSt.jsch.getSession(sshSessionSt.username,
                            sshSessionSt.hostname, sshSessionSt.port);
                    s.setUserInfo(userInfo);
                    s.setHostKeyRepository(new SshHostKeyRepository(context));
                    s.setOnPublicKeyAuth(this::onPublicKeyAuth);
                    s.setConfig("kex", sshSessionSt.kex);
                    s.setConfig("cipher.s2c", sshSessionSt.cipher_s2c);
                    s.setConfig("cipher.c2s", sshSessionSt.cipher_c2s);
                    s.setConfig("mac.s2c", sshSessionSt.mac_s2c);
                    s.setConfig("mac.c2s", sshSessionSt.mac_c2s);
                    s.setConfig("PubkeyAcceptedAlgorithms",
                            sshSessionSt.pubkey_accepted_algorithms);
                    s.setConfig("enable_server_sig_algs",
                            toJSchBoolOpt(sshSessionSt.enable_server_sig_algs));
                    final String cfgComp = sshSessionSt.preferCompression ?
                            "zlib,none" : "none,zlib";
                    s.setConfig("compression.s2c", cfgComp);
                    s.setConfig("compression.c2s", cfgComp);
                    s.setConfig("StrictHostKeyChecking", "ask");
                    s.setConfig("PreferredAuthentications", sshSessionSt.preferKeyAuth
                            ? "none,publickey,password,keyboard-interactive"
                            : "none,password,publickey,keyboard-interactive");
                    s.setServerAliveInterval(sshSessionSt.keepaliveInterval);
                    s.setServerAliveCountMax(10);
                    s.setX11Host(sshSessionSt.x11Host);
                    s.setX11Port(6000 + sshSessionSt.x11Port);
                    s.connect(5000);
                    sshSessionSt.session = s;
                    sshSessionSt.key = obtainSshSessionKey();
                    sshSessionSts.put(sshSessionSt.key, sshSessionSt);
                    for (final PortMapping pm : sshSessionSt.localPortMappings)
                        s.setPortForwardingL(pm.srcPort, pm.host, pm.dstPort);
                    for (final PortMapping pm : sshSessionSt.remotePortMappings)
                        s.setPortForwardingR(pm.srcPort, pm.host, pm.dstPort);
                }
            }
            if (execute.isEmpty()) {
                ch = sshSessionSt.session.openChannel("shell");
                ((ChannelShell) ch).setPty(true);
                ((ChannelShell) ch).setPtyType(terminalString);
            } else {
                ch = sshSessionSt.session.openChannel("exec");
                ((ChannelExec) ch).setPty(true);
                ((ChannelExec) ch).setPtyType(terminalString);
                ((ChannelExec) ch).setCommand(execute);
                ((ChannelExec) ch).setErrStream(mOS_set);
            }
            ch.setXForwarding(x11);
            ch.setOnDisconnect(mOnDisconnect);
            ch.setOutputStream(mOS_set);
            mOS_get_orig = ch.getOutputStream();
            ch.connect(3000);
        } catch (final JSchException | JSchErrorException e) {
            sshSessionSt.refs.decrementAndGet();
            disconnect();
            throw new BackendException(e.getLocalizedMessage());
        } catch (final SshHostKeyRepository.Exception e) {
            sshSessionSt.refs.decrementAndGet();
            disconnect();
            throw new BackendException(e);
        } catch (final IOException e) {
            sshSessionSt.refs.decrementAndGet();
            disconnect();
            throw new BackendException(e);
        } catch (final NoClassDefFoundError e) {
            sshSessionSt.refs.decrementAndGet();
            disconnect();
            throw new BackendException(context.getString(R.string.msg_feature_class_not_found,
                    e.getLocalizedMessage()));
        }
        channel = ch;
        if (isAcquireWakeLockOnConnect())
            acquireWakeLock();
    }

    @Override
    public void disconnect() {
        try {
            final Channel ch = channel;
            if (ch != null) {
                ch.disconnect();
                channel = null;
                sshSessionSt.refs.decrementAndGet();
            }
            synchronized (sshSessionSt.lock) {
                if (sshSessionSt.session != null && sshSessionSt.refs.get() <= 0) {
                    sshSessionSt.session.disconnect();
                    sshSessionSt.session = null;
                    sshSessionSts.remove(sshSessionSt.key);
                }
            }
        } finally {
            if (isReleaseWakeLockOnDisconnect())
                releaseWakeLock();
        }
    }

    @Override
    public void resize(final int col, final int row, final int wp, final int hp) {
        final Channel ch = channel;
        if (ch instanceof ChannelExec)
            ((ChannelExec) channel).setPtySize(col, row, wp, hp);
        else if (ch instanceof ChannelShell)
            ((ChannelShell) channel).setPtySize(col, row, wp, hp);
    }

    @Override
    @NonNull
    public String getConnDesc() {
        return String.format(Locale.getDefault(), "ssh://%s@%s:%d",
                sshSessionSt.username, sshSessionSt.hostname, sshSessionSt.port);
    }

    @Override
    @Nullable
    public DisconnectionReason getDisconnectionReason() {
        final Integer es = channelExitStatus;
        return es != null ? new ProcessExitDisconnectionReason(es) : null;
    }

    @Keep
    @ExportedUIMethod(titleRes = R.string.action_new_shell_in_this_ssh_session,
            longTitleRes = R.string.desc_new_shell_in_this_ssh_session,
            order = 1)
    @Nullable
    public SshModule startShellChannel() {
        final SshModule be = new SshModule(this);
        be.execute = "";
        return be;
    }

    @Keep
    @ExportedUIMethod(titleRes = R.string.action_manage_portFw_in_this_ssh_session,
            longTitleRes = R.string.desc_manage_portFw_in_this_ssh_session,
            order = 2)
    @Nullable
    public Intent managePortForwarding() {
        synchronized (sshSessionSt.lock) {
            if (sshSessionSt.session == null)
                return null;
            return new Intent(context, SshModulePortFwActivity.class)
                    .putExtra(SshModulePortFwActivity.IFK_SSH_SESS_KEY, sshSessionSt.key);
        }
    }

    /**
     * SSH "signal" <code>SSH_MSG_CHANNEL_REQUEST</code>.
     * https://www.rfc-editor.org/rfc/rfc4254.html#section-6.9
     */
    @Keep
    @ExportedUIMethodOnThread(before = true)
    @ExportedUIMethod(titleRes = R.string.action_send_signal,
            longTitleRes = R.string.action_send_signal_to_remote_process, order = 3)
    public void sendSignal(@ExportedUIMethodStrEnum(values = {
            "ABRT", "ALRM", "FPE", "HUP", "ILL", "INT", "KILL", "PIPE",
            "QUIT", "SEGV", "TERM", "USR1", "USR2"
    }) final String signal) {
        final Channel ch = channel;
        if (ch != null) {
            try {
                ch.sendSignal(signal);
            } catch (final JSchException | JSchErrorException e) {
                throw new BackendException("SSH: " + e.getMessage(), e);
            } catch (final Exception e) {
                throw new BackendException(e);
            }
        }
    }
}
