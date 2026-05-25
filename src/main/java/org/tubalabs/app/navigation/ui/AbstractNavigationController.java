package org.tubalabs.app.navigation.ui;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.util.UUID;

public abstract class AbstractNavigationController {

    private final @NonNull CurrentUserIdResolver currentUserIdResolver;
    private final @NonNull CurrentUserSession currentUserSession;
    private final @NonNull ProfileSetupRequirementService profileSetupRequirementService;
    private final @NonNull NavigationPageModel navigationPageModel;

    protected AbstractNavigationController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                                           @NonNull CurrentUserSession currentUserSession,
                                           @NonNull ProfileSetupRequirementService profileSetupRequirementService,
                                           @NonNull NavigationPageModel navigationPageModel) {
        this.currentUserIdResolver = currentUserIdResolver;
        this.currentUserSession = currentUserSession;
        this.profileSetupRequirementService = profileSetupRequirementService;
        this.navigationPageModel = navigationPageModel;
    }

    @ModelAttribute
    public void addAuthenticatedNavigation(@NonNull Model model,
                                           @NonNull HttpServletRequest request,
                                           Authentication authentication) {
        if (!authenticated(authentication)) {
            return;
        }

        final CurrentUser currentUser = currentUserSession.currentUser(request)
                .orElseGet(() -> loadCurrentUser(request, authentication));
        model.addAttribute("authenticatedNavigationMenu",
                navigationPageModel.navigationMenu(currentUser, currentPath(request)));
    }

    private CurrentUser loadCurrentUser(@NonNull HttpServletRequest request,
                                        @NonNull Authentication authentication) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final boolean profileSetupRequired = profileSetupRequirementService.isSetupRequiredForSession(request, userId);
        return currentUserSession.refresh(request, userId, profileSetupRequired);
    }

    private boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String currentPath(@NonNull HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "/";
        }

        final String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            final String path = requestUri.substring(contextPath.length());
            return path.isBlank() ? "/" : path;
        }
        return requestUri;
    }
}
