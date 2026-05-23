package org.tubalabs.app.navigation.api.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AuthenticatedNavigationModelAdviceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "person@example.com";
    private static final String DISPLAY_NAME = "Person Name";
    private static final String DEFAULT_PROFILE_LINK_TEXT = "Profile";

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final AuthenticatedNavigationModelAdvice advice =
            new AuthenticatedNavigationModelAdvice(currentUserIdResolver, userProfileService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsDefaultTextForAnonymousUser() {
        assertThat(advice.currentUserDisplayName()).isEqualTo(DEFAULT_PROFILE_LINK_TEXT);
    }

    @Test
    void returnsDisplayNameForAuthenticatedUser() {
        final Authentication authentication = authenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile(DISPLAY_NAME));

        assertThat(advice.currentUserDisplayName()).isEqualTo(DISPLAY_NAME);
    }

    @Test
    void returnsDefaultTextWhenDisplayNameIsBlank() {
        final Authentication authentication = authenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenReturn(profile(" "));

        assertThat(advice.currentUserDisplayName()).isEqualTo(DEFAULT_PROFILE_LINK_TEXT);
    }

    @Test
    void returnsDefaultTextWhenProfileIsMissing() {
        final Authentication authentication = authenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID)).thenThrow(new NoSuchElementException("Missing profile"));

        assertThat(advice.currentUserDisplayName()).isEqualTo(DEFAULT_PROFILE_LINK_TEXT);
    }

    private Authentication authenticatedUser() {
        return UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, java.util.List.of());
    }

    private UserProfileDbo profile(String displayName) {
        return UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(displayName)
                .build();
    }
}
