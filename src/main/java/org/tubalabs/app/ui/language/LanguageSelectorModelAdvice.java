package org.tubalabs.app.ui.language;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserRequestContext;
import org.tubalabs.app.users.settings.UserLanguage;

@ControllerAdvice(basePackages = "org.tubalabs.app.ui")
@RequiredArgsConstructor
public class LanguageSelectorModelAdvice {

    private final @NonNull CurrentUserRequestContext currentUserRequestContext;
    private final @NonNull LanguageSelectorPageModel languageSelectorPageModel;

    @ModelAttribute
    public void addLanguageSelector(@NonNull Model model,
                                    @NonNull HttpServletRequest request,
                                    Authentication authentication) {
        final boolean authenticated = currentUserRequestContext.authenticated(authentication);
        languageSelectorPageModel.addLanguageSelector(
                model,
                currentLanguage(request, authentication, authenticated),
                authenticated ? LanguageSelectorPersistence.DATABASE : LanguageSelectorPersistence.LOCAL_STORAGE);
    }

    private UserLanguage currentLanguage(@NonNull HttpServletRequest request,
                                         Authentication authentication,
                                         boolean authenticated) {
        if (authenticated) {
            final CurrentUser currentUser = currentUserRequestContext.currentUser(request, authentication);
            return UserLanguage.fromTagOrDefault(currentUser.languageTag());
        }
        return UserLanguage.fromTagOrDefault(LocaleContextHolder.getLocale().toLanguageTag());
    }
}
