package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SshHostKeyRepository implements HostKeyRepository {

    public static final class Exception extends RuntimeException {
        public Exception(final Throwable e) {
            super(e);
        }

        public Exception(final String message) {
            super(message);
        }
    }

    private final SharedPreferences sp;

    public SshHostKeyRepository(@NonNull final Context ctx) {
        sp = ctx.getSharedPreferences(ctx.getPackageName() + "_hostkeys", Context.MODE_PRIVATE);
    }

    private static String serializeKey(@NonNull final HostKey hostKey) {
        return hostKey.getType() + ":" + hostKey.getKey() + ":" + hostKey.getComment();
    }

    @Override
    public void add(@NonNull final HostKey hostKey, @NonNull final UserInfo userInfo) {
        final Set<String> keys = sp.getStringSet(hostKey.getHost(), new HashSet<String>());
        final SharedPreferences.Editor ed = sp.edit();
        keys.add(serializeKey(hostKey));
        ed.putStringSet(hostKey.getHost(), keys);
        ed.apply();
    }

    private static SshHostKey parseKey(@NonNull final String host, @NonNull final String unparsed) {
        final String kss[] = unparsed.split(":", 3);
        try {
            return new SshHostKey(host, kss[0], kss[1], kss[2]);
        } catch (final JSchException e) {
            throw new SshHostKeyRepository.Exception(e);
        }
    }

    @Override
    public int check(@NonNull final String s, @NonNull final byte[] bytes) {
        final Set<String> keys = sp.getStringSet(s, Collections.<String>emptySet());
        if (keys.size() == 0) return NOT_INCLUDED;
        for (String ent : keys) {
            SshHostKey k = parseKey(s, ent);
            if (Arrays.equals(k.getRawKey(), bytes)) {
                return OK;
            }
        }
        return CHANGED;
    }

    public Set<SshHostKey> getHostKeySet(@NonNull final String s, @Nullable final String s1) {
        final Set<String> keys = sp.getStringSet(s, Collections.<String>emptySet());
        final Set<SshHostKey> r = new HashSet<>();
        for (String ent : keys) {
            SshHostKey k = parseKey(s, ent);
            if (s1 == null || s1.equals(k.getType()))
                r.add(k);
        }
        return r;
    }

    public Set<SshHostKey> getHostKeySet() {
        final Set<SshHostKey> r = new HashSet<>();
        for (String host : sp.getAll().keySet()) {
            r.addAll(getHostKeySet(host, null));
        }
        return r;
    }

    @Override
    public HostKey[] getHostKey() {
        return getHostKeySet().toArray(new HostKey[0]);
    }

    @Override
    public HostKey[] getHostKey(@NonNull final String s, @Nullable final String s1) {
        return getHostKeySet(s, s1).toArray(new HostKey[0]);
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "Main Hosts Repository";
    }

    @Override
    public void remove(@NonNull final String s, @Nullable final String s1) {
        final Set<String> keys = sp.getStringSet(s, Collections.<String>emptySet());
        final SharedPreferences.Editor ed = sp.edit();
        for (SshHostKey k : getHostKeySet(s, s1)) {
            keys.remove(serializeKey(k));
            ed.putStringSet(s, keys);
        }
        ed.apply();
    }

    @Override
    public void remove(@NonNull final String s, @Nullable final String s1, @NonNull final byte[] bytes) {
        final Set<String> keys = sp.getStringSet(s, Collections.<String>emptySet());
        final SharedPreferences.Editor ed = sp.edit();
        for (SshHostKey k : getHostKeySet(s, s1)) {
            if (Arrays.equals(k.getRawKey(), bytes)) {
                keys.remove(serializeKey(k));
                ed.putStringSet(s, keys);
                ed.apply();
                return;
            }
        }
    }

    public void remove(@NonNull final SshHostKey key) {
        remove(key.getHost(), key.getType(), key.getRawKey());
    }
}
