package org.tubalabs.app.users.identity.password.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.web.util.UriComponentsBuilder;
import org.tubalabs.app.security.remember.RememberLoginProperties;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordSecurityCustomizerTest {

    private static final String EMAIL = "person@example.com";
    private static final String LOCAL_LOGIN_PATH = "/login/local";
    private static final String EMAIL_PARAMETER = "email";
    private static final String ERROR_PARAMETER = "error";

    private final LocalUserService localUserService = Mockito.mock(LocalUserService.class);
    private final RememberMeServices rememberMeServices = Mockito.mock(RememberMeServices.class);
    private final RememberLoginProperties rememberLoginProperties =
            new RememberLoginProperties(Duration.ofHours(1), Duration.ofDays(7), "test-key");
    private final ProfileSetupRequirementService profileSetupRequirementService =
            Mockito.mock(ProfileSetupRequirementService.class);
    private final CurrentUserSession currentUserSession = Mockito.mock(CurrentUserSession.class);
    private final PasswordSecurityCustomizer customizer = new PasswordSecurityCustomizer(
            localUserService,
            rememberMeServices,
            rememberLoginProperties,
            profileSetupRequirementService,
            currentUserSession);

    @Test
    void redirectsBackToLocalLoginWithSubmittedEmailOnFailure() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(EMAIL_PARAMETER, EMAIL);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        customizer.passwordAuthenticationFailureHandler()
                .onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

        final String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).startsWith(LOCAL_LOGIN_PATH);
        assertThat(queryParameter(redirectUrl, ERROR_PARAMETER)).isEqualTo("true");
        assertThat(queryParameter(redirectUrl, EMAIL_PARAMETER)).isEqualTo(EMAIL);
    }

    private String queryParameter(String redirectUrl, String parameterName) {
        return UriComponentsBuilder.fromUriString(redirectUrl)
                .build()
                .getQueryParams()
                .getFirst(parameterName);
    }
}
