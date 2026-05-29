package org.tubalabs.app.ui.login;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.tubalabs.app.localization.LocalizationService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LoginControllerTest {

    private static final String EMAIL = "person@example.com";
    private static final String LOCAL_LOGIN_VIEW = "ui/login/login-local";
    private static final String LOGIN_EMAIL_ATTRIBUTE = "loginEmail";
    private static final String HAS_LOGIN_ERROR_ATTRIBUTE = "hasLoginError";

    private final ClientRegistrationRepository clientRegistrationRepository = registrationId -> null;
    private final LocalizationService localizationService = Mockito.mock(LocalizationService.class);
    private final LoginController controller = new LoginController(clientRegistrationRepository, localizationService);

    @Test
    void keepsEmailOnLocalLoginError() {
        final Model model = new ExtendedModelMap();

        final String view = controller.localLogin(
                null,
                Optional.of("true"),
                Optional.of(EMAIL),
                Optional.empty(),
                model);

        assertThat(view).isEqualTo(LOCAL_LOGIN_VIEW);
        assertThat(model.getAttribute(HAS_LOGIN_ERROR_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOGIN_EMAIL_ATTRIBUTE)).isEqualTo(EMAIL);
    }
}
