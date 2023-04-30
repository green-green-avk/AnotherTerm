package com.felhr.utils;

import android.hardware.usb.UsbRequest;
import android.os.Build;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class SafeUsbRequest extends UsbRequest {
    @Override
    public boolean queue(final ByteBuffer buffer, final int length) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                final Field usbRequestBuffer = UsbRequest.class.getDeclaredField("mBuffer");
                final Field usbRequestLength = UsbRequest.class.getDeclaredField("mLength");
                usbRequestBuffer.setAccessible(true);
                usbRequestLength.setAccessible(true);
                usbRequestBuffer.set(this, buffer);
                usbRequestLength.set(this, length);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.queue(buffer, length);
    }
}
