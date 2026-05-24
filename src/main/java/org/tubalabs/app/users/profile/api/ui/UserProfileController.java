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
import org.tubalabs.app.users.profile.UserProfileUpdate;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageFailure;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String PROFILE_VIEW = "org/tubalabs/app/users/profile/profile";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";
    private static final String EMPTY_PROFILE_PICTURE_MESSAGE = "Choose a profile picture to upload";
    private static final String PROFILE_PICTURE_TOO_LARGE_MESSAGE = "Profile picture is too large";
    private static final String INVALID_PROFILE_PICTURE_MESSAGE = "Profile picture must be a PNG, JPEG, or GIF image";
    private static final String PROFILE_PICTURE_DIMENSIONS_TOO_LARGE_MESSAGE =
            "Profile picture dimensions are too large";
    private static final String PROFILE_PICTURE_UPLOAD_FAILED_MESSAGE = "Could not upload profile picture";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final UserProfileService userProfileService;
    private final ProfileSetupSession profileSetupSession;
    private final ProfilePictureStorageService profilePictureStorageService;
    private final ProfilePageModel profilePageModel;

    @GetMapping("/profile")
    public String profile(@NonNull Authentication authentication,
                          @NonNull Model model,
                          @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        addProfileFormIfMissing(model, profile);
        profilePageModel.addProfileAttributes(model, userId, profile, request);
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
            profilePageModel.addProfileAttributes(model, userId, currentProfile, request);
            return PROFILE_VIEW;
        }

        final UserProfileUpdate update;
        try {
            update = updateWithPicture(userId, profileForm, currentProfile, pictureFile);
        } catch (ProfilePictureStorageException exception) {
            bindingResult.rejectValue("pictureUrl", "profilePictureUpload", profilePictureFailureMessage(exception));
            profilePageModel.addProfileAttributes(model, userId, currentProfile, request);
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

    private String profilePictureFailureMessage(@NonNull ProfilePictureStorageException exception) {
        final ProfilePictureStorageFailure reason = exception.reason();
        return switch (reason) {
            case EMPTY_FILE -> EMPTY_PROFILE_PICTURE_MESSAGE;
            case FILE_TOO_LARGE -> PROFILE_PICTURE_TOO_LARGE_MESSAGE;
            case INVALID_IMAGE -> INVALID_PROFILE_PICTURE_MESSAGE;
            case DIMENSIONS_TOO_LARGE -> PROFILE_PICTURE_DIMENSIONS_TOO_LARGE_MESSAGE;
            case INVALID_PATH, READ_FAILED, STORE_FAILED -> PROFILE_PICTURE_UPLOAD_FAILED_MESSAGE;
        };
    }
}
