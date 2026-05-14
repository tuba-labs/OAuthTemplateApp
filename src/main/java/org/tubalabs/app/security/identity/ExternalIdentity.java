package org.tubalabs.app.security.identity;

import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
public record ExternalIdentity(

        @NonNull String providerId,
        @NonNull String subject,
        @NonNull String displayName,

        String email,

        String givenName,
        String familyName,

        String avatarUrl
) {

    public static ExternalIdentity minimal(String providerId, String subject, String displayName) {
        return ExternalIdentity.builder()
                .providerId(providerId)
                .subject(subject)
                .displayName(displayName)
                .build();
    }
}