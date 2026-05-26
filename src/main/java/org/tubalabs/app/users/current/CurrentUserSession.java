package org.tubalabs.app.users.current;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.settings.UserLanguageLocaleResolver;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserSession {

    private static final String CURRENT_USER_ATTRIBUTE = CurrentUserSession.class.getName() + ".currentUser";

    private final @NonNull CurrentUserLoader currentUserLoader;

    public Optional<CurrentUser> currentUser(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        final Object value = session.getAttribute(CURRENT_USER_ATTRIBUTE);
        if (value instanceof CurrentUser currentUser) {
            applyLocale(request, currentUser);
            return Optional.of(currentUser);
        }
        return Optional.empty();
    }

    public CurrentUser refresh(@NonNull HttpServletRequest request,
                               @NonNull UUID userId,
                               boolean profileSetupRequired) {
        final CurrentUser currentUser = currentUserLoader.load(userId, profileSetupRequired);
        setCurrentUser(request, currentUser);
        applyLocale(request, currentUser);
        return currentUser;
    }

    private void setCurrentUser(@NonNull HttpServletRequest request, @NonNull CurrentUser currentUser) {
        request.getSession().setAttribute(CURRENT_USER_ATTRIBUTE, currentUser);
    }

    private void applyLocale(@NonNull HttpServletRequest request, @NonNull CurrentUser currentUser) {
        UserLanguageLocaleResolver.setRequestLocale(request, currentUser.locale());
    }
}
