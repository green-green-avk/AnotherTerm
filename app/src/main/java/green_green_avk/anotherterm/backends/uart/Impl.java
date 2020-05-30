package green_green_avk.anotherterm.backends.uart;

import androidx.annotation.NonNull;

import java.io.OutputStream;

abstract class Impl {
    @NonNull
    protected final UartModule base;

    Impl(@NonNull final UartModule base) {
        this.base = base;
    }

    @NonNull
    abstract OutputStream getOutputStream();

    abstract boolean isConnected();

    abstract void connect() throws UartModule.AdapterNotFoundException;

    abstract void disconnect();

    @NonNull
    abstract String getSubDesc();
}
