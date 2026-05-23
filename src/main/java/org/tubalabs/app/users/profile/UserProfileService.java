package org.tubalabs.app.users.profile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.db.UserProfileRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileDbo createInitialProfile(@NonNull UUID userId,
                                               @NonNull ExternalIdentity externalIdentity) {
        return createInitialProfile(
                userId,
                externalIdentity.displayName(),
                externalIdentity.email(),
                externalIdentity.avatarUrl());
    }

    public UserProfileDbo createInitialProfile(@NonNull UUID userId,
                                               @NonNull String displayName) {
        return createInitialProfile(userId, displayName, null, null);
    }

    public UserProfileDbo createInitialProfile(@NonNull UUID userId,
                                               String displayName,
                                               String email,
                                               String pictureUrl) {
        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(userId)
                .displayName(displayName)
                .email(email)
                .pictureUrl(pictureUrl)
                .build();
        return userProfileRepository.insert(profile);
    }
}
