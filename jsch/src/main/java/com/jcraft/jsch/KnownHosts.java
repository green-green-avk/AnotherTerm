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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

final class KnownHosts implements HostKeyRepository {
    private final JSch jsch;
    private String known_hosts = null;
    private final List<HostKey> pool = new ArrayList<>();

    private final MAC hmacsha1;

    KnownHosts(final JSch jsch) {
        super();
        this.jsch = jsch;
        this.hmacsha1 = createHMAC(JSch.getConfig("hmac-sha1"));
    }

    void setKnownHosts(final String filename) throws JSchException {
        try {
            known_hosts = filename;
            final FileInputStream fis = new FileInputStream(Util.checkTilde(filename));
            setKnownHosts(fis);
        } catch (final FileNotFoundException e) {
            // The non-existing file should be allowed.
        }
    }

    void setKnownHosts(final InputStream input) throws JSchException {
        pool.clear();
        final StringBuilder sb = new StringBuilder();
        byte i;
        int j;
        final boolean error = false;
        try {
            final InputStream fis = input;
            String host;
            String key;
            int type;
            byte[] buf = new byte[1024];
            int bufl;
            loop:
            while (true) {
                bufl = 0;
                while (true) {
                    j = fis.read();
                    if (j == -1) {
                        if (bufl == 0) {
                            break loop;
                        }
                        break;
                    }
                    if (j == 0x0d) {
                        continue;
                    }
                    if (j == 0x0a) {
                        break;
                    }
                    if (buf.length <= bufl) {
                        if (bufl > 1024 * 10) break;   // too long...
                        final byte[] newbuf = new byte[buf.length * 2];
                        System.arraycopy(buf, 0, newbuf, 0, buf.length);
                        buf = newbuf;
                    }
                    buf[bufl++] = (byte) j;
                }

                j = 0;
                while (j < bufl) {
                    i = buf[j];
                    if (i == ' ' || i == '\t') {
                        j++;
                        continue;
                    }
                    if (i == '#') {
                        addInvalidLine(Util.byte2str(buf, 0, bufl));
                        continue loop;
                    }
                    break;
                }
                if (j >= bufl) {
                    addInvalidLine(Util.byte2str(buf, 0, bufl));
                    continue loop;
                }

                sb.setLength(0);
                while (j < bufl) {
                    i = buf[j++];
                    if (i == 0x20 || i == '\t') {
                        break;
                    }
                    sb.append((char) i);
                }
                host = sb.toString();
                if (j >= bufl || host.isEmpty()) {
                    addInvalidLine(Util.byte2str(buf, 0, bufl));
                    continue loop;
                }

                while (j < bufl) {
                    i = buf[j];
                    if (i == ' ' || i == '\t') {
                        j++;
                        continue;
                    }
                    break;
                }

                String marker = "";
                if (host.charAt(0) == '@') {
                    marker = host;

                    sb.setLength(0);
                    while (j < bufl) {
                        i = buf[j++];
                        if (i == 0x20 || i == '\t') {
                            break;
                        }
                        sb.append((char) i);
                    }
                    host = sb.toString();
                    if (j >= bufl || host.isEmpty()) {
                        addInvalidLine(Util.byte2str(buf, 0, bufl));
                        continue loop;
                    }

                    while (j < bufl) {
                        i = buf[j];
                        if (i == ' ' || i == '\t') {
                            j++;
                            continue;
                        }
                        break;
                    }
                }

                sb.setLength(0);
                type = -1;
                while (j < bufl) {
                    i = buf[j++];
                    if (i == 0x20 || i == '\t') {
                        break;
                    }
                    sb.append((char) i);
                }
                final String typeStr = sb.toString();
                final int typeId = HostKey.name2type(typeStr);
                if (typeId != HostKey.UNKNOWN) {
                    type = typeId;
                } else {
                    j = bufl;
                }
                if (j >= bufl) {
                    addInvalidLine(Util.byte2str(buf, 0, bufl));
                    continue loop;
                }

                while (j < bufl) {
                    i = buf[j];
                    if (i == ' ' || i == '\t') {
                        j++;
                        continue;
                    }
                    break;
                }

                sb.setLength(0);
                while (j < bufl) {
                    i = buf[j++];
                    if (i == 0x0d) {
                        continue;
                    }
                    if (i == 0x0a) {
                        break;
                    }
                    if (i == 0x20 || i == '\t') {
                        break;
                    }
                    sb.append((char) i);
                }
                key = sb.toString();
                if (key.isEmpty()) {
                    addInvalidLine(Util.byte2str(buf, 0, bufl));
                    continue loop;
                }

                while (j < bufl) {
                    i = buf[j];
                    if (i == ' ' || i == '\t') {
                        j++;
                        continue;
                    }
                    break;
                }

                /*
                 "man sshd" has following descriptions,
                 Note that the lines in these files are typically hundreds
                 of characters long, and you definitely don't want to type
                 in the host keys by hand.  Rather, generate them by a script,
                 ssh-keyscan(1) or by taking /usr/local/etc/ssh_host_key.pub and
                 adding the host names at the front.
                 This means that a comment is allowed to appear at the end of each
                 key entry.
                 */
                String comment = null;
                if (j < bufl) {
                    sb.setLength(0);
                    while (j < bufl) {
                        i = buf[j++];
                        if (i == 0x0d) {
                            continue;
                        }
                        if (i == 0x0a) {
                            break;
                        }
                        sb.append((char) i);
                    }
                    comment = sb.toString();
                }

                //System.err.println(host);
                //System.err.println("|"+key+"|");

                final HostKey hk = new HashedHostKey(marker, host, type,
                        Util.fromBase64(Util.str2byte(key), 0,
                                key.length()), comment);
                pool.add(hk);
            }
            if (error) {
                throw new JSchException("KnownHosts: invalid format");
            }
        } catch (final Exception e) {
            if (e instanceof JSchException)
                throw (JSchException) e;
            throw new JSchException(e.toString(), e);
        } finally {
            try {
                input.close();
            } catch (final IOException e) {
//                throw new JSchException(e.toString(), e); // TODO: was it correct?
            }
        }
    }

