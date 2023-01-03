package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerActivity;
import green_green_avk.anotherterm.ui.ProfileManagerUi;

public final class TermKeyMapManagerActivity extends ProfileManagerActivity<TermKeyMapRules> {
    @Override
    @NonNull
    protected ProfileManagerUi<TermKeyMapRules> getUi() {
        return TermKeyMapManagerUi.instance;
    }
}
