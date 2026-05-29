package org.tubalabs.app.users.current;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferenceValues;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferences;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserLoader {

    private final @NonNull UserProfileService userProfileService;
    private final @NonNull LocalUserService localUserService;
    private final @NonNull UserLoginTypeService userLoginTypeService;
    private final @NonNull GlobalUserPreferences globalUserPreferences;

    public CurrentUser load(@NonNull UUID userId, boolean profileSetupRequired) {
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        final GlobalUserPreferenceValues preferences = globalUserPreferences.preferences(userId);
        return new CurrentUser(
                userId,
                profile.displayName(),
                profile.pictureUrl(),
                profileSetupRequired,
                localUserService.loginName(userId).orElse(null),
                userLoginTypeService.canLinkLocalLoginType(userId),
                preferences.language().tag(),
                preferences.disableBackgroundAnimation());
    }
}
