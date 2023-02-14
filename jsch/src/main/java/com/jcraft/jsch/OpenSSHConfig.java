/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2013-2018 ymnk, JCraft,Inc. All rights reserved.

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class implements ConfigRepository interface, and parses
 * OpenSSH's configuration file.  The following keywords will be recognized,
 * <ul>
 *   <li>Host</li>
 *   <li>User</li>
 *   <li>Hostname</li>
 *   <li>Port</li>
 *   <li>PreferredAuthentications</li>
 *   <li>PubkeyAcceptedAlgorithms</li>
 *   <li>FingerprintHash (removed and no-op)</li>
 *   <li>IdentityFile</li>
 *   <li>NumberOfPasswordPrompts</li>
 *   <li>ConnectTimeout</li>
 *   <li>HostKeyAlias</li>
 *   <li>UserKnownHostsFile</li>
 *   <li>KexAlgorithms</li>
 *   <li>HostKeyAlgorithms</li>
 *   <li>Ciphers</li>
 *   <li>Macs</li>
 *   <li>Compression</li>
 *   <li>CompressionLevel</li>
 *   <li>ForwardAgent</li>
 *   <li>RequestTTY</li>
 *   <li>ServerAliveInterval</li>
 *   <li>LocalForward</li>
 *   <li>RemoteForward</li>
 *   <li>ClearAllForwardings</li>
 * </ul>
 *
 * @see ConfigRepository
 */
public class OpenSSHConfig implements ConfigRepository {

    private static final Set<String> keysWithListAdoption;

    static {
        final Set<String> set = new HashSet<>();
        for (final String s : Arrays.asList("KexAlgorithms",
                "Ciphers",
                "HostKeyAlgorithms",
                "MACs",
                "PubkeyAcceptedAlgorithms",
                "PubkeyAcceptedKeyTypes")) {
            final String toUpperCase = s.toUpperCase();
            set.add(toUpperCase);
        }
        keysWithListAdoption = set;
    }

    /**
     * Parses the given string, and returns an instance of ConfigRepository.
     *
     * @param conf string, which includes OpenSSH's config
     * @return an instanceof OpenSSHConfig
     */
    public static OpenSSHConfig parse(final String conf) throws IOException {
        try (final Reader r = new StringReader(conf)) {
            try (final BufferedReader br = new BufferedReader(r)) {
                return new OpenSSHConfig(br);
            }
        }
    }

    /**
     * Parses the given file, and returns an instance of ConfigRepository.
     *
     * @param reader OpenSSH's config file reader
     * @return an instanceof OpenSSHConfig
     */
    public static OpenSSHConfig parseFile(final BufferedReader reader) throws IOException {
        return new OpenSSHConfig(reader);
    }

    OpenSSHConfig(final BufferedReader br) throws IOException {
        _parse(br);
    }

    private final Map<String, List<String[]>> config = new HashMap<>();
    private final List<String> hosts = new ArrayList<>();

    private void _parse(final BufferedReader br) throws IOException {
        String host = "";
        List<String[]> kv = new ArrayList<>();
        String l;

        while ((l = br.readLine()) != null) {
            l = l.trim();
            if (l.isEmpty() || l.charAt(0) == '#')
                continue;

            final String[] key_value = l.split("[= \t]", 2);
            for (int i = 0; i < key_value.length; i++)
                key_value[i] = key_value[i].trim();

            if (key_value.length <= 1)
                continue;

            if ("Host".equalsIgnoreCase(key_value[0])) {
                config.put(host, kv);
                hosts.add(host);
                host = key_value[1];
                kv = new ArrayList<>();
            } else {
                kv.add(key_value);
            }
        }
        config.put(host, kv);
        hosts.add(host);
    }

    @Override
    public Config getConfig(final String host) {
        return new MyConfig(host);
    }

    /**
     * Returns mapping of jsch config property names to OpenSSH property names.
     *
     * @return map
     */
    static Map<String, String> getKeymap() {
        return keymap;
    }

    private static final Map<String, String> keymap = new HashMap<>();

