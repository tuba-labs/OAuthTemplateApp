package org.tubalabs.app.users.profile.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.tubalabs.app.security.SecurityAllowedPaths;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileSetupInterceptor implements HandlerInterceptor {

    private final @NonNull ProfileSetupRequirementService profileSetupRequirementService;
    private final @NonNull CurrentUserIdResolver currentUserIdResolver;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        if (isAllowed(request) || !isSetupRequired(request)) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + SecurityAllowedPaths.PROFILE_PATH);
        return false;
    }

    private boolean isSetupRequired(HttpServletRequest request) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        return profileSetupRequirementService.isSetupRequiredForSession(request, userId);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isAllowed(HttpServletRequest request) {
        return SecurityAllowedPaths.isProfileSetupAllowedPath(requestPath(request));
    }

    private String requestPath(HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        final String requestUri = request.getRequestURI();
        if (contextPath == null || contextPath.isBlank()) {
            return requestUri;
        }
        return requestUri.substring(contextPath.length());
    }
}
