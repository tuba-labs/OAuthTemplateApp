package org.tubalabs.app.users.profile.api.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileControllerTest {

    private static final String PROFILE_VIEW = "org/tubalabs/app/users/profile/profile";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";
    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PASSWORD_FORM_ATTRIBUTE = "passwordForm";
    private static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    private static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    private static final String PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE = "passwordChangeAvailable";
    private static final String PASSWORD_DIALOG_OPEN_ATTRIBUTE = "passwordDialogOpen";
    private static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String PASSWORD_SAVED_ATTRIBUTE = "passwordSaved";
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String UPLOADED_PICTURE_URL = "/profile-pictures/22222222-2222-2222-2222-222222222222.jpg";
    private static final String CURRENT_PASSWORD = "ValidPassword1";
    private static final String NEW_PASSWORD = "NewValidPassword1";

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final LocalUserService localUserService = Mockito.mock(LocalUserService.class);
    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final ProfilePictureStorageService profilePictureStorageService = Mockito.mock(ProfilePictureStorageService.class);
    private final UserProfileController controller =
            new UserProfileController(
                    currentUserIdResolver, userProfileService, localUserService, profileSetupSession, profilePictureStorageService);
    private final Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());

    @BeforeEach
    void setUp() {
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.empty());
    }

    @Test
    void showsCurrentProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = controller.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.containsAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
        assertThat(model.getAttribute(PROFILE_PICTURE_ATTRIBUTE)).isEqualTo(PICTURE_URL);
        assertThat(model.getAttribute(PROFILE_FORM_ATTRIBUTE))
                .isEqualTo(new UserProfileUpdate(DISPLAY_NAME, PICTURE_URL));
    }

    @Test
    void showsLocalLoginNameAndPasswordFormForLocalProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.of(EMAIL));
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = controller.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.getAttribute(PASSWORD_FORM_ATTRIBUTE)).isEqualTo(new UserPasswordChange(null, null, null));
    }

    @Test
    void hidesPasswordChangeDuringProfileSetup() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.of(EMAIL));
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(profileSetupSession.isProfileSetupRequired(request)).thenReturn(true);

        final String view = controller.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void updatesCurrentProfileAndKeepsCurrentPictureWhenNoPictureIsUploaded() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdate profileForm = new UserProfileUpdate(DISPLAY_NAME, null);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockMultipartFile pictureFile = emptyPictureFile();

        final String view = controller.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(PROFILE_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(PROFILE_SAVED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        verify(userProfileService).updateProfile(USER_ID, new UserProfileUpdate(DISPLAY_NAME, PICTURE_URL));
    }

    @Test
    void updatesCurrentProfileWithUploadedPictureUrl() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdate profileForm = new UserProfileUpdate(DISPLAY_NAME, null);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockMultipartFile pictureFile = pngPictureFile();
        when(profilePictureStorageService.store(USER_ID, pictureFile)).thenReturn(UPLOADED_PICTURE_URL);

        final String view = controller.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(PROFILE_REDIRECT);
        verify(userProfileService).updateProfile(USER_ID, new UserProfileUpdate(DISPLAY_NAME, UPLOADED_PICTURE_URL));
    }

    @Test
    void doesNotUpdateWhenPictureUploadFails() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdate profileForm = new UserProfileUpdate(DISPLAY_NAME, null);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockMultipartFile pictureFile = pngPictureFile();
        when(profilePictureStorageService.store(USER_ID, pictureFile))
                .thenThrow(new ProfilePictureStorageException("Bad image"));

        final String view = controller.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(PROFILE_PICTURE_ATTRIBUTE)).isEqualTo(PICTURE_URL);
        assertThat(bindingResult.getFieldError("pictureUrl").getDefaultMessage()).isEqualTo("Bad image");
        verify(userProfileService, never()).updateProfile(Mockito.any(), Mockito.any());
    }

    @Test
    void redirectsToRememberLoginAfterRequiredProfileSetup() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdate profileForm = new UserProfileUpdate(DISPLAY_NAME, null);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockMultipartFile pictureFile = emptyPictureFile();
        when(profileSetupSession.isProfileSetupRequired(request)).thenReturn(true);

        final String view = controller.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(REMEMBER_LOGIN_REDIRECT);
        verify(userProfileService).updateProfile(USER_ID, new UserProfileUpdate(DISPLAY_NAME, PICTURE_URL));
        verify(profileSetupSession).completeProfileSetup(request);
    }

    @Test
    void doesNotUpdateWhenFormHasErrors() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdate profileForm = new UserProfileUpdate("", PICTURE_URL);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        bindingResult.rejectValue("displayName", "NotBlank", "Display name is required");
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockMultipartFile pictureFile = emptyPictureFile();

        final String view = controller.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(PROFILE_PICTURE_ATTRIBUTE)).isEqualTo(PICTURE_URL);
        verify(userProfileService, never()).updateProfile(Mockito.any(), Mockito.any());
        verify(profileSetupSession, never()).completeProfileSetup(Mockito.any());
    }

    @Test
    void changesPasswordForLocalProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.of(EMAIL));
        final UserPasswordChange passwordForm = new UserPasswordChange(CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        final BindingResult bindingResult = new BeanPropertyBindingResult(passwordForm, PASSWORD_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = controller.changePassword(authentication, passwordForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(PASSWORD_SAVED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        verify(localUserService).changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);
    }

    @Test
    void doesNotChangePasswordWhenConfirmationDoesNotMatch() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.of(EMAIL));
        final UserPasswordChange passwordForm = new UserPasswordChange(CURRENT_PASSWORD, NEW_PASSWORD, "DifferentPassword1");
        final BindingResult bindingResult = new BeanPropertyBindingResult(passwordForm, PASSWORD_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = controller.changePassword(authentication, passwordForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(bindingResult.getFieldError("newPasswordConfirmation").getDefaultMessage()).isEqualTo("Passwords do not match");
        assertThat(model.getAttribute(PASSWORD_DIALOG_OPEN_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        verify(localUserService, never()).changePassword(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private MockMultipartFile emptyPictureFile() {
        return new MockMultipartFile("pictureFile", "", "application/octet-stream", new byte[0]);
    }

    private MockMultipartFile pngPictureFile() {
        return new MockMultipartFile("pictureFile", "avatar.png", "image/png", new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00});
    }

    private UserProfileDbo profile() {
        return UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(DISPLAY_NAME)
                .pictureUrl(PICTURE_URL)
                .build();
    }
}
