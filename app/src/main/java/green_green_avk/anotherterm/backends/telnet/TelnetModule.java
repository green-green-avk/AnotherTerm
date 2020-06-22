package green_green_avk.anotherterm.backends.telnet;

import android.net.Uri;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.telnetclient.EchoTelnetOptionHandler;
import green_green_avk.telnetclient.SuppressGATelnetOptionHandler;
import green_green_avk.telnetclient.TelnetClient;
import green_green_avk.telnetclient.TelnetClientException;
import green_green_avk.telnetclient.TerminalTypeTelnetOptionHandler;
import green_green_avk.telnetclient.WindowSizeTelnetOptionHandler;

public final class TelnetModule extends BackendModule {

    @Keep
    public static final Meta meta = new Meta(TelnetModule.class, "telnet") {
        @Override
        @NonNull
        public Map<String, ?> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque()) throw new ParametersUriParseException();
            final Map<String, Object> params = new HashMap<>();
            for (final String k : uri.getQueryParameterNames()) {
                // TODO: '+' decoding issue before Jelly Bean
                params.put(k, uri.getQueryParameter(k));
            }
            final String hostname = uri.getHost();
            if (hostname != null) {
                params.put("hostname", hostname);
                final String username = uri.getUserInfo();
                if (username != null) params.put("username", username);
                final int port = uri.getPort();
                if (port >= 0) params.put("port", port);
            }
            return params;
        }

        @Override
        @NonNull
        public Uri toUri(@NonNull final Map<String, ?> params) {
            final Object username = params.get("username");
            final String auth;
            if (username != null)
                auth = String.format(Locale.ROOT, "%s@%s:%s",
                        URLEncoder.encode(username.toString()),
                        params.get("hostname").toString(),
                        params.get("port").toString());
            else
                auth = String.format(Locale.ROOT, "%s:%s",
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
    private int port = 23;
    private String username;
    private String terminalString = "xterm";
    private int keepaliveInterval = 0;

    @Override
    public void setParameters(@NonNull final Map<String, ?> params) {
        final ParametersWrapper pp = new ParametersWrapper(params);
        hostname = pp.getString("hostname", null);
        if (hostname == null) throw new BackendException("`hostname' is not defined");

        port = pp.getInt("port", port);

        username = pp.getString("username", null);

        terminalString = pp.getString("terminal_string", terminalString);

        keepaliveInterval = pp.getInt("keepalive_interval", keepaliveInterval);
    }

    private final OutputStream mOS_get = new OutputStream() {
        @Override
        public void write(final int b) {
            if (tc == null) return;
            try {
                tc.send((byte) b);
            } catch (final TelnetClientException e) {
                throw new BackendException(e);
            }
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            if (tc == null) return;
            try {
                tc.send(b, off, off + len);
            } catch (final TelnetClientException e) {
                throw new BackendException(e);
            }
        }

        @Override
        public void write(final byte[] b) {
            if (tc == null) return;
            try {
                tc.send(b);
            } catch (final TelnetClientException e) {
                throw new BackendException(e);
            }
        }
    };

    @Override
    public void setOutputStream(@NonNull final OutputStream stream) {
        tc.setOutputStream(stream);
    }

    @NonNull
    @Override
    public OutputStream getOutputStream() {
        return mOS_get;
    }

    @Override
    public void setOnMessageListener(@Nullable final OnMessageListener l) {
        if (l == null) tc.setOnErrorListener(null);
        else tc.setOnErrorListener(new TelnetClient.OnErrorListener() {
            @Override
            public void onError(final Throwable e) {
                l.onMessage(e);
            }
        });
    }

    private final TelnetClient tc = new TelnetClient();
    private final TerminalTypeTelnetOptionHandler ttoh = new TerminalTypeTelnetOptionHandler();
    private final WindowSizeTelnetOptionHandler wsoh = new WindowSizeTelnetOptionHandler();

    {
        tc.setOptionHandler(new EchoTelnetOptionHandler());
        tc.setOptionHandler(new SuppressGATelnetOptionHandler());
        tc.setOptionHandler(ttoh);
        tc.setOptionHandler(wsoh);
    }

    @Override
    public boolean isConnected() {
        return tc.isConnected();
    }

    @Override
    public void connect() {
        try {
            ttoh.update(terminalString);
            tc.setKeepAliveInterval(keepaliveInterval);
            tc.connect(hostname, port);
        } catch (final TelnetClientException e) {
            throw new BackendException(e);
        }
        if (isAcquireWakeLockOnConnect()) acquireWakeLock();
    }

    @Override
    public void disconnect() {
        try {
            tc.disconnect();
        } catch (final TelnetClientException e) {
            throw new BackendException(e);
        } finally {
            if (isReleaseWakeLockOnDisconnect()) releaseWakeLock();
        }
    }

    @Override
    public void resize(final int col, final int row, final int wp, final int hp) {
        try {
            wsoh.update(col, row);
        } catch (final TelnetClientException e) {
            throw new BackendException(e);
        }
    }

    @NonNull
    @Override
    public String getConnDesc() {
        if (StringUtils.isEmpty(username))
            return String.format(Locale.getDefault(), "telnet://%s:%d", hostname, port);
        return String.format(Locale.getDefault(), "telnet://%s@%s:%d", username, hostname, port);
    }
}
