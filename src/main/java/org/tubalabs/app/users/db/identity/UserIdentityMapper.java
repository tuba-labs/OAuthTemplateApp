package org.tubalabs.app.users.db.identity;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.security.identity.ExternalIdentity;

import java.util.UUID;

@Component
public class UserIdentityMapper {

    public UserIdentityDbo newIdentity(
            @NonNull UUID identityId,
            @NonNull UUID userId,
            @NonNull ExternalIdentity externalIdentity) {

        return UserIdentityDbo.builder()
                .id(identityId)
                .userId(userId)
                .providerId(externalIdentity.providerId())
                .subject(externalIdentity.subject())
                .displayName(externalIdentity.displayName())
                .givenName(externalIdentity.givenName())
                .familyName(externalIdentity.familyName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();
    }

    public UserIdentityDbo updateIdentity(
            @NonNull UserIdentityDbo existingIdentity,
            @NonNull ExternalIdentity externalIdentity) {

        return existingIdentity.toBuilder()
                .displayName(externalIdentity.displayName())
                .givenName(externalIdentity.givenName())
                .familyName(externalIdentity.familyName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();
    }
}