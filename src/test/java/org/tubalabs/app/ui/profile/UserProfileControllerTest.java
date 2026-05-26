package org.tubalabs.app.ui.profile;

import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
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
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
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
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageException;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageFailure;
import org.tubalabs.app.users.profile.profilepicture.ProfilePictureStorageService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.profile.UserProfileUpdate;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.settings.UserLanguage;
import org.tubalabs.app.users.settings.UserSettingsService;
import org.tubalabs.app.ui.profile.changepassword.UserProfilePasswordController;
import org.tubalabs.app.ui.profile.logintypes.dtos.ProfileLinkedLoginTypeOptionDto;
import org.tubalabs.app.ui.profile.logintypes.dtos.ProfileLoginTypeOptionDto;
import org.tubalabs.app.ui.profile.logintypes.dtos.UserLocalLoginTypeLinkDto;
import org.tubalabs.app.ui.profile.logintypes.menusystem.ProfileLoginTypesPageModel;
import org.tubalabs.app.ui.profile.logintypes.UserProfileLoginTypesController;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePageModel;
import org.tubalabs.app.ui.profile.profile.UserProfileController;
import org.tubalabs.app.ui.profile.changepassword.dtos.UserPasswordChangeDto;
import org.tubalabs.app.ui.profile.profile.dtos.UserProfileUpdateDto;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileControllerTest {

    private static final String PROFILE_VIEW = "ui/profile/profile/profile";
    private static final String PROFILE_PASSWORD_VIEW = "ui/profile/changepassword/profile-password";
    private static final String PROFILE_LOGIN_TYPES_VIEW = "ui/profile/logintypes/profile-login-types";
    private static final String PROFILE_LOCAL_LOGIN_TYPE_VIEW = "ui/profile/logintypes/local/profile-local-login-type";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String PROFILE_LOGIN_TYPES_REDIRECT = "redirect:/profile/login-types";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";
    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PASSWORD_FORM_ATTRIBUTE = "passwordForm";
    private static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    private static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    private static final String LANGUAGE_OPTIONS_ATTRIBUTE = "languageOptions";
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
    private static final String LOCAL_LOGIN_TYPE_LABEL = "Email and password";
    private static final String NORWEGIAN_LOCAL_LOGIN_TYPE_LABEL = "E-post og passord";
    private static final String LOGIN_TYPE_LINKED_MESSAGE = "Login type linked.";
    private static final String LOCAL_LOGIN_TYPE_LINKED_MESSAGE = "Email and password linked.";
    private static final String LOGIN_TYPE_UNLINKED_MESSAGE = "Login type unlinked.";
    private static final String PROVIDER_ALREADY_LINKED_MESSAGE = "This account already has that login type linked";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";
    private static final String INVALID_PROFILE_PICTURE_MESSAGE = "Profile picture must be a PNG, JPEG, or GIF image";
    private static final String UNLINKED_LOGIN_TYPE_MESSAGE = "Login type is not linked";
    private static final Locale TEST_LOCALE = Locale.ENGLISH;
    private static final ProfileLoginTypeOptionDto GOOGLE_LOGIN_TYPE_OPTION =
            new ProfileLoginTypeOptionDto(GOOGLE_PROVIDER_ID, GOOGLE_LABEL);
    private static final LinkedLoginType GOOGLE_LINKED_LOGIN_TYPE =
            new LinkedLoginType(GOOGLE_PROVIDER_ID, false, LoginTypeUnlinkState.AVAILABLE);
    private static final ProfileLinkedLoginTypeOptionDto GOOGLE_LINKED_LOGIN_TYPE_OPTION =
            new ProfileLinkedLoginTypeOptionDto(
                    GOOGLE_PROVIDER_ID,
                    GOOGLE_LABEL,
                    false,
                    false,
                    true,
                    "");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String UPLOADED_PICTURE_URL = "/profile-pictures/22222222-2222-2222-2222-222222222222.jpg";
    private static final String CURRENT_PASSWORD = "ValidPassword1";
    private static final String NEW_PASSWORD = "NewValidPassword1";
    private static final String ENGLISH_LANGUAGE_TAG = "en";
    private static final String NORWEGIAN_LANGUAGE_TAG = "nb";

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final CurrentLoginProviderResolver currentLoginProviderResolver = Mockito.mock(CurrentLoginProviderResolver.class);
    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final UserSettingsService userSettingsService = Mockito.mock(UserSettingsService.class);
    private final LocalUserService localUserService = Mockito.mock(LocalUserService.class);
    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final ProfilePictureStorageService profilePictureStorageService = Mockito.mock(ProfilePictureStorageService.class);
    private final ClientRegistrationRepository clientRegistrationRepository = Mockito.mock(ClientRegistrationRepository.class);
    private final UserLoginTypeService userLoginTypeService = Mockito.mock(UserLoginTypeService.class);
    private final ExternalIdentityLinkSession externalIdentityLinkSession = Mockito.mock(ExternalIdentityLinkSession.class);
    private final CurrentUserSession currentUserSession = Mockito.mock(CurrentUserSession.class);
    private final ProfileSetupRequirementService profileSetupRequirementService =
            Mockito.mock(ProfileSetupRequirementService.class);
    private final NavigationPageModel navigationPageModel = Mockito.mock(NavigationPageModel.class);
    private final LocalizationService localizationService = localizationService();
    private final ProfilePageModel profilePageModel = new ProfilePageModel(localizationService);
    private final ProfileLoginTypesPageModel profileLoginTypesPageModel =
            new ProfileLoginTypesPageModel(
                    clientRegistrationRepository,
                    userLoginTypeService,
                    externalIdentityLinkSession,
                    localizationService);
    private final UserProfileController profileController =
            new UserProfileController(
                    currentUserIdResolver,
                    currentUserSession,
                    profileSetupRequirementService,
                    navigationPageModel,
                    userProfileService,
                    profileSetupSession,
                    profilePictureStorageService,
                    profilePageModel,
                    localizationService,
                    userSettingsService);
    private final UserProfilePasswordController passwordController =
            new UserProfilePasswordController(
                    currentUserIdResolver,
                    currentUserSession,
                    profileSetupRequirementService,
                    navigationPageModel,
                    localUserService,
                    profileSetupSession,
                    localizationService);
    private final UserProfileLoginTypesController loginTypesController =
            new UserProfileLoginTypesController(
                    currentUserIdResolver,
                    currentUserSession,
                    profileSetupRequirementService,
                    navigationPageModel,
                    currentLoginProviderResolver,
                    localUserService,
                    profileSetupSession,
                    userLoginTypeService,
                    externalIdentityLinkSession,
                    profileLoginTypesPageModel,
                    localizationService);
    private final Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(TEST_LOCALE);
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
        when(currentUserSession.currentUser(Mockito.any())).thenReturn(Optional.of(externalCurrentUser()));
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
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
        assertThat(model.containsAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
        assertThat(model.getAttribute(PROFILE_PICTURE_ATTRIBUTE)).isEqualTo(PICTURE_URL);
        assertThat(model.containsAttribute(LANGUAGE_OPTIONS_ATTRIBUTE)).isTrue();
        assertThat(model.getAttribute(PROFILE_FORM_ATTRIBUTE))
                .isEqualTo(new UserProfileUpdateDto(DISPLAY_NAME, PICTURE_URL));
    }

    @Test
    void showsLocalLoginNameAndPasswordFormForLocalProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(localCurrentUser()));

        final String view = profileController.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void hidesPasswordChangeDuringProfileSetup() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(profileSetupSession.isProfileSetupRequired(request)).thenReturn(true);
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(setupCurrentUser()));

        final String view = profileController.profile(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(LOCAL_PROFILE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.getAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(PASSWORD_FORM_ATTRIBUTE)).isFalse();
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void showsPasswordPageForLocalProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(localCurrentUser()));

        final String view = passwordController.password(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_PASSWORD_VIEW);
        assertThat(model.getAttribute(PASSWORD_FORM_ATTRIBUTE)).isEqualTo(new UserPasswordChangeDto(null, null, null));
    }

    @Test
    void showsLoginTypesPage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        assertThat(model.getAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isEqualTo(List.of(GOOGLE_LOGIN_TYPE_OPTION));
        assertThat(model.getAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isEqualTo(List.of(GOOGLE_LINKED_LOGIN_TYPE_OPTION));
        assertThat(model.getAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)).isFalse();
    }

    @Test
    void showsLoginTypesPageDuringProfileSetupWithoutLinkActions() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(setupCurrentUser()));

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        assertThat(model.getAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE)).isEqualTo(List.of());
        assertThat(model.getAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        final List<ProfileLinkedLoginTypeOptionDto> loginTypeOptions = linkedLoginTypeOptions(model);
        assertThat(loginTypeOptions)
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.providerId()).isEqualTo(GOOGLE_PROVIDER_ID);
                    assertThat(option.unlinkAvailable()).isFalse();
                });
    }

    @Test
    void marksLinkedLocalLoginTypeForTemplateLocalization() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(NORWEGIAN_LANGUAGE_TAG));
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userLoginTypeService.linkedLoginTypes(USER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID)))
                .thenReturn(List.of(new LinkedLoginType(
                        LocalUserService.LOCAL_PROVIDER_ID,
                        true,
                        LoginTypeUnlinkState.CURRENT_LOGIN)));
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        final List<ProfileLinkedLoginTypeOptionDto> loginTypeOptions = linkedLoginTypeOptions(model);
        assertThat(loginTypeOptions)
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.localLoginType()).isTrue();
                    assertThat(option.label()).isEqualTo(NORWEGIAN_LOCAL_LOGIN_TYPE_LABEL);
                });
    }

    @Test
    void showsLoginTypesPageWithExternalLinkSuccessMessage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(externalIdentityLinkSession.consumeSuccess(request)).thenReturn(true);

        final String view = loginTypesController.loginTypes(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_VIEW);
        assertThat(model.getAttribute(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE)).isEqualTo(LOGIN_TYPE_LINKED_MESSAGE);
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
                .isEqualTo(PROVIDER_ALREADY_LINKED_MESSAGE);
    }

    @Test
    void showsLocalLoginTypePage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userLoginTypeService.canLinkLocalLoginType(USER_ID)).thenReturn(true);
        final Model model = new ExtendedModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.localLoginType(authentication, model, request);

        assertThat(view).isEqualTo(PROFILE_LOCAL_LOGIN_TYPE_VIEW);
        assertThat(model.getAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE))
                .isEqualTo(new UserLocalLoginTypeLinkDto(null, null, null));
    }

    @Test
    void updatesCurrentProfileAndKeepsCurrentPictureWhenNoPictureIsUploaded() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdateDto profileForm = new UserProfileUpdateDto(DISPLAY_NAME, null);
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
        verify(userSettingsService).updateLanguage(USER_ID, UserLanguage.ENGLISH);
    }

    @Test
    void updatesCurrentProfileWithUploadedPictureUrl() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdateDto profileForm = new UserProfileUpdateDto(DISPLAY_NAME, null);
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
        verify(userSettingsService).updateLanguage(USER_ID, UserLanguage.ENGLISH);
    }

    @Test
    void updatesCurrentProfileLanguage() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdateDto profileForm =
                new UserProfileUpdateDto(DISPLAY_NAME, null, NORWEGIAN_LANGUAGE_TAG);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockMultipartFile pictureFile = emptyPictureFile();

        final String view = profileController.updateProfile(
                authentication, profileForm, bindingResult, model, redirectAttributes, request, pictureFile);

        assertThat(view).isEqualTo(PROFILE_REDIRECT);
        verify(userSettingsService).updateLanguage(USER_ID, UserLanguage.NORWEGIAN);
    }

    @Test
    void doesNotUpdateWhenPictureUploadFails() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdateDto profileForm = new UserProfileUpdateDto(DISPLAY_NAME, null);
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
                .isEqualTo(INVALID_PROFILE_PICTURE_MESSAGE);
        verify(userProfileService, never()).updateProfile(Mockito.any(), Mockito.any());
    }

    @Test
    void redirectsToRememberLoginAfterRequiredProfileSetup() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final UserProfileUpdateDto profileForm = new UserProfileUpdateDto(DISPLAY_NAME, null);
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
        final UserProfileUpdateDto profileForm = new UserProfileUpdateDto("", PICTURE_URL);
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
        final UserPasswordChangeDto passwordForm = new UserPasswordChangeDto(CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        final BindingResult bindingResult = new BeanPropertyBindingResult(passwordForm, PASSWORD_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(localCurrentUser()));

        final String view = passwordController.changePassword(
                authentication, passwordForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(PASSWORD_SAVED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        verify(localUserService).changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);
    }

    @Test
    void doesNotChangePasswordWhenConfirmationDoesNotMatch() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final UserPasswordChangeDto passwordForm = new UserPasswordChangeDto(CURRENT_PASSWORD, NEW_PASSWORD, "DifferentPassword1");
        final BindingResult bindingResult = new BeanPropertyBindingResult(passwordForm, PASSWORD_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(localCurrentUser()));

        final String view = passwordController.changePassword(
                authentication, passwordForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_PASSWORD_VIEW);
        assertThat(bindingResult.getFieldError("newPasswordConfirmation").getDefaultMessage())
                .isEqualTo(PASSWORD_MISMATCH_MESSAGE);
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
        final UserLocalLoginTypeLinkDto form = new UserLocalLoginTypeLinkDto(EMAIL, NEW_PASSWORD, NEW_PASSWORD);
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.linkLocalLoginType(
                authentication, form, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_LOGIN_TYPES_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE))
                .isEqualTo(LOCAL_LOGIN_TYPE_LINKED_MESSAGE);
        verify(localUserService).linkLogin(USER_ID, new LocalUserRegistration(EMAIL, NEW_PASSWORD));
    }

    @Test
    void doesNotLinkLocalLoginTypeWhenConfirmationDoesNotMatch() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        when(userLoginTypeService.canLinkLocalLoginType(USER_ID)).thenReturn(true);
        final UserLocalLoginTypeLinkDto form = new UserLocalLoginTypeLinkDto(EMAIL, NEW_PASSWORD, "DifferentPassword1");
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = loginTypesController.linkLocalLoginType(
                authentication, form, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_LOCAL_LOGIN_TYPE_VIEW);
        assertThat(bindingResult.getFieldError("passwordConfirmation").getDefaultMessage())
                .isEqualTo(PASSWORD_MISMATCH_MESSAGE);
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
                .isEqualTo(LOGIN_TYPE_UNLINKED_MESSAGE);
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
        assertThat(redirectAttributes.getFlashAttributes().get(LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE))
                .isEqualTo(UNLINKED_LOGIN_TYPE_MESSAGE);
    }

    private static LocalizationService localizationService() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("language.option.en", TEST_LOCALE, "English");
        messageSource.addMessage("language.option.nb", TEST_LOCALE, "Norwegian");
        messageSource.addMessage("profile.picture.error.empty", TEST_LOCALE, "Choose a profile picture to upload");
        messageSource.addMessage("profile.picture.error.file-too-large", TEST_LOCALE, "Profile picture is too large");
        messageSource.addMessage("profile.picture.error.invalid-image", TEST_LOCALE, INVALID_PROFILE_PICTURE_MESSAGE);
        messageSource.addMessage(
                "profile.picture.error.dimensions-too-large",
                TEST_LOCALE,
                "Profile picture dimensions are too large");
        messageSource.addMessage("profile.picture.error.upload-failed", TEST_LOCALE, "Could not upload profile picture");
        messageSource.addMessage(
                "profile.change-password.error.password-mismatch",
                TEST_LOCALE,
                PASSWORD_MISMATCH_MESSAGE);
        messageSource.addMessage(
                "profile.change-password.error.bad-current-password",
                TEST_LOCALE,
                "Current password is incorrect");
        messageSource.addMessage(
                "profile.change-password.access.local-required",
                TEST_LOCALE,
                "An established local account is required to change password");
        messageSource.addMessage("profile.language.error.unsupported", TEST_LOCALE, "Choose a supported language");
        messageSource.addMessage("profile.login-types.local.label", TEST_LOCALE, LOCAL_LOGIN_TYPE_LABEL);
        messageSource.addMessage(
                "profile.login-types.local.label",
                Locale.forLanguageTag(NORWEGIAN_LANGUAGE_TAG),
                NORWEGIAN_LOCAL_LOGIN_TYPE_LABEL);
        messageSource.addMessage("profile.login-types.action.link", TEST_LOCALE, "Link");
        messageSource.addMessage("profile.login-types.action.unlink", TEST_LOCALE, "Unlink");
        messageSource.addMessage("profile.login-types.action.unlink", Locale.forLanguageTag(NORWEGIAN_LANGUAGE_TAG), "Koble fra");
        messageSource.addMessage("profile.login-types.confirm.unlink-title", TEST_LOCALE, "Unlink {0}?");
        messageSource.addMessage(
                "profile.login-types.confirm.unlink-title",
                Locale.forLanguageTag(NORWEGIAN_LANGUAGE_TAG),
                "Koble fra {0}?");
        messageSource.addMessage("profile.login-types.status.available", TEST_LOCALE, "");
        messageSource.addMessage("profile.login-types.status.current-login", TEST_LOCALE, "Current login");
        messageSource.addMessage(
                "profile.login-types.status.current-login",
                Locale.forLanguageTag(NORWEGIAN_LANGUAGE_TAG),
                "Nåværende innlogging");
        messageSource.addMessage("profile.login-types.status.last-login-type", TEST_LOCALE, "Only login type");
        messageSource.addMessage("profile.login-types.status.unknown-current-login", TEST_LOCALE, "Log in again to unlink");
        messageSource.addMessage("profile.login-types.message.linked", TEST_LOCALE, LOGIN_TYPE_LINKED_MESSAGE);
        messageSource.addMessage("profile.login-types.message.local-linked", TEST_LOCALE, LOCAL_LOGIN_TYPE_LINKED_MESSAGE);
        messageSource.addMessage("profile.login-types.message.unlinked", TEST_LOCALE, LOGIN_TYPE_UNLINKED_MESSAGE);
        messageSource.addMessage(
                "profile.login-types.error.provider-mismatch",
                TEST_LOCALE,
                "Could not link the selected login type. Try again.");
        messageSource.addMessage(
                "profile.login-types.error.external-identity-used",
                TEST_LOCALE,
                "That login type is already linked to another account");
        messageSource.addMessage(
                "profile.login-types.error.provider-already-linked",
                TEST_LOCALE,
                PROVIDER_ALREADY_LINKED_MESSAGE);
        messageSource.addMessage("profile.login-types.error.password-mismatch", TEST_LOCALE, PASSWORD_MISMATCH_MESSAGE);
        messageSource.addMessage(
                "profile.login-types.error.local-email-used",
                TEST_LOCALE,
                "That email is already linked to another account");
        messageSource.addMessage(
                "profile.login-types.error.unlink-current-login",
                TEST_LOCALE,
                "You cannot unlink the login type used for this session");
        messageSource.addMessage(
                "profile.login-types.error.unlink-last-login-type",
                TEST_LOCALE,
                "At least one login type must remain linked");
        messageSource.addMessage(
                "profile.login-types.error.unlink-unknown-current-login",
                TEST_LOCALE,
                "Log in again before unlinking login types");
        messageSource.addMessage("profile.login-types.error.unlink-not-linked", TEST_LOCALE, UNLINKED_LOGIN_TYPE_MESSAGE);
        messageSource.addMessage(
                "profile.login-types.access.link-requires-complete-profile",
                TEST_LOCALE,
                "A complete profile is required to link login types");
        messageSource.addMessage(
                "profile.login-types.access.unlink-requires-complete-profile",
                TEST_LOCALE,
                "A complete profile is required to unlink login types");
        messageSource.addMessage(
                "profile.login-types.access.local-login-unavailable",
                TEST_LOCALE,
                "Email and password login is not available to link");
        messageSource.addMessage(
                "profile.login-types.access.external-login-unavailable",
                TEST_LOCALE,
                "Login type is not available to link");
        return new LocalizationService(messageSource);
    }

    @SuppressWarnings("unchecked")
    private static List<ProfileLinkedLoginTypeOptionDto> linkedLoginTypeOptions(@NonNull Model model) {
        return (List<ProfileLinkedLoginTypeOptionDto>) model.getAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE);
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

    private CurrentUser externalCurrentUser() {
        return new CurrentUser(USER_ID, DISPLAY_NAME, PICTURE_URL, false, null, true);
    }

    private CurrentUser localCurrentUser() {
        return new CurrentUser(USER_ID, DISPLAY_NAME, PICTURE_URL, false, EMAIL, false);
    }

    private CurrentUser setupCurrentUser() {
        return new CurrentUser(USER_ID, DISPLAY_NAME, PICTURE_URL, true, EMAIL, true);
    }
}