    static {
        keymap.put("kex", "KexAlgorithms");
        keymap.put("server_host_key", "HostKeyAlgorithms");
        keymap.put("cipher.c2s", "Ciphers");
        keymap.put("cipher.s2c", "Ciphers");
        keymap.put("mac.c2s", "Macs");
        keymap.put("mac.s2c", "Macs");
        keymap.put("compression.s2c", "Compression");
        keymap.put("compression.c2s", "Compression");
        keymap.put("compression_level", "CompressionLevel");
        keymap.put("MaxAuthTries", "NumberOfPasswordPrompts");
    }

    class MyConfig implements Config {

        private final String host;
        private final List<List<String[]>> _configs = new ArrayList<>();

        MyConfig(final String host) {
            this.host = host;

            _configs.add(config.get(""));

            final byte[] _host = Util.str2byte(host);
            if (hosts.size() > 1) {
                for (int i = 1; i < hosts.size(); i++) {
                    final String[] patterns = hosts.get(i).split("[ \t]");
                    for (final String pattern : patterns) {
                        boolean negate = false;
                        String foo = pattern.trim();
                        if (foo.startsWith("!")) {
                            negate = true;
                            foo = foo.substring(1).trim();
                        }
                        if (Util.glob(Util.str2byte(foo), _host)) {
                            if (!negate) {
                                _configs.add(config.get(hosts.get(i)));
                            }
                        } else if (negate) {
                            _configs.add(config.get(hosts.get(i)));
                        }
                    }
                }
            }
        }

        private String find(String key) {
            final String originalKey = key;
            key = Util.requireNonNullElse(keymap.get(key), key);
            key = key.toUpperCase();
            String value = null;
            for (final List<String[]> v : _configs) {
                for (final String[] kv : v) {
                    if (kv[0].toUpperCase().equals(key)) {
                        value = kv[1];
                        break;
                    }
                }
                if (value != null)
                    break;
            }
            // TODO: The following change should be applied,
            //       but it is breaking changes.
            //       The consensus is required to enable it.
      /*
      if(value!=null &&
         (key.equals("SERVERALIVEINTERVAL") ||
          key.equals("CONNECTTIMEOUT"))){
        try {
          int timeout = Integer.parseInt(value);
          value = Integer.toString(timeout*1000);
        } catch (final NumberFormatException e) {
        }
      }
      */

            if (keysWithListAdoption.contains(key) && value != null && (value.startsWith("+") ||
                    value.startsWith("-") || value.startsWith("^"))) {

                final String origConfig = JSch.getConfig(originalKey).trim();

                if (value.startsWith("+")) {
                    value = origConfig + "," + value.substring(1).trim();
                } else if (value.startsWith("-")) {
                    final List<String> algList =
                            new ArrayList<>(Arrays.asList(Util.split(origConfig, ",")));
                    for (final String alg : Util.split(value.substring(1).trim(), ",")) {
                        algList.remove(alg.trim());
                    }
                    value = String.join(",", algList);
                } else if (value.startsWith("^")) {
                    value = value.substring(1).trim() + "," + origConfig;
                }
            }

            return value;
        }

        private String[] multiFind(String key) {
            key = key.toUpperCase();
            final List<String> value = new ArrayList<>();
            for (int i = 0; i < _configs.size(); i++) {
                final List<String[]> v = _configs.get(i);
                for (int j = 0; j < v.size(); j++) {
                    final String[] kv = v.get(j);
                    if (kv[0].toUpperCase().equals(key)) {
                        final String foo = kv[1];
                        if (foo != null) {
                            value.remove(foo);
                            value.add(foo);
                        }
                    }
                }
            }
            return value.toArray(new String[0]);
        }

        @Override
        public String getHostname() {
            return find("Hostname");
        }

        @Override
        public String getUser() {
            return find("User");
        }

        @Override
        public int getPort() {
            final String foo = find("Port");
            int port = -1;
            try {
                port = Integer.parseInt(foo);
            } catch (final NumberFormatException e) {
                // wrong format
            }
            return port;
        }

        @Override
        public String getValue(final String key) {
            switch (key) {
                case "compression.s2c":
                case "compression.c2s":
                    final String foo = find(key);
                    if (foo == null || "no".equals(foo))
                        return "none,zlib@openssh.com,zlib";
                    return "zlib@openssh.com,zlib,none";
            }
            return find(key);
        }

        @Override
        public String[] getValues(final String key) {
            return multiFind(key);
        }
    }
}
