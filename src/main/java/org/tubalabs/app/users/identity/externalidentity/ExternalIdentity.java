package org.tubalabs.app.users.identity.externalidentity;

import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
public record ExternalIdentity(

        @NonNull String providerId,
        @NonNull String subject,
        @NonNull String displayName,

        String email,

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
