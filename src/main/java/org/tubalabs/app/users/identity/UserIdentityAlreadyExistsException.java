package org.tubalabs.app.users.identity;

import lombok.NonNull;

/**
 * Handles exceptions when a user identity already exists for a given provider and subject. Typically this would be a
 * race condition, where multiple requests attempt to create the same user identity simultaneously.
 */
public class UserIdentityAlreadyExistsException extends RuntimeException {

    private final String providerId;
    private final String subject;

    public UserIdentityAlreadyExistsException(@NonNull String providerId, @NonNull String subject) {
        super("User identity already exists for providerId=%s, subject=%s".formatted(providerId, subject));
        this.providerId = providerId;
        this.subject = subject;
    }

    public String providerId() {
        return providerId;
    }

    public String subject() {
        return subject;
    }
}
