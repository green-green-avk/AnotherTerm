package green_green_avk.anotherterm;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TermKeyMapRulesParser {
    private TermKeyMapRulesParser() {
    }

    private static final class RulesFromSP implements TermKeyMapRules.Editable,
            TermKeyMapRules.UriImportable, TermKeyMapRules.UriExportable {
        private static final Pattern keyP_v1 = Pattern.compile("^([0-9]{1,3})_([0-9])_(.)");

        @NonNull
        private final Map<String, Object> map;
        private final transient Map<Integer, Object> fastMap = new HashMap<>();

        private void syncFastMap() {
            for (final Map.Entry<String, Object> me : map.entrySet()) {
                try {
                    this.fastMap.put(Integer.parseInt(me.getKey(), 16), me.getValue());
                } catch (final NumberFormatException ignored) {
                }
            }
        }

        private RulesFromSP(@NonNull final Map<String, Object> map) {
            if (!map.containsKey("V")) {
                this.map = new HashMap<>();
                for (final Map.Entry<String, Object> me : map.entrySet()) {
                    putEntry_v1(me.getKey(), me.getValue());
                }
                this.map.put("V", "2");
                return;
            }
            this.map = map;
            syncFastMap();
        }

        private void putEntry_v1(@NonNull final String key, final Object value) {
            final Matcher m = keyP_v1.matcher(key);
            if (m.find()) {
                final int hash = getKeyHash(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        m.group(3).toLowerCase().charAt(0) == 't'
                );
                this.fastMap.put(hash, value);
                this.map.put(Integer.toHexString(hash), value);
            }
        }

        private void putEntry_v2(@NonNull final String key, final Object value) {
            try {
                this.fastMap.put(Integer.parseInt(key, 16), value);
                this.map.put(key, value);
            } catch (final NumberFormatException ignored) {
            }
        }

        private static int getKeyAmHash(final int code) {
            return code | 0x8000;
        }

        private static int getKeyHash(final int code, final int modifiers, final boolean appMode) {
            return code | (modifiers << 10) | (appMode ? 0x4000 : 0);
        }

        private int getKeyHash(final int code, final int modifiers, final int appMode) {
            return getKeyHash(code, modifiers, (getAppMode(code) & appMode) != 0);
        }

        private static boolean isKeyCodeValid(final int code) {
            return code < 1024 && code >= 0;
        }

        @Override
        public int getAppMode(final int code) {
            if (!isKeyCodeValid(code)) return TermKeyMap.APP_MODE_DEFAULT;
            final Object am = fastMap.get(getKeyAmHash(code));
            if (am == null) return TermKeyMap.APP_MODE_DEFAULT;
            try {
                return Integer.parseInt((String) am);
            } catch (final ClassCastException e) {
                return TermKeyMap.APP_MODE_DEFAULT;
            }
        }

        @Nullable
        @Override
        public String get(final int code, final int modifiers, final int appMode) {
            if (!isKeyCodeValid(code)) return null;
            try {
                return (String) fastMap.get(getKeyHash(code, modifiers, appMode));
            } catch (final ClassCastException e) {
                return null;
            }
        }

        @Override
        public void setAppMode(final int code, final int appMode) {
            if (!isKeyCodeValid(code)) return;
            final int hash = getKeyAmHash(code);
            fastMap.put(hash, appMode);
            map.put(Integer.toHexString(hash), appMode);
        }

        @Override
        public void set(final int code, final int modifiers, final int appMode,
                        @Nullable final String keyOutput) {
            if (!isKeyCodeValid(code)) return;
            final int hash = getKeyHash(code, modifiers, appMode);
            final String strHash = Integer.toHexString(hash);
            if (keyOutput == null) {
                fastMap.remove(hash);
                map.remove(strHash);
            } else {
                fastMap.put(hash, keyOutput);
                map.put(strHash, keyOutput);
            }
        }

        private static final String uriScheme = "termkeymap";
        private static final Pattern uriCheckP_v1 = Pattern.compile("^[0-9]");
        private static final Pattern uriCheckP_v2 = Pattern.compile("^[0-9a-fA-F]+$");

        @Override
        public void fromUri(@NonNull final Uri uri) {
            if (uriScheme.equals(uri.getScheme())) {
                final String path = uri.getPath();
                if (path != null) {
                    // TODO: '+' decoding issue before Jelly Bean
                    switch (path) {
                        case "/v1":
                            for (final String k : uri.getQueryParameterNames()) {
                                if (!uriCheckP_v1.matcher(k).find()) continue;
                                final String v = uri.getQueryParameter(k);
                                if (v == null || v.isEmpty()) continue;
                                putEntry_v1(k, v);
                            }
                            return;
                        case "/v2":
                            for (final String k : uri.getQueryParameterNames()) {
                                if (!uriCheckP_v2.matcher(k).find()) continue;
                                final String v = uri.getQueryParameter(k);
                                if (v == null || v.isEmpty()) continue;
                                putEntry_v2(k, v);
                            }
                            return;
                    }
                }
            }
            throw new IllegalArgumentException("Unsupported URI format");
        }

        @NonNull
        @Override
        public Uri toUri() {
            final Uri.Builder b = new Uri.Builder()
                    .scheme(uriScheme)
                    .path("/v2");
            for (final String k : map.keySet()) {
                if (!"V".equals(k))
                    b.appendQueryParameter(k, map.get(k).toString());
            }
            return b.build();
        }

        private void readObject(@NonNull final java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            in.defaultReadObject();
            syncFastMap();
        }
    }

    @NonNull
    public static TermKeyMapRules.Editable getNew() {
        return new RulesFromSP(new HashMap<>());
    }

    @NonNull
    public static TermKeyMapRules.Editable fromSP(@NonNull final Map<String, Object> vv) {
        return new RulesFromSP(vv);
    }

    @NonNull
    public static Map<String, Object> toSP(@NonNull final TermKeyMapRules rules) {
        if (rules instanceof RulesFromSP) return ((RulesFromSP) rules).map;
        throw new IllegalArgumentException("Key mapping rules cannot be saved");
    }

    @NonNull
    public static TermKeyMapRules.Editable fromUri(@NonNull final Uri uri) {
        final RulesFromSP rr = new RulesFromSP(new HashMap<>());
        rr.fromUri(uri);
        return rr;
    }
}
