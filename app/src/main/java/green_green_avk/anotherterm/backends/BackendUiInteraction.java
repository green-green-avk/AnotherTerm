package green_green_avk.anotherterm.backends;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public interface BackendUiInteraction {
    interface CustomField {
        @NonNull
        CustomFieldOpts getOpts();
    }

    interface CustomValueField<T> extends CustomField {
        @NonNull
        T getValue();
    }

    interface CustomPrompt {
        void submit();

        @NonNull
        List<CustomField> getFields();
    }

    interface CustomFieldAction {
        CustomFieldAction label = new CustomFieldAction() {
        };
    }

    interface CustomButtonAction extends CustomFieldAction {
        void onClick(@NonNull CustomPrompt prompt);
    }

    interface CustomValueAction<T> extends CustomFieldAction {
        @NonNull
        T onInit();

        void onSubmit(@NonNull CustomPrompt prompt, @NonNull T v);
    }

    interface CustomCheckboxAction extends CustomValueAction<Boolean> {
        @Override
        @NonNull
        default Boolean onInit() {
            return false;
        }
    }

    interface CustomTextInputAction extends CustomValueAction<CharSequence> {
        enum Type {NORMAL, PASSWORD}

        @NonNull
        default Type getType() {
            return Type.NORMAL;
        }

        @Override
        @NonNull
        default CharSequence onInit() {
            return "";
        }
    }

    final class CustomFieldOpts {
        @NonNull
        public final CharSequence label;
        @NonNull
        public final CustomFieldAction action;

        public CustomFieldOpts(@NonNull final CharSequence label,
                               @NonNull final CustomFieldAction action) {
            this.label = label;
            this.action = action;
        }
    }

    /**
     * Erases sensitive data returned by {@code prompt*()} calls.
     *
     * @param v data to erase
     */
    void erase(@NonNull CharSequence v);

    boolean promptFields(@NonNull final List<CustomFieldOpts> fieldsOpts)
            throws InterruptedException;

    @Nullable
    CharSequence promptPassword(@NonNull CharSequence message,
                                @NonNull List<CustomFieldOpts> extras)
            throws InterruptedException;

    @Nullable
    default CharSequence promptPassword(@NonNull final CharSequence message)
            throws InterruptedException {
        return promptPassword(message, Collections.emptyList());
    }

    boolean promptYesNo(@NonNull CharSequence message) throws InterruptedException;

    void showMessage(@NonNull CharSequence message);

    void showToast(@NonNull CharSequence message);

    @Nullable
    byte[] promptContent(@NonNull CharSequence message, @NonNull String mimeType, long sizeLimit)
            throws InterruptedException, IOException;

    boolean promptPermissions(@NonNull String[] perms) throws InterruptedException;
}
