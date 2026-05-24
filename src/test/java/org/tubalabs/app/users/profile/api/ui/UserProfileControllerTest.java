package org.tubalabs.app.users.profile.api.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.identity.CurrentLoginProviderResolver;
import org.tubalabs.app.users.identity.LinkedLoginType;
import org.tubalabs.app.users.identity.LoginTypeUnlinkException;
import org.tubalabs.app.users.identity.LoginTypeUnlinkFailure;
import org.tubalabs.app.users.identity.LoginTypeUnlinkState;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkFailure;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.UserProfileUpdate;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageFailure;
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
    private static final String PROFILE_PASSWORD_VIEW = "org/tubalabs/app/users/profile/profile-password";
    private static final String PROFILE_LOGIN_TYPES_VIEW = "org/tubalabs/app/users/profile/profile-login-types";
    private static final String PROFILE_LOCAL_LOGIN_TYPE_VIEW = "org/tubalabs/app/users/profile/profile-local-login-type";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String PROFILE_LOGIN_TYPES_REDIRECT = "redirect:/profile/login-types";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";
    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PASSWORD_FORM_ATTRIBUTE = "passwordForm";
    private static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    private static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    private static final String PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE = "passwordChangeAvailable";
    private static final String LOGIN_TYPES_AVAILABLE_ATTRIBUTE = "loginTypesAvailable";
    private static final String LOGIN_TYPE_OPTIONS_ATTRIBUTE = "loginTypeOptions";
    private static final String LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE = "linkedLoginTypeOptions";
    private static final String LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE = "localLoginTypeAvailable";
    private static final String LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE = "localLoginTypeForm";
    private static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String PASSWORD_SAVED_ATTRIBUTE = "passwordSaved";
    private static final String LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE = "loginTypeLinkedMessage";
    private static final String LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE = "loginTypeErrorMessage";
    private static final String GOOGLE_PROVIDER_ID = "google";
    private static final String GOOGLE_LABEL = "Google";
    private static final ProfileLoginTypeOption GOOGLE_LOGIN_TYPE_OPTION =
            new ProfileLoginTypeOption(GOOGLE_PROVIDER_ID, GOOGLE_LABEL);
    private static final LinkedLoginType GOOGLE_LINKED_LOGIN_TYPE =
            new LinkedLoginType(GOOGLE_PROVIDER_ID, false, LoginTypeUnlinkState.AVAILABLE);
    private static final ProfileLinkedLoginTypeOption GOOGLE_LINKED_LOGIN_TYPE_OPTION =
            new ProfileLinkedLoginTypeOption(GOOGLE_PROVIDER_ID, GOOGLE_LABEL, false, true, "");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String UPLOADED_PICTURE_URL = "/profile-pictures/22222222-2222-2222-2222-222222222222.jpg";
    private static final String CURRENT_PASSWORD = "ValidPassword1";
    private static final String NEW_PASSWORD = "NewValidPassword1";

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final CurrentLoginProviderResolver currentLoginProviderResolver = Mockito.mock(CurrentLoginProviderResolver.class);
    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final LocalUserService localUserService = Mockito.mock(LocalUserService.class);
    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final ProfilePictureStorageService profilePictureStorageService = Mockito.mock(ProfilePictureStorageService.class);
    private final ClientRegistrationRepository clientRegistrationRepository = Mockito.mock(ClientRegistrationRepository.class);
    private final UserLoginTypeService userLoginTypeService = Mockito.mock(UserLoginTypeService.class);
    private final ExternalIdentityLinkSession externalIdentityLinkSession = Mockito.mock(ExternalIdentityLinkSession.class);
    private final ProfilePageModel profilePageModel =
            new ProfilePageModel(
                    localUserService,
                    clientRegistrationRepository,
                    profileSetupSession,
                    userLoginTypeService,
                    externalIdentityLinkSession);
    private final UserProfileController profileController =
            new UserProfileController(
                    currentUserIdResolver,
                    userProfileService,
                    profileSetupSession,
                    profilePictureStorageService,
                    profilePageModel);
    private final UserProfilePasswordController passwordController =
            new UserProfilePasswordController(
                    currentUserIdResolver,
                    localUserService,
                    profileSetupSession,
                    profilePageModel);
    private final UserProfileLoginTypesController loginTypesController =
            new UserProfileLoginTypesController(
                    currentUserIdResolver,
                    currentLoginProviderResolver,
                    localUserService,
                    profileSetupSession,
                    userLoginTypeService,
                    externalIdentityLinkSession,
                    profilePageModel);
    private final Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());

    @BeforeEach
    void setUp() {
        when(currentLoginProviderResolver.providerId(Mockito.eq(authentication), Mockito.any()))
                .thenReturn(Optional.of(LocalUserService.LOCAL_PROVIDER_ID));
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.empty());
        when(clientRegistrationRepository.findByRegistrationId(GOOGLE_PROVIDER_ID))
                .thenReturn(clientRegistration(GOOGLE_PROVIDER_ID, GOOGLE_LABEL));
        when(userLoginTypeService.availableExternalLoginTypes(USER_ID)).thenReturn(List.of(GOOGLE_PROVIDER_ID));
        when(userLoginTypeService.linkedLoginTypes(USER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID)))
                .thenReturn(List.of(GOOGLE_LINKED_LOGIN_TYPE));
        when(userLoginTypeService.canLinkLocalLoginType(USER_ID)).thenReturn(true);
        when(externalIdentityLinkSession.consumeFailure(Mockito.any())).thenReturn(Optional.empty());
    }

    @Test
    void showsCurrentProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = profileController.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.getAttribute(LOGIN_TYPES_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.containsAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)).isFalse();
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

        final String view = profileController.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void hidesPasswordChangeDuringProfileSetup() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.of(EMAIL));
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(profileSetupSession.isProfileSetupRequired(request)).thenReturn(true);

        final String view = profileController.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.getAttribute(LOGIN_TYPES_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void showsPasswordPageForLocalProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(localUserService.loginName(USER_ID)).thenReturn(Optional.of(EMAIL));
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = passwordController.password(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_PASSWORD_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOGIN_TYPES_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.getAttribute(PASSWORD_FORM_ATTRIBUTE)).isEqualTo(new UserPasswordChange(null, null, null));
    }

    @Test
    void showsLoginTypesPage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.getAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertThat(model.getAttribute(LOGIN_TYPES_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isEqualTo(List.of(GOOGLE_LOGIN_TYPE_OPTION));
        assertThat(model.getAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isEqualTo(List.of(GOOGLE_LINKED_LOGIN_TYPE_OPTION));
        assertThat(model.getAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void showsLoginTypesPageWithExternalLinkSuccessMessage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(externalIdentityLinkSession.consumeSuccess(request)).thenReturn(true);

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        assertThat(model.getAttribute(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE)).isEqualTo("Login type linked.");
    }

    @Test
    void showsLoginTypesPageWithExternalLinkFailureMessage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(externalIdentityLinkSession.consumeFailure(request))
                .thenReturn(Optional.of(IdentityLinkFailure.PROVIDER_ALREADY_LINKED));

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        assertThat(model.getAttribute(LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE))
                .isEqualTo("This account already has that login type linked");
    }

    @Test
    void showsLocalLoginTypePage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userLoginTypeService.canLinkLocalLoginType(USER_ID)).thenReturn(true);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.localLoginType(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOCAL_LOGIN_TYPE_VIEW);
        assertThat(model.getAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE))
                .isEqualTo(new UserLocalLoginTypeLink(null, null, null));
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

        final String view = profileController.updateProfile(
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

        final String view = profileController.updateProfile(
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
                .thenThrow(new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE));

        final String view = profileController.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(PROFILE_PICTURE_ATTRIBUTE)).isEqualTo(PICTURE_URL);
        assertThat(bindingResult.getFieldError("pictureUrl").getDefaultMessage())
                .isEqualTo("Profile picture must be a PNG, JPEG, or GIF image");
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

        final String view = profileController.updateProfile(
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

        final String view = profileController.updateProfile(
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

        final String view = passwordController.changePassword(
                authentication, passwordForm, bindingResult, model, redirectAttributes, request);

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

        final String view = passwordController.changePassword(
                authentication, passwordForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_PASSWORD_VIEW);
        assertThat(bindingResult.getFieldError("newPasswordConfirmation").getDefaultMessage()).isEqualTo("Passwords do not match");
        verify(localUserService, never()).changePassword(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void startsLoginTypeLink() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userLoginTypeService.canLinkExternalLoginType(USER_ID, GOOGLE_PROVIDER_ID)).thenReturn(true);
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.linkLoginType(authentication, GOOGLE_PROVIDER_ID, request);

        assertThat(view).isEqualTo("redirect:/oauth2/authorization/" + GOOGLE_PROVIDER_ID);
        verify(externalIdentityLinkSession).start(request, USER_ID, GOOGLE_PROVIDER_ID, authentication);
    }

    @Test
    void linksLocalLoginType() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(userLoginTypeService.canLinkLocalLoginType(USER_ID)).thenReturn(true);
        final UserLocalLoginTypeLink form = new UserLocalLoginTypeLink(EMAIL, NEW_PASSWORD, NEW_PASSWORD);
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.linkLocalLoginType(
                authentication, form, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE))
                .isEqualTo("Email and password linked.");
        verify(localUserService).linkLogin(USER_ID, new LocalUserRegistration(EMAIL, NEW_PASSWORD));
    }

    @Test
    void doesNotLinkLocalLoginTypeWhenConfirmationDoesNotMatch() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(userLoginTypeService.canLinkLocalLoginType(USER_ID)).thenReturn(true);
        final UserLocalLoginTypeLink form = new UserLocalLoginTypeLink(EMAIL, NEW_PASSWORD, "DifferentPassword1");
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.linkLocalLoginType(
                authentication, form, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_LOCAL_LOGIN_TYPE_VIEW);
        assertThat(bindingResult.getFieldError("passwordConfirmation").getDefaultMessage()).isEqualTo("Passwords do not match");
        verify(localUserService, never()).linkLogin(Mockito.any(), Mockito.any());
    }

    @Test
    void unlinksLoginType() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        final String view = loginTypesController.unlinkLoginType(authentication, GOOGLE_PROVIDER_ID, request, redirectAttributes);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE))
                .isEqualTo("Login type unlinked.");
        verify(userLoginTypeService).unlinkLoginType(
                USER_ID, GOOGLE_PROVIDER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID));
    }

    @Test
    void redirectsWithErrorWhenUnlinkFails() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Mockito.doThrow(new LoginTypeUnlinkException(LoginTypeUnlinkFailure.NOT_LINKED))
                .when(userLoginTypeService)
                .unlinkLoginType(USER_ID, GOOGLE_PROVIDER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID));

        final String view = loginTypesController.unlinkLoginType(authentication, GOOGLE_PROVIDER_ID, request, redirectAttributes);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE)).isEqualTo("Login type is not linked");
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

    private ClientRegistration clientRegistration(String providerId, String label) {
        return ClientRegistration.withRegistrationId(providerId)
                .clientName(label)
                .clientId("client-" + providerId)
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/" + providerId)
                .authorizationUri("https://example.com/" + providerId + "/authorize")
                .tokenUri("https://example.com/" + providerId + "/token")
                .build();
    }
}
