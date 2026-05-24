package org.tubalabs.app.users.identity.password.db;

import lombok.NonNull;

import java.util.UUID;

public class UserPasswordCredentialAlreadyExistsException extends RuntimeException {

    private final UUID userId;
    private final String email;

    public UserPasswordCredentialAlreadyExistsException(@NonNull UUID userId,
                                                        @NonNull String email,
                                                        @NonNull Throwable cause) {
        super("User password credential already exists for userId=%s, email=%s".formatted(userId, email), cause);
        this.userId = userId;
        this.email = email;
    }

    public UUID userId() {
        return userId;
    }

    public String email() {
        return email;
    }
}
