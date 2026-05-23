package org.tubalabs.app.users.profile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.tubalabs.app.users.CurrentUserIdResolver;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileSetupInterceptor implements HandlerInterceptor {

    private static final String PROFILE_PATH = "/profile";
    private static final List<String> ALLOWED_PREFIXES = List.of(
            "/css/",
            "/js/",
            "/actuator/",
            "/api/",
            "/oauth2/",
            "/login",
            "/register",
            "/error");

    private final @NonNull ProfileSetupSession profileSetupSession;
    private final @NonNull ProfileSetupRequirementService profileSetupRequirementService;
    private final @NonNull CurrentUserIdResolver currentUserIdResolver;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        if (isAllowed(request) || !isSetupRequired(request)) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + PROFILE_PATH);
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
        final String path = requestPath(request);
        return PROFILE_PATH.equals(path)
                || path.startsWith(PROFILE_PATH + "/")
                || "/logout".equals(path)
                || ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
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
