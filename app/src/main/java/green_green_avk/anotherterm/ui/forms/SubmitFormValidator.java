package green_green_avk.anotherterm.ui.forms;

import android.view.View;

import androidx.annotation.Nullable;

public class SubmitFormValidator extends FormValidator {
    private View submitButton = null;

    public final void setSubmitButton(@Nullable final View submitButton) {
        this.submitButton = submitButton;
        updateUi();
    }

    private void updateUi() {
        if (submitButton != null)
            submitButton.setEnabled(isValid());
    }

    @Override
    public final void refresh() {
        super.refresh();
        updateUi();
    }

    @Override
    protected void onRefresh() {
    }
}
