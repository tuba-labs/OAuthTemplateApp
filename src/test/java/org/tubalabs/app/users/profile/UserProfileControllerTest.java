package org.tubalabs.app.users.profile;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.List;
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
    private static final String PROFILE_EMAIL_ATTRIBUTE = "profileEmail";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final UserProfileController controller =
            new UserProfileController(currentUserIdResolver, userProfileService, profileSetupSession);
    private final Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());

    @Test
    void showsCurrentProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile());
        final Model model = new ExtendedModelMap();

        final String view = controller.profile(authentication, model);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(PROFILE_EMAIL_ATTRIBUTE)).isEqualTo(EMAIL);
        assertThat(model.getAttribute(PROFILE_FORM_ATTRIBUTE))
                .isEqualTo(new UserProfileUpdate(DISPLAY_NAME, PICTURE_URL));
    }

    @Test
    void updatesCurrentProfile() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final UserProfileUpdate profileForm = new UserProfileUpdate(DISPLAY_NAME, PICTURE_URL);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final String view = controller.updateProfile(authentication, profileForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_REDIRECT);
        assertThat(redirectAttributes.getFlashAttributes().get(PROFILE_SAVED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        verify(userProfileService).updateProfile(USER_ID, profileForm);
    }

    @Test
    void redirectsToRememberLoginAfterRequiredProfileSetup() {
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        final UserProfileUpdate profileForm = new UserProfileUpdate(DISPLAY_NAME, PICTURE_URL);
        final BindingResult bindingResult = new BeanPropertyBindingResult(profileForm, PROFILE_FORM_ATTRIBUTE);
        final Model model = new ExtendedModelMap();
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(profileSetupSession.isProfileSetupRequired(request)).thenReturn(true);

        final String view = controller.updateProfile(authentication, profileForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(REMEMBER_LOGIN_REDIRECT);
        verify(userProfileService).updateProfile(USER_ID, profileForm);
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

        final String view = controller.updateProfile(authentication, profileForm, bindingResult, model, redirectAttributes, request);

        assertThat(view).isEqualTo(PROFILE_VIEW);
        assertThat(model.getAttribute(PROFILE_EMAIL_ATTRIBUTE)).isEqualTo(EMAIL);
        verify(userProfileService, never()).updateProfile(Mockito.any(), Mockito.any());
        verify(profileSetupSession, never()).completeProfileSetup(Mockito.any());
    }

    private UserProfileDbo profile() {
        return UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(DISPLAY_NAME)
                .email(EMAIL)
                .pictureUrl(PICTURE_URL)
                .build();
    }
}
