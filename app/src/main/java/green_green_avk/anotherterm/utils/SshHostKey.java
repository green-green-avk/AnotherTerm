package green_green_avk.anotherterm.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;

public final class SshHostKey extends HostKey {
    public SshHostKey(@NonNull final String host, @NonNull final String type,
                      @NonNull final byte[] key,
                      @Nullable final String comment) throws JSchException {
        super(host, name2type(type), key, comment);
    }
}
