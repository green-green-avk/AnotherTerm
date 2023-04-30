package com.felhr.deviceids;

import static com.felhr.deviceids.Helpers.createDevice;
import static com.felhr.deviceids.Helpers.createTable;

public final class CP2130Ids {
    private CP2130Ids() {
    }

    private static final long[] cp2130Devices = createTable(
            createDevice(0x10C4, 0x87a0)
    );

    public static boolean isDeviceSupported(final int vendorId, final int productId) {
        return Helpers.exists(cp2130Devices, vendorId, productId);
    }
}
