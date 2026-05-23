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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.password.LocalUserService;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PASSWORD_FORM_ATTRIBUTE = "passwordForm";
    private static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    private static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    private static final String PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE = "passwordChangeAvailable";
    private static final String PASSWORD_DIALOG_OPEN_ATTRIBUTE = "passwordDialogOpen";
    private static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String PASSWORD_SAVED_ATTRIBUTE = "passwordSaved";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";
    private static final String PROFILE_VIEW = "org/tubalabs/app/users/profile/profile";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final UserProfileService userProfileService;
    private final LocalUserService localUserService;
    private final ProfileSetupSession profileSetupSession;
    private final ProfilePictureStorageService profilePictureStorageService;

    @GetMapping("/profile")
    public String profile(@NonNull Authentication authentication,
                          @NonNull Model model,
                          @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        if (!model.containsAttribute(PROFILE_FORM_ATTRIBUTE)) {
            model.addAttribute(PROFILE_FORM_ATTRIBUTE, profileForm(profile));
        }
        addProfileModelAttributes(model, userId, profile, request);
        return PROFILE_VIEW;
    }

    @PostMapping("/profile")
    public String updateProfile(@NonNull Authentication authentication,
                                @Valid @ModelAttribute(PROFILE_FORM_ATTRIBUTE) @NonNull UserProfileUpdate profileForm,
                                @NonNull BindingResult bindingResult,
                                @NonNull Model model,
                                @NonNull RedirectAttributes redirectAttributes,
                                @NonNull HttpServletRequest request,
                                @RequestParam(value = "pictureFile", required = false) MultipartFile pictureFile) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo currentProfile = userProfileService.getProfile(userId);

        if (bindingResult.hasErrors()) {
            addProfileModelAttributes(model, userId, currentProfile, request);
            return PROFILE_VIEW;
        }

        final UserProfileUpdate update;
        try {
            update = updateWithPicture(userId, profileForm, currentProfile, pictureFile);
        } catch (ProfilePictureStorageException exception) {
            bindingResult.rejectValue("pictureUrl", "profilePictureUpload", exception.userMessage());
            addProfileModelAttributes(model, userId, currentProfile, request);
            return PROFILE_VIEW;
        }

        userProfileService.updateProfile(userId, update);
        if (profileSetupSession.isProfileSetupRequired(request)) {
            profileSetupSession.completeProfileSetup(request);
            return REMEMBER_LOGIN_REDIRECT;
        }
        redirectAttributes.addFlashAttribute(PROFILE_SAVED_ATTRIBUTE, true);
        return PROFILE_REDIRECT;
    }

    @PostMapping("/profile/password")
    public String changePassword(@NonNull Authentication authentication,
                                 @Valid @ModelAttribute(PASSWORD_FORM_ATTRIBUTE) @NonNull UserPasswordChange passwordForm,
                                 @NonNull BindingResult bindingResult,
                                 @NonNull Model model,
                                 @NonNull RedirectAttributes redirectAttributes,
                                 @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo currentProfile = userProfileService.getProfile(userId);
        if (localUserService.loginName(userId).isEmpty() || profileSetupSession.isProfileSetupRequired(request)) {
            throw new AccessDeniedException("An established local account is required to change password");
        }

        if (!Objects.equals(passwordForm.newPassword(), passwordForm.newPasswordConfirmation())) {
            bindingResult.rejectValue("newPasswordConfirmation", "passwordMismatch", PASSWORD_MISMATCH_MESSAGE);
        }
        if (bindingResult.hasErrors()) {
            return profileViewWithPasswordErrors(model, userId, currentProfile, passwordForm, request);
        }

        try {
            localUserService.changePassword(userId, passwordForm.currentPassword(), passwordForm.newPassword());
        } catch (BadCredentialsException exception) {
            bindingResult.rejectValue("currentPassword", "badCredentials", exception.getMessage());
            return profileViewWithPasswordErrors(model, userId, currentProfile, passwordForm, request);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("newPassword", "passwordRejected", exception.getMessage());
            return profileViewWithPasswordErrors(model, userId, currentProfile, passwordForm, request);
        }

        redirectAttributes.addFlashAttribute(PASSWORD_SAVED_ATTRIBUTE, true);
        return PROFILE_REDIRECT;
    }

    private String profileViewWithPasswordErrors(@NonNull Model model,
                                                 @NonNull UUID userId,
                                                 @NonNull UserProfileDbo currentProfile,
                                                 @NonNull UserPasswordChange passwordForm,
                                                 @NonNull HttpServletRequest request) {
        model.addAttribute(PASSWORD_FORM_ATTRIBUTE, passwordForm);
        model.addAttribute(PASSWORD_DIALOG_OPEN_ATTRIBUTE, true);
        addProfileModelAttributes(model, userId, currentProfile, request);
        addProfileFormIfMissing(model, currentProfile);
        return PROFILE_VIEW;
    }

    private void addProfileFormIfMissing(@NonNull Model model, @NonNull UserProfileDbo profile) {
        if (!model.containsAttribute(PROFILE_FORM_ATTRIBUTE)) {
            model.addAttribute(PROFILE_FORM_ATTRIBUTE, profileForm(profile));
        }
    }

    private UserProfileUpdate profileForm(@NonNull UserProfileDbo profile) {
        return new UserProfileUpdate(profile.displayName(), profile.pictureUrl());
    }

    private UserProfileUpdate updateWithPicture(@NonNull UUID userId,
                                                @NonNull UserProfileUpdate profileForm,
                                                @NonNull UserProfileDbo currentProfile,
                                                MultipartFile pictureFile) {
        if (pictureFile == null || pictureFile.isEmpty()) {
            return new UserProfileUpdate(profileForm.displayName(), currentProfile.pictureUrl());
        }
        final String uploadedPictureUrl = profilePictureStorageService.store(userId, pictureFile);
        return new UserProfileUpdate(profileForm.displayName(), uploadedPictureUrl);
    }

    private void addProfileModelAttributes(@NonNull Model model,
                                           @NonNull UUID userId,
                                           @NonNull UserProfileDbo profile,
                                           @NonNull HttpServletRequest request) {
        final Optional<String> loginName = localUserService.loginName(userId);
        final boolean passwordChangeAvailable =
                loginName.isPresent() && !profileSetupSession.isProfileSetupRequired(request);
        model.addAttribute(LOCAL_PROFILE_ATTRIBUTE, loginName.isPresent());
        model.addAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE, passwordChangeAvailable);
        loginName.ifPresent(value -> model.addAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE, value));
        if (passwordChangeAvailable && !model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)) {
            model.addAttribute(PASSWORD_FORM_ATTRIBUTE, new UserPasswordChange(null, null, null));
        }
        model.addAttribute(PROFILE_PICTURE_ATTRIBUTE, profile.pictureUrl());
    }
}
