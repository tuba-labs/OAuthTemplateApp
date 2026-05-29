package org.tubalabs.app.ui.registration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.password.CreateResult;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;
import org.tubalabs.app.users.identity.password.security.LocalSessionAuthentication;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.settings.UserLanguage;
import org.tubalabs.app.ui.registration.dtos.LocalRegistrationFormDto;

import java.util.Objects;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LocalRegistrationController {

    private static final String REGISTRATION_ERROR_ATTRIBUTE = "registrationError";
    private static final String LANGUAGE_PARAMETER = "language";
    private static final String REGISTER_VIEW = "ui/registration/register";
    private static final String REGISTER_REDIRECT = "redirect:/register";
    private static final String PROFILE_REDIRECT = "redirect:/profile";

    private final LocalUserService localUserService;
    private final LocalSessionAuthentication localSessionAuthentication;
    private final ProfileSetupSession profileSetupSession;
    private final CurrentUserSession currentUserSession;
    private final LocalizationService localizationService;

    @GetMapping("/register")
    public String registerForm() {
        return REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute LocalRegistrationFormDto form,
                           BindingResult bindingResult,
                           @NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull RedirectAttributes redirectAttributes,
                           @RequestParam @NonNull Optional<String> language) {

        if (bindingResult.hasErrors()) {
            addRegistrationError(redirectAttributes, bindingResult.getAllErrors().get(0).getDefaultMessage(), language);
            return REGISTER_REDIRECT;
        }
        if (!Objects.equals(form.password(), form.passwordConfirmation())) {
            addRegistrationError(
                    redirectAttributes,
                    localizationService.message("registration.error.password-mismatch"),
                    language);
            return REGISTER_REDIRECT;
        }

        try {
            final LocalUserRegistration registration = new LocalUserRegistration(form.email(), form.password());
            final CreateResult createResult = localUserService.register(registration);
            if (createResult.vetoed()) {
                addRegistrationError(redirectAttributes, createResult.firstVeto().englishReason(), language);
                return REGISTER_REDIRECT;
            }
            localUserService.login(form.email(), clientIp(request), userAgent(request));
            localSessionAuthentication.authenticate(form.email(), request, response);
            profileSetupSession.requireProfileSetup(request);
            currentUserSession.refresh(request, createResult.id(), true);
            return PROFILE_REDIRECT;
        } catch (IllegalArgumentException exception) {
            addRegistrationError(redirectAttributes, exception.getMessage(), language);
            return REGISTER_REDIRECT;
        }
    }

    private void addRegistrationError(RedirectAttributes redirectAttributes,
                                      String message,
                                      Optional<String> language) {
        redirectAttributes.addFlashAttribute(REGISTRATION_ERROR_ATTRIBUTE, message);
        language.flatMap(UserLanguage::fromTag)
                .ifPresent(userLanguage -> redirectAttributes.addAttribute(LANGUAGE_PARAMETER, userLanguage.tag()));
    }

    private String clientIp(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }
}
