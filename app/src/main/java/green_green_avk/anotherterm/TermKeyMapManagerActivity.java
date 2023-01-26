package green_green_avk.anotherterm;

import androidx.annotation.NonNull;

import green_green_avk.anotherterm.ui.ProfileManagerActivity;

public final class TermKeyMapManagerActivity extends ProfileManagerActivity<TermKeyMapRules> {
    @Override
    @NonNull
    protected TermKeyMapManagerUi getUi() {
        return TermKeyMapManagerUi.instance;
    }
}
