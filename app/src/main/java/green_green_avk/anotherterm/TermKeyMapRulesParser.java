package green_green_avk.anotherterm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class TermKeyMapRulesParser {
    private TermKeyMapRulesParser() {
    }

    private static final class RulesFromSP implements TermKeyMapRules.Editable {
        private final Map<String, Object> map;

        public RulesFromSP(Map<String, ?> map) {
            this.map = (Map<String, Object>) map;
        }

        @NonNull
        private String getKey(int code) {
            return String.format(Locale.ROOT, "%d", code);
        }

        @NonNull
        private String getKey(int code, int modifiers, int appMode) {
            return String.format(Locale.ROOT, "%d_%d_%b", code, modifiers, (getAppMode(code) & appMode) != 0);
        }

        @Override
        public int getAppMode(int code) {
            try {
                return (int) (Object) map.get(getKey(code));
            } catch (NullPointerException e) {
                return TermKeyMap.APP_MODE_DEFAULT;
            } catch (ClassCastException e) {
                return TermKeyMap.APP_MODE_DEFAULT;
            }
        }

        @Nullable
        @Override
        public String get(int code, int modifiers, int appMode) {
            try {
                return (String) map.get(getKey(code, modifiers, appMode));
            } catch (ClassCastException e) {
                return null;
            }
        }

        @Override
        public void setAppMode(int code, int appMode) {
            map.put(getKey(code), appMode);
        }

        @Override
        public void set(int code, int modifiers, int appMode, @Nullable String keyOutput) {
            if (keyOutput == null) map.remove(getKey(code, modifiers, appMode));
            else map.put(getKey(code, modifiers, appMode), keyOutput);
        }
    }

    @NonNull
    public static TermKeyMapRules.Editable getNew() {
        return new RulesFromSP(new HashMap<String, Object>());
    }

    @NonNull
    public static TermKeyMapRules.Editable fromSP(Map<String, ?> vv) {
        return new RulesFromSP(vv);
    }

    @NonNull
    public static Map<String, ?> toSP(@NonNull TermKeyMapRules rules) {
        if (rules instanceof RulesFromSP) return ((RulesFromSP) rules).map;
        throw new IllegalArgumentException("Key mapping rules cannot be saved");
    }
}
