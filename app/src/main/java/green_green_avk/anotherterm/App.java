package green_green_avk.anotherterm;

import android.app.Application;
import android.preference.PreferenceManager;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.libusbmanager.LibUsbManager;

public final class App extends Application {

    public static final class Settings extends green_green_avk.anotherterm.utils.Settings {
        @Keep
        @Param(defRes = R.bool.terminal_font_default_fromfiles)
        public boolean terminal_font_default_fromfiles;

        @Keep
        @Param(defRes = R.integer.terminal_font_default_size_sp)
        public int terminal_font_default_size_sp;

        @Keep
        @Param(defRes = R.integer.terminal_selection_pad_size_dp)
        public int terminal_selection_pad_size_dp;

        @Keep
        @Param(defRes = R.integer.terminal_key_height_dp)
        public int terminal_key_height_dp;

        @Keep
        @Param(defRes = R.bool.terminal_key_repeat)
        public boolean terminal_key_repeat;

        @Keep
        @Param(defRes = R.integer.terminal_key_repeat_delay)
        public int terminal_key_repeat_delay;

        @Keep
        @Param(defRes = R.integer.terminal_key_repeat_interval)
        public int terminal_key_repeat_interval;

        @Override
        protected void onAfterChange(@NonNull final String key, @Nullable final Object value) {
            if ("terminal_font_default_fromfiles".equals(key)) {
                FontsManager.setFrom(terminal_font_default_fromfiles);
            }
        }
    }

    public final Settings settings = new Settings();

    // Turned out, it's supposed that any obfuscated fields are reflection unreachable...
    // Must be kept in order to prevent its unexpected early finalization.
    @Keep
    public TermSh termSh = null;

    @Keep
    public LibUsbManager libUsbManager = null;

    @Override
    public void onCreate() {
        super.onCreate();
        PluginsManager.init(this);
        settings.init(this, PreferenceManager.getDefaultSharedPreferences(this));
        FontsManager.init(this);
        FontsManager.setFrom(settings.terminal_font_default_fromfiles);
        TermKeyMapManager.init(this);
        FavoritesManager.init(this);
        termSh = new TermSh(this);
        libUsbManager = new LibUsbManager(this);
    }
}