    private void addInvalidLine(final String line) throws JSchException {
        final HostKey hk = new HostKey(line, HostKey.UNKNOWN, null);
        pool.add(hk);
    }

    String getKnownHostsFile() {
        return known_hosts;
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return known_hosts;
    }

    @Override
    public int check(final String host, final byte[] key) {
        int result = NOT_INCLUDED;
        if (host == null) {
            return result;
        }

        final HostKey hk;
        try {
            hk = new HostKey(host, HostKey.GUESS, key);
        } catch (final Exception e) {  // unsupported key
            final Logger l = jsch.getInstanceLogger();
            if (l.isEnabled(Logger.VERBOSE))
                l.log(Logger.VERBOSE, "exception while trying to read key while checking host '" + host + "'", e);
            return result;
        }

        synchronized (pool) {
            for (final HostKey _hk : pool) {
                if (_hk.isMatched(host) && _hk.type == hk.type) {
                    if (Arrays.equals(_hk.key, key)) {
                        return OK;
                    }
                    result = CHANGED;
                }
            }
        }

        if (result == NOT_INCLUDED &&
                host.startsWith("[") &&
                host.indexOf("]:") > 1
        ) {
            return check(host.substring(1, host.indexOf("]:")), key);
        }

        return result;
    }

    @Override
    public void add(final HostKey hostkey, final UserInfo userinfo) {
        final int type = hostkey.type;
        final String host = hostkey.getHost();
//    byte[] key=hostkey.key;

        synchronized (pool) {
            for (final HostKey hk : pool) {
                if (hk.isMatched(host) && hk.type == type) {
/*
          if(Util.array_equals(hk.key, key)){ return; }
          if(hk.host.equals(host)){
            hk.key=key;
            return;
          }
          else{
            hk.host=deleteSubString(hk.host, host);
            break;
          }
*/
                }
            }
        }

        pool.add(hostkey);

        syncKnownHostsFile(userinfo);
    }

