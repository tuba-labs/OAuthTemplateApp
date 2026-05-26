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
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePage;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePageModel;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.profile.UserProfileUpdate;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;
import org.tubalabs.app.users.settings.UserLanguage;
import org.tubalabs.app.users.settings.UserSettingsService;
import org.tubalabs.app.ui.profile.profile.dtos.UserProfileUpdateDto;

import java.util.Optional;
import java.util.UUID;

@Controller
public class UserProfileController extends AbstractNavigationController {

    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final UserProfileService userProfileService;
    private final ProfileSetupSession profileSetupSession;
    private final ProfilePictureStorageService profilePictureStorageService;
    private final ProfilePageModel profilePageModel;
    private final LocalizationService localizationService;
    private final UserSettingsService userSettingsService;
    private final CurrentUserSession currentUserSession;

    public UserProfileController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                                 @NonNull CurrentUserSession currentUserSession,
                                 @NonNull ProfileSetupRequirementService profileSetupRequirementService,
                                 @NonNull NavigationPageModel navigationPageModel,
                                 @NonNull UserProfileService userProfileService,
                                 @NonNull ProfileSetupSession profileSetupSession,
                                 @NonNull ProfilePictureStorageService profilePictureStorageService,
                                 @NonNull ProfilePageModel profilePageModel,
                                 @NonNull LocalizationService localizationService,
                                 @NonNull UserSettingsService userSettingsService) {
        super(currentUserIdResolver, currentUserSession, profileSetupRequirementService, navigationPageModel);
        this.currentUserIdResolver = currentUserIdResolver;
        this.userProfileService = userProfileService;
        this.profileSetupSession = profileSetupSession;
        this.profilePictureStorageService = profilePictureStorageService;
        this.profilePageModel = profilePageModel;
        this.localizationService = localizationService;
        this.userSettingsService = userSettingsService;
        this.currentUserSession = currentUserSession;
    }

    @GetMapping(ProfilePage.RELATIVE_URL)
    public String profile(@NonNull Authentication authentication,
                          @NonNull Model model,
                          @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        final CurrentUser currentUser = currentUser(userId, request);
        addProfileFormIfMissing(model, profile, currentUser);
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
        final Optional<UserLanguage> selectedLanguage = UserLanguage.fromTag(profileForm.languageTag());
        if (selectedLanguage.isEmpty()) {
            bindingResult.rejectValue(
                    "languageTag",
                    "unsupportedLanguage",
                    localizationService.message("profile.language.error.unsupported"));
        }

        if (bindingResult.hasErrors()) {
            profilePageModel.addProfileAttributes(model, currentUser(userId, request), currentProfile);
            return ProfilePage.VIEW;
        }

        final UserProfileUpdateDto update;
        try {
            update = updateWithPicture(userId, profileForm, currentProfile, pictureFile);
        } catch (ProfilePictureStorageException exception) {
            bindingResult.rejectValue(
                    "pictureUrl",
                    "profilePictureUpload",
                    localizationService.message(exception.reason()));
            profilePageModel.addProfileAttributes(model, currentUser(userId, request), currentProfile);
            return ProfilePage.VIEW;
        }

        userProfileService.updateProfile(userId, profileUpdate(update));
        userSettingsService.updateLanguage(userId, selectedLanguage.orElseThrow());
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

    private void addProfileFormIfMissing(@NonNull Model model,
                                         @NonNull UserProfileDbo profile,
                                         @NonNull CurrentUser currentUser) {
        if (!model.containsAttribute(PROFILE_FORM_ATTRIBUTE)) {
            model.addAttribute(PROFILE_FORM_ATTRIBUTE, profileForm(profile, currentUser));
        }
    }

    private UserProfileUpdateDto profileForm(@NonNull UserProfileDbo profile, @NonNull CurrentUser currentUser) {
        return new UserProfileUpdateDto(profile.displayName(), profile.pictureUrl(), currentUser.languageTag());
    }

    private UserProfileUpdateDto updateWithPicture(@NonNull UUID userId,
                                                   @NonNull UserProfileUpdateDto profileForm,
                                                   @NonNull UserProfileDbo currentProfile,
                                                   MultipartFile pictureFile) {
        if (pictureFile == null || pictureFile.isEmpty()) {
            return new UserProfileUpdateDto(
                    profileForm.displayName(),
                    currentProfile.pictureUrl(),
                    profileForm.languageTag());
        }
        final String uploadedPictureUrl = profilePictureStorageService.store(userId, pictureFile);
        return new UserProfileUpdateDto(profileForm.displayName(), uploadedPictureUrl, profileForm.languageTag());
    }

    private UserProfileUpdate profileUpdate(@NonNull UserProfileUpdateDto profileForm) {
        return new UserProfileUpdate(profileForm.displayName(), profileForm.pictureUrl());
    }
}
