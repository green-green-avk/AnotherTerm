package green_green_avk.anotherterm.backends;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import green_green_avk.anotherterm.utils.Password;

public interface BackendUiPasswordStorage {
    /**
     * Loads a password from the storage.
     *
     * @param target unique id for keyring
     * @return the password (a copy) or {@code null} if not exists
     */
    @Nullable
    Password getPassword(@NonNull String target);

    /**
     * Saves a password to the storage.
     *
     * @param target unique id for keyring etc.
     * @param pwd    password to save (will be copied)
     */
    void putPassword(@NonNull String target, @NonNull CharSequence pwd);

    /**
     * Erases a password from the storage.
     *
     * @param target unique id for keyring
     * @param pwd    if not {@code null}, remove only if the stored password matches
     */
    void erasePassword(@NonNull String target, @Nullable CharSequence pwd);
}
