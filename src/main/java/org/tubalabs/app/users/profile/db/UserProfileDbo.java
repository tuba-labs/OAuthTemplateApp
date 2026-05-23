package org.tubalabs.app.users.profile.db;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record UserProfileDbo(
        @NonNull UUID userId,

        String displayName,
        String email,
        String pictureUrl) {
}
