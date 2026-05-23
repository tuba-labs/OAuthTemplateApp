package org.tubalabs.app.users.profile;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.profile.api.ui.UserProfileUpdate;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.db.UserProfileRepository;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Validated
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
                                               @NonNull String displayName,
                                               @NonNull String email) {
        return createInitialProfile(userId, displayName, email, null);
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

    public UserProfileDbo getProfile(@NonNull UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found: " + userId));
    }

    @Transactional
    public UserProfileDbo updateProfile(@NonNull UUID userId, @Valid @NonNull UserProfileUpdate update) {
        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(userId)
                .displayName(update.displayName())
                .pictureUrl(update.pictureUrl())
                .build();
        return userProfileRepository.update(profile);
    }
}
