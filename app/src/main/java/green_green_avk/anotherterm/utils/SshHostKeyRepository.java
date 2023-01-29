package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchErrorException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SshHostKeyRepository implements HostKeyRepository {
    public static final class Exception extends RuntimeException {
        public Exception(final Throwable e) {
            super(e);
        }

        public Exception(final String message) {
            super(message);
        }
    }

    private static boolean isMetaName(@NonNull final String name) {
        return !name.isEmpty() && name.charAt(0) == '$';
    }

    private static final String versionName = "$Version";
    private static final Object migrationLock = new Object();

    @NonNull
    private final SharedPreferences sp;

    private void checkAndMigrate() {
        synchronized (migrationLock) {
            final int version;
            try {
                version = sp.getInt(versionName, 1);
            } catch (final ClassCastException e) {
                return;
            }
            if (version == 1) {
                final Pattern oldHostPortRe = Pattern.compile("^\\[(.+)]:(\\d+)$");
                final SharedPreferences.Editor editor = sp.edit();
                for (final Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
                    try {
                        final Matcher m = oldHostPortRe.matcher(entry.getKey());
                        if (m.matches()) {
                            editor.remove(entry.getKey());
                            editor.putStringSet(m.group(1) + ":" + m.group(2),
                                    (Set<String>) entry.getValue());
                        } else if (!isMetaName(entry.getKey()) && entry.getKey().indexOf(':') < 0) {
                            editor.remove(entry.getKey());
                            editor.putStringSet(entry.getKey() + ":22",
                                    (Set<String>) entry.getValue());
                        }
                    } catch (final RuntimeException ignored) {
                    }
                }
                editor.putInt(versionName, 2);
                editor.apply();
            }
        }
    }

    public SshHostKeyRepository(@NonNull final Context ctx) {
        sp = ctx.getSharedPreferences(ctx.getPackageName() + "_hostkeys",
                Context.MODE_PRIVATE);
        checkAndMigrate();
    }

    @NonNull
    private static String serializeKey(@NonNull final HostKey hostKey) {
        return hostKey.getType() + ":" +
                Base64.encodeToString(hostKey.getKey(), Base64.NO_WRAP)
                + ":" + Misc.requireNonNullElse(hostKey.getComment(), "");
    }

    @NonNull
    private static SshHostKey deserializeKey(@NonNull final String host,
                                             @NonNull final String serialized) {
        final String[] kss = serialized.split(":", 3);
        if (kss.length != 3) {
            throw new SshHostKeyRepository.Exception("Key record is broken");
        }
        try {
            return new SshHostKey(host, kss[0],
                    Base64.decode(kss[1], Base64.DEFAULT),
                    kss[2]);
        } catch (final JSchException | JSchErrorException e) {
            throw new SshHostKeyRepository.Exception(e);
        }
    }

    @NonNull
    private Set<String> getKeys(@NonNull final String name) {
        try {
            return sp.getStringSet(name, Collections.emptySet());
        } catch (final ClassCastException e) {
            final SharedPreferences.Editor editor = sp.edit();
            editor.remove(name);
            editor.apply();
            return Collections.emptySet();
        }
    }

    @Override
    public void add(@NonNull final HostKey hostKey, @NonNull final UserInfo userInfo) {
        final Set<String> keys = new HashSet<>(getKeys(hostKey.getHost()));
        final SharedPreferences.Editor ed = sp.edit();
        keys.add(serializeKey(hostKey));
        ed.putStringSet(hostKey.getHost(), keys);
        ed.apply();
    }

    @Override
    public int check(@NonNull final String host, @NonNull final byte[] bytes) {
        final Set<String> keys = getKeys(host);
        if (keys.isEmpty()) {
            return NOT_INCLUDED;
        }
        for (final String ent : keys) {
            final SshHostKey k = deserializeKey(host, ent);
            if (Arrays.equals(k.getKey(), bytes)) {
                return OK;
            }
        }
        return CHANGED;
    }

    @NonNull
    public Set<SshHostKey> getHostKeySet(@NonNull final String host,
                                         @Nullable final String keyType) {
        final Set<String> keys = getKeys(host);
        final Set<SshHostKey> r = new HashSet<>();
        for (final String ent : keys) {
            final SshHostKey k = deserializeKey(host, ent);
            if (keyType == null || keyType.equals(k.getType())) {
                r.add(k);
            }
        }
        return r;
    }

    @NonNull
    public Set<SshHostKey> getHostKeySet() {
        final Set<SshHostKey> r = new HashSet<>();
        for (final String host : sp.getAll().keySet()) {
            if (!isMetaName(host))
                r.addAll(getHostKeySet(host, null));
        }
        return r;
    }

    @Override
    @NonNull
    public HostKey[] getHostKey() {
        return getHostKeySet().toArray(new HostKey[0]);
    }

    @Override
    @NonNull
    public HostKey[] getHostKey(@NonNull final String host, @Nullable final String keyType) {
        return getHostKeySet(host, keyType).toArray(new HostKey[0]);
    }

    @Override
    @NonNull
    public String getKnownHostsRepositoryID() {
        return "Main Hosts Repository";
    }

    @Override
    public void remove(@NonNull final String host, @Nullable final String keyType) {
        final Set<String> keys = new HashSet<>(getKeys(host));
        final SharedPreferences.Editor ed = sp.edit();
        for (final SshHostKey k : getHostKeySet(host, keyType)) {
            keys.remove(serializeKey(k));
            ed.putStringSet(host, keys);
        }
        ed.apply();
    }

    @Override
    public void remove(@NonNull final String host, @Nullable final String keyType,
                       @NonNull final byte[] bytes) {
        final Set<String> keys = new HashSet<>(getKeys(host));
        final SharedPreferences.Editor ed = sp.edit();
        for (final SshHostKey k : getHostKeySet(host, keyType)) {
            if (Arrays.equals(k.getKey(), bytes)) {
                keys.remove(serializeKey(k));
                ed.putStringSet(host, keys);
                ed.apply();
                return;
            }
        }
    }

    public void remove(@NonNull final SshHostKey key) {
        remove(key.getHost(), key.getType(), key.getKey());
    }

    @NonNull
    public Set<String> getUsages(@NonNull final byte[] bytes) {
        final Set<String> r = new HashSet<>();
        for (final String host : sp.getAll().keySet()) {
            if (!isMetaName(host) && check(host, bytes) == OK)
                r.add(host);
        }
        return r;
    }
}
