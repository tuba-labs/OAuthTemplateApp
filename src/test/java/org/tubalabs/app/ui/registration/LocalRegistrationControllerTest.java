package org.tubalabs.app.ui.registration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.ui.registration.dtos.LocalRegistrationFormDto;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.identity.password.security.LocalSessionAuthentication;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRegistrationControllerTest {

    private static final String EMAIL = "person@example.com";
    private static final String PASSWORD = "Password123";
    private static final String FORM_OBJECT_NAME = "localRegistrationFormDto";
    private static final String EMAIL_FIELD = "email";
    private static final String INVALID_EMAIL_CODE = "invalidEmail";
    private static final String INVALID_EMAIL_MESSAGE = "Choose a valid email address";
    private static final String REGISTRATION_ERROR_ATTRIBUTE = "registrationError";
    private static final String LANGUAGE_PARAMETER = "language";
    private static final String NORWEGIAN_LANGUAGE_TAG = "nb";
    private static final String UNSUPPORTED_LANGUAGE_TAG = "fr";
    private static final String REGISTER_REDIRECT = "redirect:/register";

    private final LocalUserService localUserService = Mockito.mock(LocalUserService.class);
    private final LocalSessionAuthentication localSessionAuthentication = Mockito.mock(LocalSessionAuthentication.class);
    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final CurrentUserSession currentUserSession = Mockito.mock(CurrentUserSession.class);
    private final LocalizationService localizationService = Mockito.mock(LocalizationService.class);
    private final LocalRegistrationController controller = new LocalRegistrationController(
            localUserService,
            localSessionAuthentication,
            profileSetupSession,
            currentUserSession,
            localizationService);

    @Test
    void preservesSelectedLanguageWhenRedirectingBackWithValidationError() {
        final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        final String view = controller.register(
                validForm(),
                invalidBindingResult(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                redirectAttributes,
                Optional.of(NORWEGIAN_LANGUAGE_TAG));

        assertThat(view).isEqualTo(REGISTER_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(REGISTRATION_ERROR_ATTRIBUTE))
                .isEqualTo(INVALID_EMAIL_MESSAGE);
        assertThat(redirectAttributes.asMap().get(LANGUAGE_PARAMETER)).isEqualTo(NORWEGIAN_LANGUAGE_TAG);
    }

    @Test
    void ignoresUnsupportedLanguageWhenRedirectingBackWithValidationError() {
        final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        final String view = controller.register(
                validForm(),
                invalidBindingResult(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                redirectAttributes,
                Optional.of(UNSUPPORTED_LANGUAGE_TAG));

        assertThat(view).isEqualTo(REGISTER_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(REGISTRATION_ERROR_ATTRIBUTE))
                .isEqualTo(INVALID_EMAIL_MESSAGE);
        assertThat(redirectAttributes.asMap()).doesNotContainKey(LANGUAGE_PARAMETER);
    }

    private LocalRegistrationFormDto validForm() {
        return new LocalRegistrationFormDto(EMAIL, PASSWORD, PASSWORD);
    }

    private BindingResult invalidBindingResult() {
        final LocalRegistrationFormDto form = validForm();
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, FORM_OBJECT_NAME);
        bindingResult.rejectValue(EMAIL_FIELD, INVALID_EMAIL_CODE, INVALID_EMAIL_MESSAGE);
        return bindingResult;
    }
}
