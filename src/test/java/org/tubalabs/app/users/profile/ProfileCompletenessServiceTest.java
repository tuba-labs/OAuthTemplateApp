package org.tubalabs.app.users.profile;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.validation.DisplayNameValidator;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ProfileCompletenessServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String VALID_DISPLAY_NAME = "Person";
    private static final String INVALID_DISPLAY_NAME = "!!";

    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final ProfileCompletenessService profileCompletenessService =
            new ProfileCompletenessService(userProfileService, new DisplayNameValidator());

    @Test
    void incompleteFlagKeepsProfileIncompleteEvenWithValidDisplayName() {
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile(false, VALID_DISPLAY_NAME));

        assertThat(profileCompletenessService.isProfileComplete(USER_ID)).isFalse();
    }

    @Test
    void completeFlagRequiresValidDisplayName() {
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile(true, INVALID_DISPLAY_NAME));

        assertThat(profileCompletenessService.isProfileComplete(USER_ID)).isFalse();
    }

    @Test
    void returnsCompleteWhenFlagAndDisplayNameAreValid() {
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile(true, VALID_DISPLAY_NAME));

        assertThat(profileCompletenessService.isProfileComplete(USER_ID)).isTrue();
    }

    @Test
    void missingProfileIsIncomplete() {
        when(userProfileService.getProfile(USER_ID)).thenThrow(new NoSuchElementException("missing"));

        assertThat(profileCompletenessService.isProfileComplete(USER_ID)).isFalse();
    }

    private UserProfileDbo profile(boolean profileComplete, String displayName) {
        return UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(displayName)
                .profileComplete(profileComplete)
                .build();
    }
}
