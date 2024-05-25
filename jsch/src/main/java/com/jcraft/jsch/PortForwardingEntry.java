package com.jcraft.jsch;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PortForwardingEntry {
    /**
     * Alas! There is no {@link java.net.InetSocketAddress#getHostString()} before Android API 19...
     */
    public static final class HostAndPort {
        @NotNull
        public final String host;
        public final int port;

        HostAndPort(@NotNull final String host, final int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final HostAndPort that = (HostAndPort) o;
            return port == that.port && host.equals(that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
    }

    /**
     * Where to listen.
     */
    @NotNull
    public final HostAndPort src;
    /**
     * Where to connect.
     */
    @NotNull
    public final HostAndPort dst;

    PortForwardingEntry(final String srcHost, final int srcPort,
                        final String dstHost, final int dstPort) {
        this.src = new HostAndPort(srcHost, srcPort);
        this.dst = new HostAndPort(dstHost, dstPort);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final PortForwardingEntry that = (PortForwardingEntry) o;
        return src.equals(that.src) && dst.equals(that.dst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }
}
