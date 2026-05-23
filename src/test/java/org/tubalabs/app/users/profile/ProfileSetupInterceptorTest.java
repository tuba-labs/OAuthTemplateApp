package org.tubalabs.app.users.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.tubalabs.app.users.CurrentUserIdResolver;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ProfileSetupInterceptorTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final ProfileSetupRequirementService profileSetupRequirementService = Mockito.mock(ProfileSetupRequirementService.class);
    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final ProfileSetupInterceptor interceptor =
            new ProfileSetupInterceptor(profileSetupSession, profileSetupRequirementService, currentUserIdResolver);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestsWhenUserIsNotAuthenticated() throws Exception {
        final MockHttpServletRequest request = request("/");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void allowsRequestsWhenProfileSetupIsNotRequired() throws Exception {
        final MockHttpServletRequest request = request("/");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final Authentication authentication = authenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(profileSetupRequirementService.isSetupRequiredForSession(request, USER_ID)).thenReturn(false);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void redirectsHomeRequestsToProfileWhenProfileSetupIsRequired() throws Exception {
        final MockHttpServletRequest request = request("/");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final Authentication authentication = authenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(profileSetupRequirementService.isSetupRequiredForSession(request, USER_ID)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/profile");
    }

    @Test
    void allowsProfileRequestsWhenProfileSetupIsRequired() throws Exception {
        final MockHttpServletRequest request = request("/profile");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    private Authentication authenticatedUser() {
        return UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of());
    }

    private MockHttpServletRequest request(String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }
}
