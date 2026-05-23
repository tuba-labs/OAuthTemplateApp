package org.tubalabs.app.users.identity.db;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record UserIdentityDbo(
        @NonNull UUID id,
        @NonNull UUID userId,

        @NonNull String providerId,
        @NonNull String subject,

        String displayName,
        String email,
        String pictureUrl) {
}
