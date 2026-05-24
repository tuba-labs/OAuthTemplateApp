package org.tubalabs.app.ui.profile.profile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
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
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.profile.UserProfileUpdate;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageFailure;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;
import org.tubalabs.app.ui.profile.profile.dtos.UserProfileUpdateDto;

import java.util.UUID;

@Controller
public class UserProfileController extends AbstractNavigationController {

    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
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
    private final CurrentUserSession currentUserSession;

    public UserProfileController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                                 @NonNull CurrentUserSession currentUserSession,
                                 @NonNull ProfileSetupRequirementService profileSetupRequirementService,
                                 @NonNull NavigationPageModel navigationPageModel,
                                 @NonNull UserProfileService userProfileService,
                                 @NonNull ProfileSetupSession profileSetupSession,
                                 @NonNull ProfilePictureStorageService profilePictureStorageService,
                                 @NonNull ProfilePageModel profilePageModel) {
        super(currentUserIdResolver, currentUserSession, profileSetupRequirementService, navigationPageModel);
        this.currentUserIdResolver = currentUserIdResolver;
        this.userProfileService = userProfileService;
        this.profileSetupSession = profileSetupSession;
        this.profilePictureStorageService = profilePictureStorageService;
        this.profilePageModel = profilePageModel;
        this.currentUserSession = currentUserSession;
    }

    @GetMapping(ProfilePage.RELATIVE_URL)
    public String profile(@NonNull Authentication authentication,
                          @NonNull Model model,
                          @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        final CurrentUser currentUser = currentUser(userId, request);
        addProfileFormIfMissing(model, profile);
        profilePageModel.addProfileAttributes(model, currentUser, profile);
        return ProfilePage.VIEW;
    }

    @PostMapping(ProfilePage.RELATIVE_URL)
    public String updateProfile(@NonNull Authentication authentication,
                                @Valid @ModelAttribute(PROFILE_FORM_ATTRIBUTE) @NonNull UserProfileUpdateDto profileForm,
                                @NonNull BindingResult bindingResult,
                                @NonNull Model model,
                                @NonNull RedirectAttributes redirectAttributes,
                                @NonNull HttpServletRequest request,
                                @RequestParam(value = "pictureFile", required = false) MultipartFile pictureFile) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo currentProfile = userProfileService.getProfile(userId);

        if (bindingResult.hasErrors()) {
            profilePageModel.addProfileAttributes(model, currentUser(userId, request), currentProfile);
            return ProfilePage.VIEW;
        }

        final UserProfileUpdateDto update;
        try {
            update = updateWithPicture(userId, profileForm, currentProfile, pictureFile);
        } catch (ProfilePictureStorageException exception) {
            bindingResult.rejectValue("pictureUrl", "profilePictureUpload", profilePictureFailureMessage(exception));
            profilePageModel.addProfileAttributes(model, currentUser(userId, request), currentProfile);
            return ProfilePage.VIEW;
        }

        userProfileService.updateProfile(userId, profileUpdate(update));
        if (profileSetupSession.isProfileSetupRequired(request)) {
            profileSetupSession.completeProfileSetup(request);
            currentUserSession.refresh(request, userId, false);
            return REMEMBER_LOGIN_REDIRECT;
        }
        currentUserSession.refresh(request, userId, false);
        redirectAttributes.addFlashAttribute(PROFILE_SAVED_ATTRIBUTE, true);
        return ProfilePage.REDIRECT;
    }

    private CurrentUser currentUser(@NonNull UUID userId, @NonNull HttpServletRequest request) {
        return currentUserSession.currentUser(request)
                .orElseGet(() -> currentUserSession.refresh(
                        request, userId, profileSetupSession.isProfileSetupRequired(request)));
    }

    private void addProfileFormIfMissing(@NonNull Model model, @NonNull UserProfileDbo profile) {
        if (!model.containsAttribute(PROFILE_FORM_ATTRIBUTE)) {
            model.addAttribute(PROFILE_FORM_ATTRIBUTE, profileForm(profile));
        }
    }

    private UserProfileUpdateDto profileForm(@NonNull UserProfileDbo profile) {
        return new UserProfileUpdateDto(profile.displayName(), profile.pictureUrl());
    }

    private UserProfileUpdateDto updateWithPicture(@NonNull UUID userId,
                                                   @NonNull UserProfileUpdateDto profileForm,
                                                   @NonNull UserProfileDbo currentProfile,
                                                   MultipartFile pictureFile) {
        if (pictureFile == null || pictureFile.isEmpty()) {
            return new UserProfileUpdateDto(profileForm.displayName(), currentProfile.pictureUrl());
        }
        final String uploadedPictureUrl = profilePictureStorageService.store(userId, pictureFile);
        return new UserProfileUpdateDto(profileForm.displayName(), uploadedPictureUrl);
    }

    private UserProfileUpdate profileUpdate(@NonNull UserProfileUpdateDto profileForm) {
        return new UserProfileUpdate(profileForm.displayName(), profileForm.pictureUrl());
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
