package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerActivity;
import green_green_avk.anotherterm.ui.ProfileManagerUi;

public final class AnsiColorManagerActivity extends ProfileManagerActivity<AnsiColorProfile> {
    @Override
    @NonNull
    protected ProfileManagerUi<AnsiColorProfile> getUi() {
        return AnsiColorManagerUi.instance;
    }
}
