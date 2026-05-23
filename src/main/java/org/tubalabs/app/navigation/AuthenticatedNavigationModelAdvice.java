package org.tubalabs.app.navigation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.NoSuchElementException;
import java.util.UUID;

@ControllerAdvice
@RequiredArgsConstructor
public class AuthenticatedNavigationModelAdvice {

    private static final String DEFAULT_PROFILE_LINK_TEXT = "Profile";

    private final @NonNull CurrentUserIdResolver currentUserIdResolver;
    private final @NonNull UserProfileService userProfileService;

    @ModelAttribute("currentUserDisplayName")
    public String currentUserDisplayName() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return DEFAULT_PROFILE_LINK_TEXT;
        }

        try {
            final UUID userId = currentUserIdResolver.requireUserId(authentication);
            final UserProfileDbo profile = userProfileService.getProfile(userId);
            return displayNameOrDefault(profile.displayName());
        } catch (NoSuchElementException exception) {
            return DEFAULT_PROFILE_LINK_TEXT;
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String displayNameOrDefault(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return DEFAULT_PROFILE_LINK_TEXT;
        }
        return displayName;
    }
}
