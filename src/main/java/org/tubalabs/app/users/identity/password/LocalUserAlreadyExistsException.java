package org.tubalabs.app.users.identity.password;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class LocalUserAlreadyExistsException extends IllegalArgumentException {

    private final String email;

    public LocalUserAlreadyExistsException(@NonNull String email) {
        super("A local user already exists for email: " + email);
        this.email = email;
    }

    public LocalUserAlreadyExistsException(@NonNull String email, @NonNull Throwable cause) {
        super("A local user already exists for email: " + email, cause);
        this.email = email;
    }
}
