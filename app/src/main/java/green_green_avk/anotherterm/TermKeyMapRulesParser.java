package green_green_avk.anotherterm;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class TermKeyMapRulesParser {
    private TermKeyMapRulesParser() {
    }

    private static final class RulesFromSP implements TermKeyMapRules.Editable,
            TermKeyMapRules.UriImportable, TermKeyMapRules.UriExportable {
        @NonNull
        private final Map<String, Object> map;

        private RulesFromSP(@NonNull final Map<String, ?> map) {
            this.map = (Map<String, Object>) map;
        }

        @NonNull
        private String getKey(final int code) {
            return String.format(Locale.ROOT, "%d", code);
        }

        @NonNull
        private String getKey(final int code, final int modifiers, final int appMode) {
            return String.format(Locale.ROOT, "%d_%d_%b", code, modifiers, (getAppMode(code) & appMode) != 0);
        }

        @Override
        public int getAppMode(final int code) {
            try {
                return (int) (Object) map.get(getKey(code));
            } catch (final NullPointerException e) {
                return TermKeyMap.APP_MODE_DEFAULT;
            } catch (final ClassCastException e) {
                return TermKeyMap.APP_MODE_DEFAULT;
            }
        }

        @Nullable
        @Override
        public String get(final int code, final int modifiers, final int appMode) {
            try {
                return (String) map.get(getKey(code, modifiers, appMode));
            } catch (final ClassCastException e) {
                return null;
            }
        }

        @Override
        public void setAppMode(final int code, final int appMode) {
            map.put(getKey(code), appMode);
        }

        @Override
        public void set(final int code, final int modifiers, final int appMode,
                        @Nullable String keyOutput) {
            if (keyOutput == null) map.remove(getKey(code, modifiers, appMode));
            else map.put(getKey(code, modifiers, appMode), keyOutput);
        }

        private static final String uriScheme = "termkeymap";
        private static final Pattern uriCheckP = Pattern.compile("^[0-9]");

        @Override
        public void fromUri(@NonNull final Uri uri) {
            if (!uriScheme.equals(uri.getScheme()) || !"/v1".equals(uri.getPath()))
                throw new IllegalArgumentException("Unsupported URI format");
            for (final String k : uri.getQueryParameterNames()) {
                // TODO: '+' decoding issue before Jelly Bean
                final String v = uri.getQueryParameter(k);
                if (v == null || v.isEmpty() || !uriCheckP.matcher(v).find())
                    continue;
                map.put(k, v);
            }
        }

        @NonNull
        @Override
        public Uri toUri() {
            final Uri.Builder b = new Uri.Builder()
                    .scheme(uriScheme)
                    .path("/v1");
            for (final String k : map.keySet()) {
                b.appendQueryParameter(k, map.get(k).toString());
            }
            return b.build();
        }
    }

    @NonNull
    public static TermKeyMapRules.Editable getNew() {
        return new RulesFromSP(new HashMap<String, Object>());
    }

    @NonNull
    public static TermKeyMapRules.Editable fromSP(@NonNull final Map<String, ?> vv) {
        return new RulesFromSP(vv);
    }

    @NonNull
    public static Map<String, ?> toSP(@NonNull final TermKeyMapRules rules) {
        if (rules instanceof RulesFromSP) return ((RulesFromSP) rules).map;
        throw new IllegalArgumentException("Key mapping rules cannot be saved");
    }

    @NonNull
    public static TermKeyMapRules.Editable fromUri(@NonNull final Uri uri) {
        final RulesFromSP rr = new RulesFromSP(new HashMap<String, Object>());
        rr.fromUri(uri);
        return rr;
    }
}
