package org.tubalabs.app.ui.language;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferences;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class LanguagePreferenceController {

    private static final String HOME_PATH = "/";

    private final @NonNull CurrentUserIdResolver currentUserIdResolver;
    private final @NonNull GlobalUserPreferences globalUserPreferences;
    private final @NonNull CurrentUserSession currentUserSession;
    private final @NonNull ProfileSetupRequirementService profileSetupRequirementService;

    @PostMapping("/language")
    public String updateLanguage(@NonNull Authentication authentication,
                                 @RequestParam @NonNull String languageTag,
                                 @RequestParam @NonNull Optional<String> returnTo,
                                 @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        UserLanguage.fromTag(languageTag).ifPresent(language -> {
            globalUserPreferences.updateLanguage(userId, language);
            final boolean profileSetupRequired =
                    profileSetupRequirementService.isSetupRequiredForSession(request, userId);
            currentUserSession.refresh(request, userId, profileSetupRequired);
        });
        return "redirect:" + safeReturnPath(returnTo.orElse(HOME_PATH));
    }

    private String safeReturnPath(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return HOME_PATH;
        }
        if (!returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return HOME_PATH;
        }
        return returnTo;
    }
}
