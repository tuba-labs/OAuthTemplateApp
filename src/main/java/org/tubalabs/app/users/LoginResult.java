package org.tubalabs.app.users;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record LoginResult(
        @NonNull UUID identityId,
        @NonNull UUID userId,
        @NonNull String providerId,
        @NonNull String subject,
        boolean newUser) {
}
