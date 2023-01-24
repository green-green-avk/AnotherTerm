/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002-2018 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class Util {
    private Util() {
    }

    static final Charset UTF8 = Charset.forName("UTF8");

    private static final byte[] b64 = Util.str2byte("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=");

    private static byte val(final byte foo) {
        if (foo == '=') return 0;
        for (int j = 0; j < b64.length; j++) {
            if (foo == b64[j]) return (byte) j;
        }
        return 0;
    }

    static byte[] fromBase64(final byte[] buf, final int start, final int length)
            throws JSchException {
        try {
            final byte[] foo = new byte[length];
            int j = 0;
            for (int i = start; i < start + length; i += 4) {
                foo[j] = (byte) ((val(buf[i]) << 2) | ((val(buf[i + 1]) & 0x30) >>> 4));
                if (buf[i + 2] == (byte) '=') {
                    j++;
                    break;
                }
                foo[j + 1] = (byte) (((val(buf[i + 1]) & 0x0f) << 4) | ((val(buf[i + 2]) & 0x3c) >>> 2));
                if (buf[i + 3] == (byte) '=') {
                    j += 2;
                    break;
                }
                foo[j + 2] = (byte) (((val(buf[i + 2]) & 0x03) << 6) | (val(buf[i + 3]) & 0x3f));
                j += 3;
            }
            final byte[] bar = new byte[j];
            System.arraycopy(foo, 0, bar, 0, j);
            return bar;
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new JSchException("fromBase64: invalid base64 data", e);
        }
    }

    static byte[] toBase64(final byte[] buf, final int start, final int length,
                           final boolean include_pad) {

        final byte[] tmp = new byte[length * 2];
        int i, j, k;

        int foo = (length / 3) * 3 + start;
        i = 0;
        for (j = start; j < foo; j += 3) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
            tmp[i++] = b64[k];
            k = (buf[j + 1] & 0x0f) << 2 | (buf[j + 2] >>> 6) & 0x03;
            tmp[i++] = b64[k];
            k = buf[j + 2] & 0x3f;
            tmp[i++] = b64[k];
        }

        foo = (start + length) - foo;
        if (foo == 1) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = ((buf[j] & 0x03) << 4) & 0x3f;
            tmp[i++] = b64[k];
            if (include_pad) {
                tmp[i++] = (byte) '=';
                tmp[i++] = (byte) '=';
            }
        } else if (foo == 2) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
            tmp[i++] = b64[k];
            k = ((buf[j + 1] & 0x0f) << 2) & 0x3f;
            tmp[i++] = b64[k];
            if (include_pad) {
                tmp[i++] = (byte) '=';
            }
        }
        final byte[] bar = new byte[i];
        System.arraycopy(tmp, 0, bar, 0, i);
        return bar;

