package org.tubalabs.app.ui.language;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.Optional;

@ControllerAdvice(basePackages = "org.tubalabs.app.ui")
@RequiredArgsConstructor
public class LanguageSelectorModelAdvice {

    private final @NonNull CurrentUserSession currentUserSession;
    private final @NonNull LanguageSelectorPageModel languageSelectorPageModel;

    @ModelAttribute
    public void addLanguageSelector(@NonNull Model model,
                                    @NonNull HttpServletRequest request,
                                    Authentication authentication) {
        final boolean authenticated = authenticated(authentication);
        languageSelectorPageModel.addLanguageSelector(
                model,
                currentLanguage(request, authenticated),
                authenticated ? LanguageSelectorPersistence.DATABASE : LanguageSelectorPersistence.LOCAL_STORAGE);
    }

    private UserLanguage currentLanguage(@NonNull HttpServletRequest request, boolean authenticated) {
        if (authenticated) {
            final Optional<CurrentUser> currentUser = currentUserSession.currentUser(request);
            if (currentUser.isPresent()) {
                return UserLanguage.fromTagOrDefault(currentUser.get().languageTag());
            }
        }
        return UserLanguage.fromTagOrDefault(LocaleContextHolder.getLocale().toLanguageTag());
    }

    private boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
