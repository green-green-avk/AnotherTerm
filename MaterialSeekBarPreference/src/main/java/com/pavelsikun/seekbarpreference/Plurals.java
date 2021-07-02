package com.pavelsikun.seekbarpreference;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.IllegalFormatException;
import java.util.regex.Pattern;

final class Plurals {
    private static final Pattern pFmtStr = Pattern.compile("(?:^|[^%])(?:%%)*%(?:[^%]|$)");

    @AnyRes
    private int resId = 0;
    private String str = null;
    private boolean isFmt = false;
    private boolean cached = false;

    private void refresh(@NonNull final Resources rr) {
        if (!cached) {
            if (resId != 0 && !"plurals".equalsIgnoreCase(rr.getResourceTypeName(resId)))
                set(rr.getString(resId));
            cached = true;
        }
    }

    @Nullable
    String apply(@NonNull final Context ctx, final int value) {
        final Resources rr = ctx.getResources();
        refresh(rr);
        if (str != null) {
            return isFmt ? String.format(str, value) : str;
        }
        if (resId != 0) {
            final String s = rr.getQuantityString(resId, value);
            if (!pFmtStr.matcher(s).find()) {
                isFmt = false; // tricky
                return s.replace("%%", "%");
            }
            try {
                isFmt = true; // tricky
                return String.format(s, value);
            } catch (final IllegalFormatException e) {
                isFmt = false; // tricky
                return s.replace("%%", "%");
            }
        }
        return null;
    }

    boolean isFormatted() { // TODO: refactor
        return isFmt;
    }

    @Nullable
    String get(@NonNull final Context ctx, final int value) {
        final Resources rr = ctx.getResources();
        refresh(rr);
        if (str != null) return str;
        if (resId != 0) return rr.getQuantityString(resId, value);
        return null;
    }

    boolean isPlurals(@NonNull final Context ctx) {
        final Resources rr = ctx.getResources();
        refresh(rr);
        return resId != 0 && str == null;
    }

    void set(@AnyRes final int resId) {
        cached = false;
        str = null;
        this.resId = resId;
    }

    void set(@Nullable final String str) {
        resId = 0;
        this.str = str;
        if (str == null) return;
        if (!pFmtStr.matcher(str).find()) {
            this.str = str.replace("%%", "%"); // Or be stricter?
            isFmt = false; // tricky
        } else
            try {
                String.format(str, 0); // Should be better than regexp
                isFmt = true;
            } catch (final IllegalFormatException e) {
                this.str = str.replace("%%", "%"); // Or be stricter?
                isFmt = false;
            }
        cached = true;
    }
}