//    return sun.misc.BASE64Encoder().encode(buf);
    }

    static String[] split(final String foo, final String split) {
        if (foo == null)
            return null;
        return foo.split(split, -1);
    }

    static List<String> splitIntoList(final String v, final String delimiter) {
        return new ArrayList<>(Arrays.asList(v.split(delimiter)));
    }

    static boolean glob(final byte[] pattern, final byte[] name) {
        return glob0(pattern, 0, name, 0);
    }

    private static boolean glob0(final byte[] pattern, final int pattern_index,
                                 final byte[] name, final int name_index) {
        if (name.length > 0 && name[0] == '.') {
            if (pattern.length > 0 && pattern[0] == '.') {
                if (pattern.length == 2 && pattern[1] == '*')
                    return true;
                return glob(pattern, pattern_index + 1, name, name_index + 1);
            }
            return false;
        }
        return glob(pattern, pattern_index, name, name_index);
    }

    private static boolean glob(final byte[] pattern, final int pattern_index,
                                final byte[] name, final int name_index) {
        //System.err.println("glob: "+new String(pattern)+", "+pattern_index+" "+new String(name)+", "+name_index);

        final int patternlen = pattern.length;
        if (patternlen == 0)
            return false;

        final int namelen = name.length;
        int i = pattern_index;
        int j = name_index;

        while (i < patternlen && j < namelen) {
            if (pattern[i] == '\\') {
                if (i + 1 == patternlen)
                    return false;
                i++;
                if (pattern[i] != name[j])
                    return false;
                i += skipUTF8Char(pattern[i]);
                j += skipUTF8Char(name[j]);
                continue;
            }

            if (pattern[i] == '*') {
                while (i < patternlen) {
                    if (pattern[i] == '*') {
                        i++;
                        continue;
                    }
                    break;
                }
                if (patternlen == i)
                    return true;

                byte foo = pattern[i];
                if (foo == '?') {
                    while (j < namelen) {
                        if (glob(pattern, i, name, j)) {
                            return true;
                        }
                        j += skipUTF8Char(name[j]);
                    }
                    return false;
                } else if (foo == '\\') {
                    if (i + 1 == patternlen)
                        return false;
                    i++;
                    foo = pattern[i];
                    while (j < namelen) {
                        if (foo == name[j]) {
                            if (glob(pattern, i + skipUTF8Char(foo),
                                    name, j + skipUTF8Char(name[j]))) {
                                return true;
                            }
                        }
                        j += skipUTF8Char(name[j]);
                    }
                    return false;
                }

                while (j < namelen) {
                    if (foo == name[j]) {
                        if (glob(pattern, i, name, j)) {
                            return true;
                        }
                    }
                    j += skipUTF8Char(name[j]);
                }
                return false;
            }

            if (pattern[i] == '?') {
                i++;
                j += skipUTF8Char(name[j]);
                continue;
            }

            if (pattern[i] != name[j])
                return false;

            i += skipUTF8Char(pattern[i]);
            j += skipUTF8Char(name[j]);

            if (!(j < namelen)) {         // name is end
                if (!(i < patternlen)) {    // pattern is end
                    return true;
                }
                if (pattern[i] == '*') {
                    break;
                }
            }
            continue;
        }

        if (i == patternlen && j == namelen)
            return true;

        if (!(j < namelen) &&  // name is end
                pattern[i] == '*') {
            boolean ok = true;
            while (i < patternlen) {
                if (pattern[i++] != '*') {
                    ok = false;
                    break;
                }
            }
            return ok;
        }

        return false;
    }

    static String quote(final String path) {
        final byte[] _path = str2byte(path);
        int count = 0;
        for (final byte b : _path) {
            if (b == '\\' || b == '?' || b == '*')
                count++;
        }
        if (count == 0)
            return path;
        final byte[] _path2 = new byte[_path.length + count];
        int j = 0;
        for (final byte b : _path) {
            if (b == '\\' || b == '?' || b == '*') {
                _path2[j++] = '\\';
            }
            _path2[j++] = b;
        }
        return byte2str(_path2);
    }

    static String unquote(final String path) {
        final byte[] foo = str2byte(path);
        final byte[] bar = unquote(foo);
        if (foo.length == bar.length)
            return path;
        return byte2str(bar);
    }

    static byte[] unquote(final byte[] path) {
        int pathlen = path.length;
        int i = 0;
        while (i < pathlen) {
            if (path[i] == '\\') {
                if (i + 1 == pathlen)
                    break;
                System.arraycopy(path, i + 1, path, i, path.length - (i + 1));
                pathlen--;
                i++;
                continue;
            }
            i++;
        }
        if (pathlen == path.length)
            return path;
        final byte[] foo = new byte[pathlen];
        System.arraycopy(path, 0, foo, 0, pathlen);
        return foo;
    }

    private static final String[] chars = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"
    };

    static String getFingerPrint(final HASH hash, final byte[] data,
                                 final boolean include_prefix, final boolean force_hex)
            throws JSchException {
        try {
            hash.init();
            hash.update(data, 0, data.length);
            final byte[] foo = hash.digest();
            final StringBuilder sb = new StringBuilder();
            if (include_prefix) {
                sb.append(hash.name());
                sb.append(":");
            }
            if (force_hex || hash.name().equals("MD5")) {
                for (int i = 0; i < foo.length; i++) {
                    final int bar = foo[i] & 0xff;
                    sb.append(chars[(bar >>> 4) & 0xf]);
                    sb.append(chars[(bar) & 0xf]);
                    if (i + 1 < foo.length)
                        sb.append(":");
                }
            } else {
                final byte[] b64str = toBase64(foo, 0, foo.length, false);
                sb.append(byte2str(b64str, 0, b64str.length));
            }
            return sb.toString();
        } catch (final Exception e) {
            throw new JSchException("Error getting fingerprint", e);
        }
    }

    static boolean array_equals(final byte[] foo, final byte[] bar) {
        return Arrays.equals(foo, bar);
    }

    static Socket createSocket(final String host, final int port, final int timeout)
            throws JSchException {
        if (timeout <= 0) {
            try {
                return new Socket(host, port);
            } catch (final Exception e) {
                throw new JSchException(e.toString(), e);
            }
        }
        final String _host = host;
        final int _port = port;
        final Socket[] sockp = new Socket[1];
        final Exception[] ee = new Exception[1];
        String message = "";
        Thread tmp = new Thread(() -> {
            sockp[0] = null;
            try {
                sockp[0] = new Socket(_host, _port);
            } catch (final Exception e) {
                ee[0] = e;
                if (sockp[0] != null && sockp[0].isConnected()) {
                    try {
                        sockp[0].close();
                    } catch (final Exception ignored) {
                    }
                }
                sockp[0] = null;
            }
        });
        tmp.setName("Opening Socket " + host);
        tmp.start();
        try {
            tmp.join(timeout);
            message = "timeout: ";
        } catch (final InterruptedException ignored) {
        }
        if (sockp[0] != null && sockp[0].isConnected()) {
            return sockp[0];
        } else {
            message += "socket is not established";
            if (ee[0] != null) {
                message = ee[0].toString();
            }
            tmp.interrupt();
            tmp = null;
            throw new JSchException(message, ee[0]);
        }
    }

    static void wipe(final ByteBuffer v) {
        if (v.hasArray()) {
            final int start = v.arrayOffset() + v.position();
            Arrays.fill(v.array(), start, start + v.remaining(), (byte) 0);
        } else {
            final int len = v.remaining();
            v.put(new byte[len]);
            v.position(v.position() - len);
        }
    }

    static void wipe(final CharBuffer v) {
        if (v.hasArray()) {
            final int start = v.arrayOffset() + v.position();
            Arrays.fill(v.array(), start, start + v.remaining(), (char) 0);
        } else {
            final int len = v.remaining();
            v.put(new char[len]);
            v.position(v.position() - len);
        }
    }

    static void wipe(final CharSequence v) {
        if (v instanceof CharBuffer) {
            wipe((CharBuffer) v);
        }
    }

    static void wipe(final char[] v) {
        Arrays.fill(v, (char) 0);
    }

    static byte[] toArray(final ByteBuffer v, final boolean wipe) {
        if (v == null)
            return null;
        if (v.hasArray() &&
                v.arrayOffset() == 0 && v.position() == 0 && v.remaining() == v.array().length)
            return v.array();
        final byte[] r = new byte[v.remaining()];
        v.get(r);
        v.position(v.position() - r.length);
        if (wipe)
            wipe(v);
        return r;
    }

    static byte[] str2byte(final CharSequence str, final Charset encoding) {
        if (str == null)
            return null;
        if (str instanceof String)
            return ((String) str).getBytes(encoding);
        if (str instanceof CharBuffer)
            return toArray(encoding.encode((CharBuffer) str), true);
        return toArray(encoding.encode(CharBuffer.wrap(str)), true);
    }

    static byte[] str2byte(final char[] str, final Charset encoding) {
        if (str == null)
            return null;
        return toArray(encoding.encode(CharBuffer.wrap(str)), true);
    }

    static byte[] str2byte(final String str, final Charset encoding) {
        if (str == null)
            return null;
        return str.getBytes(encoding);
    }

    static byte[] str2byte(final CharSequence str) {
        return str2byte(str, UTF8);
    }

    static byte[] str2byte(final char[] str) {
        return str2byte(str, UTF8);
    }

    static byte[] str2byte(final String str) {
        return str2byte(str, UTF8);
    }

    static String byte2str(final byte[] str, final Charset encoding) {
        return byte2str(str, 0, str.length, encoding);
    }

    static String byte2str(final byte[] str, final int s, final int l, final Charset encoding) {
        return new String(str, s, l, encoding);
    }

    static String byte2str(final byte[] str) {
        return byte2str(str, 0, str.length, UTF8);
    }

    static String byte2str(final byte[] str, final int s, final int l) {
        return byte2str(str, s, l, UTF8);
    }

    static CharBuffer byte2char(final byte[] str, final Charset encoding) {
        return encoding.decode(ByteBuffer.wrap(str));
    }

    static CharBuffer byte2char(final byte[] str) {
        return byte2char(str, UTF8);
    }

    static String toHex(final byte[] str) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length; i++) {
            final String foo = Integer.toHexString(str[i] & 0xff);
            sb.append("0x").append(foo.length() == 1 ? "0" : "").append(foo);
            if (i + 1 < str.length)
                sb.append(":");
        }
        return sb.toString();
    }

    static final byte[] empty = str2byte("");

    static void bzero(final byte[] foo) {
        if (foo == null)
            return;
        Arrays.fill(foo, (byte) 0);
    }

    private static String getHomeDir() {
        throw new UnsupportedOperationException();
    }

    static String checkTilde(final String str) {
        if (str.startsWith("~"))
            return str.replace("~", getHomeDir());
        return str;
    }

    private static int skipUTF8Char(final byte b) {
        if ((byte) (b & 0x80) == 0) return 1;
        if ((byte) (b & 0xe0) == (byte) 0xc0) return 2;
        if ((byte) (b & 0xf0) == (byte) 0xe0) return 3;
        return 1;
    }

    static byte[] fromFile(final String fileName) throws IOException {
        final File file = new File(checkTilde(fileName));
        try (final FileInputStream fis = new FileInputStream(file)) {
            final byte[] result = new byte[(int) (file.length())];
            int len = 0;
            while (true) {
                final int i = fis.read(result, len, result.length - len);
                if (i <= 0)
                    break;
                len += i;
            }
            return result;
        }
    }

    static String getEnvProperty(final String key, final String def) {
        return def;
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface Unique {
    }

    @SafeVarargs
    static <T> Set<T> setOf(@Unique final T... args) {
        final HashSet<T> r = new HashSet<>(args.length);
        Collections.addAll(r, args);
        return r;
    }

    // Old Androids stuff

    static <T> T requireNonNullElse(final T v, final T def) {
        return v != null ? v : def;
    }

    interface Predicate<T> {
        boolean test(T v);
    }

    static <T extends Collection<String>> T filter(final Predicate<? super String> f,
                                                   final T v) {
        final Iterator<String> it = v.iterator();
        while (it.hasNext())
            if (!f.test(it.next()))
                it.remove();
        return v;
    }

    static <T> Predicate<T> ifIn(final Collection<T> f) {
        return f::contains;
    }

    static <T> Predicate<T> ifNotIn(final Collection<T> f) {
        return v -> !f.contains(v);
    }
}
