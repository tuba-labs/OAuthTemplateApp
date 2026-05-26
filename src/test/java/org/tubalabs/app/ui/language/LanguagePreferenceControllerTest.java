package org.tubalabs.app.ui.language;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.settings.UserLanguage;
import org.tubalabs.app.users.settings.UserSettingsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LanguagePreferenceControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String NORWEGIAN_LANGUAGE_TAG = "nb";
    private static final String UNSUPPORTED_LANGUAGE_TAG = "fr";
    private static final String PROFILE_RETURN_PATH = "/profile?tab=settings";
    private static final String HOME_REDIRECT = "redirect:/";

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final UserSettingsService userSettingsService = Mockito.mock(UserSettingsService.class);
    private final CurrentUserSession currentUserSession = Mockito.mock(CurrentUserSession.class);
    private final ProfileSetupRequirementService profileSetupRequirementService =
            Mockito.mock(ProfileSetupRequirementService.class);
    private final LanguagePreferenceController controller =
            new LanguagePreferenceController(
                    currentUserIdResolver,
                    userSettingsService,
                    currentUserSession,
                    profileSetupRequirementService);
    private final Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());

    @Test
    void updatesLanguageAndRefreshesCurrentUser() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(profileSetupRequirementService.isSetupRequiredForSession(request, USER_ID)).thenReturn(true);

        final String view = controller.updateLanguage(
                authentication, NORWEGIAN_LANGUAGE_TAG, Optional.of(PROFILE_RETURN_PATH), request);

        assertThat(view).isEqualTo("redirect:" + PROFILE_RETURN_PATH);
        verify(userSettingsService).updateLanguage(USER_ID, UserLanguage.NORWEGIAN);
        verify(currentUserSession).refresh(request, USER_ID, true);
    }

    @Test
    void ignoresUnsupportedLanguage() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);

        final String view = controller.updateLanguage(
                authentication, UNSUPPORTED_LANGUAGE_TAG, Optional.of(PROFILE_RETURN_PATH), request);

        assertThat(view).isEqualTo("redirect:" + PROFILE_RETURN_PATH);
        verify(userSettingsService, never()).updateLanguage(Mockito.any(), Mockito.any());
        verify(currentUserSession, never()).refresh(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void fallsBackToHomeForUnsafeRedirect() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);

        final String view = controller.updateLanguage(
                authentication, NORWEGIAN_LANGUAGE_TAG, Optional.of("https://example.com"), request);

        assertThat(view).isEqualTo(HOME_REDIRECT);
    }
}
