package org.tubalabs.app.users.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.security.identity.ExternalIdentity;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileDbo createInitialProfile(UUID userId, ExternalIdentity externalIdentity) {
        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(userId)
                .displayName(externalIdentity.displayName())
                .givenName(externalIdentity.givenName())
                .familyName(externalIdentity.familyName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();

        return userProfileRepository.insert(profile);
    }
}
