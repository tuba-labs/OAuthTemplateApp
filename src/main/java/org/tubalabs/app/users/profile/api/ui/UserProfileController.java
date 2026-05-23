package org.tubalabs.app.users.profile.api.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PROFILE_EMAIL_ATTRIBUTE = "profileEmail";
    private static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String PROFILE_VIEW = "org/tubalabs/app/users/profile/profile";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final UserProfileService userProfileService;
    private final ProfileSetupSession profileSetupSession;
    private final ProfilePictureStorageService profilePictureStorageService;

    @GetMapping("/profile")
    public String profile(@NonNull Authentication authentication, @NonNull Model model) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        if (!model.containsAttribute(PROFILE_FORM_ATTRIBUTE)) {
            model.addAttribute(PROFILE_FORM_ATTRIBUTE, profileForm(profile));
        }
        addProfileModelAttributes(model, profile);
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
            addProfileModelAttributes(model, currentProfile);
            return PROFILE_VIEW;
        }

        final UserProfileUpdate update;
        try {
            update = updateWithPicture(userId, profileForm, currentProfile, pictureFile);
        } catch (ProfilePictureStorageException exception) {
            bindingResult.rejectValue("pictureUrl", "profilePictureUpload", exception.userMessage());
            addProfileModelAttributes(model, currentProfile);
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

    private void addProfileModelAttributes(@NonNull Model model, @NonNull UserProfileDbo profile) {
        model.addAttribute(PROFILE_EMAIL_ATTRIBUTE, profile.email());
        model.addAttribute(PROFILE_PICTURE_ATTRIBUTE, profile.pictureUrl());
    }
}
