package org.tubalabs.app.ui.profile.changepassword;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
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
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.ui.profile.changepassword.dtos.UserPasswordChangeDto;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePage;

import java.util.Objects;
import java.util.UUID;

@Controller
public class UserProfilePasswordController extends AbstractNavigationController {

    private static final String PASSWORD_FORM_ATTRIBUTE = "passwordForm";
    private static final String PASSWORD_SAVED_ATTRIBUTE = "passwordSaved";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final LocalUserService localUserService;
    private final ProfileSetupSession profileSetupSession;
    private final LocalizationService localizationService;
    private final CurrentUserSession currentUserSession;

    public UserProfilePasswordController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                                         @NonNull CurrentUserSession currentUserSession,
                                         @NonNull ProfileSetupRequirementService profileSetupRequirementService,
                                         @NonNull NavigationPageModel navigationPageModel,
                                         @NonNull LocalUserService localUserService,
                                         @NonNull ProfileSetupSession profileSetupSession,
                                         @NonNull LocalizationService localizationService) {
        super(currentUserIdResolver, currentUserSession, profileSetupRequirementService, navigationPageModel);
        this.currentUserIdResolver = currentUserIdResolver;
        this.localUserService = localUserService;
        this.profileSetupSession = profileSetupSession;
        this.localizationService = localizationService;
        this.currentUserSession = currentUserSession;
    }

    @GetMapping(ProfileChangePasswordPage.RELATIVE_URL)
    public String password(@NonNull Authentication authentication,
                           @NonNull Model model,
                           @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final CurrentUser currentUser = currentUser(userId, request);
        if (!currentUser.passwordChangeAvailable()) {
            throw new AccessDeniedException(
                    localizationService.message("profile.change-password.access.local-required"));
        }

        if (!model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)) {
            model.addAttribute(PASSWORD_FORM_ATTRIBUTE, new UserPasswordChangeDto(null, null, null));
        }
        return ProfileChangePasswordPage.VIEW;
    }

    @PostMapping(ProfileChangePasswordPage.RELATIVE_URL)
    public String changePassword(@NonNull Authentication authentication,
                                 @Valid @ModelAttribute(PASSWORD_FORM_ATTRIBUTE) @NonNull UserPasswordChangeDto passwordForm,
                                 @NonNull BindingResult bindingResult,
                                 @NonNull Model model,
                                 @NonNull RedirectAttributes redirectAttributes,
                                 @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!currentUser(userId, request).passwordChangeAvailable()) {
            throw new AccessDeniedException(
                    localizationService.message("profile.change-password.access.local-required"));
        }

        if (!Objects.equals(passwordForm.newPassword(), passwordForm.newPasswordConfirmation())) {
            bindingResult.rejectValue(
                    "newPasswordConfirmation",
                    "passwordMismatch",
                    localizationService.message("profile.change-password.error.password-mismatch"));
        }
        if (bindingResult.hasErrors()) {
            return passwordViewWithErrors(model, passwordForm);
        }

        try {
            localUserService.changePassword(userId, passwordForm.currentPassword(), passwordForm.newPassword());
        } catch (BadCredentialsException exception) {
            bindingResult.rejectValue(
                    "currentPassword",
                    "badCredentials",
                    localizationService.message("profile.change-password.error.bad-current-password"));
            return passwordViewWithErrors(model, passwordForm);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("newPassword", "passwordRejected", exception.getMessage());
            return passwordViewWithErrors(model, passwordForm);
        }

        redirectAttributes.addFlashAttribute(PASSWORD_SAVED_ATTRIBUTE, true);
        return ProfilePage.REDIRECT;
    }

    private String passwordViewWithErrors(@NonNull Model model,
                                          @NonNull UserPasswordChangeDto passwordForm) {
        model.addAttribute(PASSWORD_FORM_ATTRIBUTE, passwordForm);
        return ProfileChangePasswordPage.VIEW;
    }

    private CurrentUser currentUser(@NonNull UUID userId, @NonNull HttpServletRequest request) {
        return currentUserSession.currentUser(request)
                .orElseGet(() -> currentUserSession.refresh(
                        request, userId, profileSetupSession.isProfileSetupRequired(request)));
    }
}
