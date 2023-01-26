package green_green_avk.anotherterm.utils;

import android.util.Base64;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;

public final class SshHostKey extends HostKey {
    public SshHostKey(final String host, final String type, final String key,
                      final String comment) throws JSchException {
        super(host, name2type(type), Base64.decode(key, Base64.DEFAULT), comment);
    }

    public byte[] getRawKey() {
        return key;
    }
}
