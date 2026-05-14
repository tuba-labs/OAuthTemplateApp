package org.tubalabs.app.users.db.profile;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.security.identity.ExternalIdentity;

import java.util.UUID;

@Component
public class UserProfileMapper {

    public UserProfileDbo toProfile(
            @NonNull UUID userId,
            ExternalIdentity externalIdentity) {

        return UserProfileDbo.builder()
                .userId(userId)
                .displayName(externalIdentity.displayName())
                .givenName(externalIdentity.givenName())
                .familyName(externalIdentity.familyName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();
    }

}