package org.tubalabs.app.users.password.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.users.password.CreateResult;
import org.tubalabs.app.users.password.LocalUserService;
import org.tubalabs.app.users.password.LocalUserRegistration;
import org.tubalabs.app.users.password.security.LocalSessionAuthentication;
import org.tubalabs.app.users.password.validation.SafePassword;
import org.tubalabs.app.users.profile.ProfileSetupSession;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class LocalRegistrationController {

    private static final String REGISTRATION_ERROR_ATTRIBUTE = "registrationError";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";
    private static final String REGISTER_VIEW = "org/tubalabs/app/users/password/api/register";
    private static final String REGISTER_REDIRECT = "redirect:/register";
    private static final String PROFILE_REDIRECT = "redirect:/profile";

    private final LocalUserService localUserService;
    private final LocalSessionAuthentication localSessionAuthentication;
    private final ProfileSetupSession profileSetupSession;

    @GetMapping("/register")
    public String registerForm() {
        return REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute LocalRegistrationForm form,
                           BindingResult bindingResult,
                           @NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            addRegistrationError(redirectAttributes, bindingResult.getAllErrors().get(0).getDefaultMessage());
            return REGISTER_REDIRECT;
        }
        if (!Objects.equals(form.password(), form.passwordConfirmation())) {
            addRegistrationError(redirectAttributes, PASSWORD_MISMATCH_MESSAGE);
            return REGISTER_REDIRECT;
        }

        try {
            final LocalUserRegistration registration = new LocalUserRegistration(form.email(), form.password(), null);
            final CreateResult createResult = localUserService.register(registration);
            if (createResult.vetoed()) {
                addRegistrationError(redirectAttributes, createResult.firstVeto().englishReason());
                return REGISTER_REDIRECT;
            }
            localUserService.login(form.email(), clientIp(request), userAgent(request));
            localSessionAuthentication.authenticate(form.email(), request, response);
            profileSetupSession.requireProfileSetup(request);
            return PROFILE_REDIRECT;
        } catch (IllegalArgumentException exception) {
            addRegistrationError(redirectAttributes, exception.getMessage());
            return REGISTER_REDIRECT;
        }
    }

    private void addRegistrationError(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute(REGISTRATION_ERROR_ATTRIBUTE, message);
    }

    private String clientIp(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }

    public record LocalRegistrationForm(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 320, message = "Email must be 320 characters or fewer")
            String email,
            @NotBlank(message = "Password is required")
            @SafePassword
            String password,
            @NotBlank(message = "Password confirmation is required")
            String passwordConfirmation) {
    }
}
