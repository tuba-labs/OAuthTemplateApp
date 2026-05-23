package org.tubalabs.app.users.profile;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.profile.api.ui.UserProfileUpdate;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.db.UserProfileRepository;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.tubalabs.app.users.profile.UserProfileConstraints.DISPLAY_NAME_MAX_LENGTH;
import static org.tubalabs.app.users.profile.UserProfileConstraints.DISPLAY_NAME_MAX_LENGTH_MESSAGE;

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
                externalIdentity.avatarUrl());
    }

    public UserProfileDbo createInitialProfile(@NonNull UUID userId,
                                               @NonNull String displayName) {
        return createInitialProfile(userId, displayName, null);
    }

    public UserProfileDbo createInitialProfile(@NonNull UUID userId,
                                               String displayName,
                                               String pictureUrl) {
        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(userId)
                .displayName(initialDisplayName(displayName))
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
        validateDisplayNameLength(update.displayName());
        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(userId)
                .displayName(update.displayName())
                .pictureUrl(update.pictureUrl())
                .build();
        return userProfileRepository.update(profile);
    }

    private void validateDisplayNameLength(String displayName) {
        if (displayName != null && displayName.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(DISPLAY_NAME_MAX_LENGTH_MESSAGE);
        }
    }

    private String initialDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        final String trimmedDisplayName = displayName.trim();
        if (trimmedDisplayName.isEmpty()) {
            return null;
        }
        if (trimmedDisplayName.length() <= DISPLAY_NAME_MAX_LENGTH) {
            return trimmedDisplayName;
        }
        return trimmedDisplayName.substring(0, DISPLAY_NAME_MAX_LENGTH);
    }
}
