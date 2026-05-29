package org.tubalabs.app.users.current;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentUserRequestContextTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final CurrentUserIdResolver currentUserIdResolver = Mockito.mock(CurrentUserIdResolver.class);
    private final CurrentUserSession currentUserSession = Mockito.mock(CurrentUserSession.class);
    private final ProfileSetupRequirementService profileSetupRequirementService =
            Mockito.mock(ProfileSetupRequirementService.class);
    private final CurrentUserRequestContext currentUserRequestContext =
            new CurrentUserRequestContext(currentUserIdResolver, currentUserSession, profileSetupRequirementService);

    @Test
    void usesCurrentUserAlreadyStoredInSession() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final Authentication authentication = authenticated();
        final CurrentUser currentUser = currentUser(false);
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(currentUser));

        final CurrentUser result = currentUserRequestContext.currentUser(request, authentication);

        assertThat(result).isSameAs(currentUser);
        verify(currentUserIdResolver, never()).requireUserId(Mockito.any());
        verify(profileSetupRequirementService, never()).isSetupRequiredForSession(Mockito.any(), Mockito.any());
        verify(currentUserSession, never()).refresh(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void refreshesCurrentUserWhenSessionDoesNotHaveOne() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final Authentication authentication = authenticated();
        final CurrentUser currentUser = currentUser(true);
        when(currentUserSession.currentUser(request)).thenReturn(Optional.empty());
        when(currentUserIdResolver.requireUserId(authentication)).thenReturn(USER_ID);
        when(profileSetupRequirementService.isSetupRequiredForSession(request, USER_ID)).thenReturn(true);
        when(currentUserSession.refresh(request, USER_ID, true)).thenReturn(currentUser);

        final CurrentUser result = currentUserRequestContext.currentUser(request, authentication);

        assertThat(result).isSameAs(currentUser);
    }

    @Test
    void rejectsAnonymousUser() {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> currentUserRequestContext.currentUser(request, anonymous()))
                .isInstanceOf(AccessDeniedException.class);
        verify(currentUserSession, never()).currentUser(Mockito.any());
    }

    @Test
    void reportsAuthenticatedState() {
        assertThat(currentUserRequestContext.authenticated(authenticated())).isTrue();
        assertThat(currentUserRequestContext.authenticated(anonymous())).isFalse();
        assertThat(currentUserRequestContext.authenticated(null)).isFalse();
    }

    private Authentication authenticated() {
        return UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());
    }

    private Authentication anonymous() {
        return new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private CurrentUser currentUser(boolean profileSetupRequired) {
        return new CurrentUser(USER_ID, "Person", null, profileSetupRequired, null, true);
    }
}