    void syncKnownHostsFile(final UserInfo userinfo) {
        final String khFilename = getKnownHostsRepositoryID();
        if (khFilename == null) {
            return;
        }
        boolean doSync = true;
        File goo = new File(Util.checkTilde(khFilename));
        if (!goo.exists()) {
            doSync = false;
            if (userinfo != null) {
                doSync = userinfo.promptYesNo(null, UserInfo.Message.SIMPLE_MESSAGE,
                        khFilename + " does not exist.\n" +
                                "Are you sure you want to create it?",
                        "en"
                );
                goo = goo.getParentFile();
                if (doSync && goo != null && !goo.exists()) {
                    doSync = userinfo.promptYesNo(null, UserInfo.Message.SIMPLE_MESSAGE,
                            "The parent directory " + goo + " does not exist.\n" +
                                    "Are you sure you want to create it?",
                            "en"
                    );
                    if (doSync) {
                        if (!goo.mkdirs()) {
                            userinfo.showMessage(goo + " has not been created.");
                            doSync = false;
                        } else {
                            userinfo.showMessage(goo + " has been succesfully created.\nPlease check its access permission.");
                        }
                    }
                }
                if (goo == null) doSync = false;
            }
        }
        if (!doSync) {
            return;
        }
        try {
            sync(khFilename);
        } catch (final Exception e) {
            jsch.getInstanceLogger().log(Logger.ERROR, "unable to sync known host file " + goo.getPath(), e);
        }
    }

    @Override
    public HostKey[] getHostKey() {
        return getHostKey(null, null);
    }

    @Override
    public HostKey[] getHostKey(final String host, final String type) {
        synchronized (pool) {
            final List<HostKey> v = new ArrayList<>();
            for (final HostKey hk : pool) {
                if (hk.type == HostKey.UNKNOWN) continue;
                if (host == null ||
                        (hk.isMatched(host) &&
                                (type == null || hk.getType().equals(type)))) {
                    v.add(hk);
                }
            }
            HostKey[] foo = v.toArray(new HostKey[0]);
            if (host != null && host.startsWith("[") && host.indexOf("]:") > 1) {
                final HostKey[] tmp =
                        getHostKey(host.substring(1, host.indexOf("]:")), type);
                if (tmp.length > 0) {
                    final HostKey[] bar = new HostKey[foo.length + tmp.length];
                    System.arraycopy(foo, 0, bar, 0, foo.length);
                    System.arraycopy(tmp, 0, bar, foo.length, tmp.length);
                    foo = bar;
                }
            }
            return foo;
        }
    }

    @Override
    public void remove(final String host, final String type) {
        remove(host, type, null);
    }

    @Override
    public void remove(final String host, final String type, final byte[] key) {
        boolean sync = false;
        synchronized (pool) {
            final Iterator<HostKey> it = pool.iterator();
            while (it.hasNext()) {
                final HostKey hk = it.next();
                if (host == null ||
                        (hk.isMatched(host) &&
                                (type == null || (hk.getType().equals(type) &&
                                        (key == null || Arrays.equals(key, hk.key)))))) {
                    final String hosts = hk.getHost();
                    if (host == null || hosts.equals(host) ||
                            ((hk instanceof HashedHostKey) &&
                                    ((HashedHostKey) hk).isHashed())) {
                        it.remove();
                    } else {
                        hk.host = deleteSubString(hosts, host);
                    }
                    sync = true;
                }
            }
        }
        if (sync) {
            try {
                sync();
            } catch (final Exception ignored) {
            }
        }
    }

    void sync() throws IOException {
        if (known_hosts != null)
            sync(known_hosts);
    }

    synchronized void sync(final String foo) throws IOException {
        if (foo == null) return;
        try (final FileOutputStream fos = new FileOutputStream(Util.checkTilde(foo))) {
            dump(fos);
        }
    }

    private static final byte[] space = {(byte) 0x20};
    private static final byte[] lf = Util.str2byte("\n");

    void dump(final OutputStream out) {
        try {
            synchronized (pool) {
                for (final HostKey hk : pool) {
                    dumpHostKey(out, hk);
                }
            }
        } catch (final Exception e) {
            jsch.getInstanceLogger().log(Logger.ERROR, "unable to dump known hosts", e);
        }
    }

    void dumpHostKey(final OutputStream out, final HostKey hk) throws IOException {
        final String marker = hk.getMarker();
        final String host = hk.getHost();
        final String type = hk.getType();
        final String comment = hk.getComment();
        if ("UNKNOWN".equals(type)) {
            out.write(Util.str2byte(host));
            out.write(lf);
            return;
        }
        if (!marker.isEmpty()) {
            out.write(Util.str2byte(marker));
            out.write(space);
        }
        out.write(Util.str2byte(host));
        out.write(space);
        out.write(Util.str2byte(type));
        out.write(space);
        out.write(Util.str2byte(hk.getKey()));

        if (comment != null) {
            out.write(space);
            out.write(Util.str2byte(comment));
        }
        out.write(lf);
    }

