package green_green_avk.wayland.os;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface WlSocket {
    @NonNull
    InputStream getInputStream() throws IOException;

    @NonNull
    OutputStream getOutputStream() throws IOException;

    @Nullable
    FileDescriptor[] getAncillaryFileDescriptors() throws IOException;

    void setFileDescriptorsForSend(@Nullable FileDescriptor[] fds);
}
