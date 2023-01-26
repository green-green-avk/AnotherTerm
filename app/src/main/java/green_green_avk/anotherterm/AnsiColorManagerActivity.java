package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerActivity;

public final class AnsiColorManagerActivity extends ProfileManagerActivity<AnsiColorProfile> {
    @Override
    @NonNull
    protected AnsiColorManagerUi getUi() {
        return AnsiColorManagerUi.instance;
    }
}