    String deleteSubString(final String hosts, final String host) {
        final int hostLen = host.length();
        final int hostsLen = hosts.length();
        int i = 0;
        while (i < hostsLen) {
            final int j = hosts.indexOf(',', i);
            if (j == -1) break;
            if (!host.equals(hosts.substring(i, j))) {
                i = j + 1;
                continue;
            }
            return hosts.substring(0, i) + hosts.substring(j + 1);
        }
        if (hosts.endsWith(host) && hostsLen - i == hostLen) {
            return hosts.substring(0, (hostLen == hostsLen) ? 0 : hostsLen - hostLen - 1);
        }
        return hosts;
    }

    MAC getHMACSHA1() {
        return hmacsha1;
    }

    MAC createHMAC(final String hmacClassname) {
        try {
            final Class<? extends MAC> c = Class.forName(hmacClassname).asSubclass(MAC.class);
            return c.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            jsch.getInstanceLogger().log(Logger.ERROR,
                    "Unable to instantiate HMAC class: " + hmacClassname, e);
            throw new IllegalArgumentException(
                    "Instantiation of " + hmacClassname + " leads to an error", e);
        }
    }

    HostKey createHashedHostKey(final String host, final byte[] key) throws JSchException {
        final HashedHostKey hhk = new HashedHostKey(host, key);
        hhk.hash();
        return hhk;
    }

    class HashedHostKey extends HostKey {
        private static final String HASH_MAGIC = "|1|";
        private static final String HASH_DELIM = "|";

        private boolean hashed = false;
        byte[] salt = null;
        byte[] hash = null;

        HashedHostKey(final String host, final byte[] key) throws JSchException {
            this(host, GUESS, key);
        }

        HashedHostKey(final String host, final int type, final byte[] key) throws JSchException {
            this("", host, type, key, null);
        }

        HashedHostKey(final String marker, final String host, final int type, final byte[] key,
                      final String comment) throws JSchException {
            super(marker, host, type, key, comment);
            if (this.host.startsWith(HASH_MAGIC) &&
                    this.host.substring(HASH_MAGIC.length()).indexOf(HASH_DELIM) > 0) {
                final String data = this.host.substring(HASH_MAGIC.length());
                final String _salt = data.substring(0, data.indexOf(HASH_DELIM));
                final String _hash = data.substring(data.indexOf(HASH_DELIM) + 1);
                salt = Util.fromBase64(Util.str2byte(_salt), 0, _salt.length());
                hash = Util.fromBase64(Util.str2byte(_hash), 0, _hash.length());
                final int blockSize = hmacsha1.getBlockSize();
                if (salt.length != blockSize || hash.length != blockSize) {
                    salt = null;
                    hash = null;
                    return;
                }
                hashed = true;
            }
        }

        @Override
        boolean isMatched(final String _host) {
            if (!hashed) {
                return super.isMatched(_host);
            }
            try {
                synchronized (hmacsha1) {
                    hmacsha1.init(salt);
                    final byte[] foo = Util.str2byte(_host);
                    hmacsha1.update(foo, 0, foo.length);
                    final byte[] bar = new byte[hmacsha1.getBlockSize()];
                    hmacsha1.doFinal(bar, 0);
                    return Arrays.equals(hash, bar);
                }
            } catch (final Exception e) {
                jsch.getInstanceLogger().log(Logger.ERROR,
                        "an error occurred while trying to check hash for host "
                                + _host, e);
            }
            return false;
        }

        boolean isHashed() {
            return hashed;
        }

        void hash() {
            if (hashed)
                return;
            if (salt == null) {
                final Random random = Session.random;
                synchronized (random) {
                    salt = new byte[hmacsha1.getBlockSize()];
                    random.fill(salt, 0, salt.length);
                }
            }
            try {
                synchronized (hmacsha1) {
                    hmacsha1.init(salt);
                    final byte[] foo = Util.str2byte(host);
                    hmacsha1.update(foo, 0, foo.length);
                    hash = new byte[hmacsha1.getBlockSize()];
                    hmacsha1.doFinal(hash, 0);
                }
            } catch (final Exception e) {
                jsch.getInstanceLogger().log(Logger.ERROR, "an error occurred while trying to calculate the hash for host " + host, e);
                salt = null;
                hash = null;
                return;
            }
            host = HASH_MAGIC + Util.byte2str(Util.toBase64(salt, true)) +
                    HASH_DELIM + Util.byte2str(Util.toBase64(hash, true));
            hashed = true;
        }
    }
}
