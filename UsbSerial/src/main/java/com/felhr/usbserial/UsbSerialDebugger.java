package com.felhr.usbserial;

import android.util.Log;

import com.felhr.utils.HexData;

public final class UsbSerialDebugger {
    private static final String CLASS_ID = UsbSerialDebugger.class.getSimpleName();

    private UsbSerialDebugger() {
    }

    public static void printLogGet(final byte[] src, final boolean verbose) {
        if (!verbose) {
            Log.i(CLASS_ID, "Data obtained from write buffer: " + new String(src));
        } else {
            Log.i(CLASS_ID, "Data obtained from write buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data from write buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes obtained from write buffer: " + src.length);
        }
    }

    public static void printLogPut(final byte[] src, final boolean verbose) {
        if (!verbose) {
            Log.i(CLASS_ID, "Data obtained pushed to write buffer: " + new String(src));
        } else {
            Log.i(CLASS_ID, "Data obtained pushed to write buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data pushed to write buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes pushed from write buffer: " + src.length);
        }
    }

    public static void printReadLogGet(final byte[] src, final boolean verbose) {
        if (!verbose) {
            Log.i(CLASS_ID, "Data obtained from Read buffer: " + new String(src));
        } else {
            Log.i(CLASS_ID, "Data obtained from Read buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data from Read buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes obtained from Read buffer: " + src.length);
        }
    }

    public static void printReadLogPut(final byte[] src, final boolean verbose) {
        if (!verbose) {
            Log.i(CLASS_ID, "Data obtained pushed to read buffer: " + new String(src));
        } else {
            Log.i(CLASS_ID, "Data obtained pushed to read buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data pushed to read buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes pushed from read buffer: " + src.length);
        }
    }
}
