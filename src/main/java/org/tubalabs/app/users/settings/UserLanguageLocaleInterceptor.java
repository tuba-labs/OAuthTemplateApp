package org.tubalabs.app.users.settings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserLanguageLocaleInterceptor implements HandlerInterceptor {

    private static final String LANGUAGE_PARAMETER = "language";

    private final @NonNull CurrentUserSession currentUserSession;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        final Optional<CurrentUser> currentUser = currentUserSession.currentUser(request);
        if (currentUser.isPresent()) {
            UserLanguageLocaleResolver.setRequestLocale(request, currentUser.get().locale());
            return true;
        }
        selectedLanguage(request)
                .ifPresent(language -> UserLanguageLocaleResolver.setRequestLocale(request, language.locale()));
        return true;
    }

    private Optional<UserLanguage> selectedLanguage(@NonNull HttpServletRequest request) {
        return UserLanguage.fromTag(request.getParameter(LANGUAGE_PARAMETER));
    }
}
