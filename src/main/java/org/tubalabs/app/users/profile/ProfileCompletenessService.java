package org.tubalabs.app.users.profile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.validation.DisplayNameValidator;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileCompletenessService {

    private final @NonNull UserProfileService userProfileService;
    private final @NonNull DisplayNameValidator displayNameValidator;

    public boolean isProfileComplete(@NonNull UUID userId) {
        try {
            final UserProfileDbo profile = userProfileService.getProfile(userId);
            return hasValidDisplayName(profile.displayName());
        } catch (NoSuchElementException exception) {
            return false;
        }
    }

    private boolean hasValidDisplayName(String displayName) {
        return displayName != null
                && !displayName.isBlank()
                && displayNameValidator.isValid(displayName, null);
    }
}
