package green_green_avk.anotherterm.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;

import green_green_avk.anotherterm.backends.BackendModule;

public interface PreferenceUiWrapper {

    class ParseException extends RuntimeException {
        public final View view;
        public final String key;
        public final Object value;

        public ParseException(final String message, final View view,
                              final String key, final Object value) {
            super(message);
            this.view = view;
            this.key = key;
            this.value = value;
        }
    }

    Object get(String key);

    void set(String key, Object value);

    @NonNull
    Map<String, Object> getPreferences();

    void setPreferences(@NonNull Map<String, ?> pp);

    void setDefaultPreferences(@NonNull Map<String, ?> pp);

    void setPreferencesMeta(@NonNull Map<String, BackendModule.Meta.ParameterMeta<?>> ppMeta);

    /**
     * @return Fields defined by user, not default.
     */
    @NonNull
    Set<String> getChangedFields();

    interface Callbacks {

        void onInitialized();

        /**
         * @param key field name.
         */
        void onChanged(String key);
    }

    void setCallbacks(@Nullable Callbacks callbacks);
}
