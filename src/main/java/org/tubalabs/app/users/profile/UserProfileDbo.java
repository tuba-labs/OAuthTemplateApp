package org.tubalabs.app.users.profile;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record UserProfileDbo(
        @NonNull UUID userId,

        String displayName,
        String givenName,
        String familyName,
        String email,
        String pictureUrl) {
}
