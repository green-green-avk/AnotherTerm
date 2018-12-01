package green_green_avk.anotherterm.utils;

import android.util.Base64;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;

import java.util.Arrays;

public final class SshHostKey extends HostKey {
    public SshHostKey(final String host, final String type, final String key,
                      final String comment) throws JSchException {
        super(host, name2type(type), Base64.decode(key, Base64.DEFAULT), comment);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof byte[] && Arrays.equals(key, (byte[]) obj);
    }

    public byte[] getRawKey() {
        return key;
    }
}
