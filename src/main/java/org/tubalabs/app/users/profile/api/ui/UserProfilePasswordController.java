package org.tubalabs.app.users.profile.api.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserProfilePasswordController {

    private static final String PASSWORD_FORM_ATTRIBUTE = "passwordForm";
    private static final String PASSWORD_SAVED_ATTRIBUTE = "passwordSaved";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";
    private static final String PROFILE_PASSWORD_VIEW = "org/tubalabs/app/users/profile/profile-password";
    private static final String PROFILE_REDIRECT = "redirect:/profile";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final LocalUserService localUserService;
    private final ProfileSetupSession profileSetupSession;
    private final ProfilePageModel profilePageModel;

    @GetMapping("/profile/password")
    public String password(@NonNull Authentication authentication,
                           @NonNull Model model,
                           @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final Optional<String> loginName = localUserService.loginName(userId);
        if (loginName.isEmpty() || profileSetupSession.isProfileSetupRequired(request)) {
            throw new AccessDeniedException("An established local account is required to change password");
        }

        if (!model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)) {
            model.addAttribute(PASSWORD_FORM_ATTRIBUTE, new UserPasswordChange(null, null, null));
        }
        profilePageModel.addMenuAttributes(model, request, loginName);
        return PROFILE_PASSWORD_VIEW;
    }

    @PostMapping("/profile/password")
    public String changePassword(@NonNull Authentication authentication,
                                 @Valid @ModelAttribute(PASSWORD_FORM_ATTRIBUTE) @NonNull UserPasswordChange passwordForm,
                                 @NonNull BindingResult bindingResult,
                                 @NonNull Model model,
                                 @NonNull RedirectAttributes redirectAttributes,
                                 @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (localUserService.loginName(userId).isEmpty() || profileSetupSession.isProfileSetupRequired(request)) {
            throw new AccessDeniedException("An established local account is required to change password");
        }

        if (!Objects.equals(passwordForm.newPassword(), passwordForm.newPasswordConfirmation())) {
            bindingResult.rejectValue("newPasswordConfirmation", "passwordMismatch", PASSWORD_MISMATCH_MESSAGE);
        }
        if (bindingResult.hasErrors()) {
            return passwordViewWithErrors(model, userId, passwordForm, request);
        }

        try {
            localUserService.changePassword(userId, passwordForm.currentPassword(), passwordForm.newPassword());
        } catch (BadCredentialsException exception) {
            bindingResult.rejectValue("currentPassword", "badCredentials", exception.getMessage());
            return passwordViewWithErrors(model, userId, passwordForm, request);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("newPassword", "passwordRejected", exception.getMessage());
            return passwordViewWithErrors(model, userId, passwordForm, request);
        }

        redirectAttributes.addFlashAttribute(PASSWORD_SAVED_ATTRIBUTE, true);
        return PROFILE_REDIRECT;
    }

    private String passwordViewWithErrors(@NonNull Model model,
                                          @NonNull UUID userId,
                                          @NonNull UserPasswordChange passwordForm,
                                          @NonNull HttpServletRequest request) {
        model.addAttribute(PASSWORD_FORM_ATTRIBUTE, passwordForm);
        profilePageModel.addMenuAttributes(model, userId, request);
        return PROFILE_PASSWORD_VIEW;
    }
}
