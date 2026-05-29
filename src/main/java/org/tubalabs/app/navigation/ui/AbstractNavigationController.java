package org.tubalabs.app.navigation.ui;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserRequestContext;
import org.tubalabs.app.users.preferences.global.ui.GlobalUserPreferencesPageModel;

public abstract class AbstractNavigationController {

    private final @NonNull CurrentUserRequestContext currentUserRequestContext;
    private final @NonNull NavigationPageModel navigationPageModel;
    private final @NonNull GlobalUserPreferencesPageModel globalUserPreferencesPageModel;

    protected AbstractNavigationController(@NonNull CurrentUserRequestContext currentUserRequestContext,
                                           @NonNull NavigationPageModel navigationPageModel,
                                           @NonNull GlobalUserPreferencesPageModel globalUserPreferencesPageModel) {
        this.currentUserRequestContext = currentUserRequestContext;
        this.navigationPageModel = navigationPageModel;
        this.globalUserPreferencesPageModel = globalUserPreferencesPageModel;
    }

    @ModelAttribute
    public void addAuthenticatedNavigation(@NonNull Model model,
                                           @NonNull HttpServletRequest request,
                                           Authentication authentication) {
        if (!currentUserRequestContext.authenticated(authentication)) {
            return;
        }

        final CurrentUser currentUser = currentUser(request, authentication);
        globalUserPreferencesPageModel.addGlobalPreferences(model, currentUser);
        model.addAttribute("authenticatedNavigationMenu",
                navigationPageModel.navigationMenu(currentUser, currentPath(request)));
    }

    protected CurrentUser currentUser(@NonNull HttpServletRequest request, @NonNull Authentication authentication) {
        return currentUserRequestContext.currentUser(request, authentication);
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
