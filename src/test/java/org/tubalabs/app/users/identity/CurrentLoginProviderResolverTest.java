package org.tubalabs.app.users.identity;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CurrentLoginProviderResolverTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver = Mockito.mock(CurrentLoginIdentityResolver.class);
    private final CurrentLoginProviderResolver resolver = new CurrentLoginProviderResolver(currentLoginIdentityResolver);
    private final Authentication authentication = Mockito.mock(Authentication.class);

    @Test
    void resolvesProviderFromCurrentIdentity() {
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.of(identity()));

        assertThat(resolver.providerId(authentication)).contains(PROVIDER_ID);
    }

    @Test
    void returnsEmptyWhenCurrentIdentityIsUnknown() {
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.empty());

        assertThat(resolver.providerId(authentication)).isEmpty();
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
