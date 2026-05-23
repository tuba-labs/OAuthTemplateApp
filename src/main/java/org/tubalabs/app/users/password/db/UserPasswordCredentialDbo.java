package org.tubalabs.app.users.password.db;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record UserPasswordCredentialDbo(
        @NonNull UUID userId,
        @NonNull String email,
        @NonNull String passwordHash) {
}
