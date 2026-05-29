package org.tubalabs.app.users.current;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserRequestContext {

    private final @NonNull CurrentUserIdResolver currentUserIdResolver;
    private final @NonNull CurrentUserSession currentUserSession;
    private final @NonNull ProfileSetupRequirementService profileSetupRequirementService;

    public boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public CurrentUser currentUser(@NonNull HttpServletRequest request, @NonNull Authentication authentication) {
        if (!authenticated(authentication)) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        return currentUserSession.currentUser(request)
                .orElseGet(() -> loadCurrentUser(request, authentication));
    }

    private CurrentUser loadCurrentUser(@NonNull HttpServletRequest request, @NonNull Authentication authentication) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final boolean profileSetupRequired = profileSetupRequirementService.isSetupRequiredForSession(request, userId);
        return currentUserSession.refresh(request, userId, profileSetupRequired);
    }
}
