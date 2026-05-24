package org.tubalabs.app.users;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CurrentUserIdResolverTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "test-provider";
    private static final String SUBJECT = "provider-subject";

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver = Mockito.mock(CurrentLoginIdentityResolver.class);
    private final CurrentUserIdResolver resolver = new CurrentUserIdResolver(currentLoginIdentityResolver);
    private final Authentication authentication = Mockito.mock(Authentication.class);

    @Test
    void resolvesUserIdFromCurrentIdentity() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.of(identity()));

        final UUID userId = resolver.requireUserId(authentication);

        assertThat(userId).isEqualTo(USER_ID);
    }

    @Test
    void rejectsAnonymousAuthentication() {
        final Authentication authentication = new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> resolver.requireUserId(authentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsWhenCurrentIdentityIsUnknown() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.requireUserId(authentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    private UserIdentityDbo identity() {
        return UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .build();
    }
}
