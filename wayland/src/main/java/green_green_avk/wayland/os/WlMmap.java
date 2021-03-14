package green_green_avk.wayland.os;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface WlMmap {
    @NonNull
    ByteBuffer mmap(@NonNull FileDescriptor fd, int size) throws IOException;

    void munmap(@NonNull ByteBuffer mem);

    void close(@NonNull FileDescriptor fd);
}
