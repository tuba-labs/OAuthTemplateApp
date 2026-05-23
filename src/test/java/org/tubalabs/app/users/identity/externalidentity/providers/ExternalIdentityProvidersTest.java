package org.tubalabs.app.users.identity.externalidentity.providers;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProviders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalIdentityProvidersTest {

    private static final String PROVIDER_ID = "provider";
    private static final String UNKNOWN_PROVIDER_ID = "missing";

    @Test
    void returnsProviderById() {
        final ExternalIdentityProvider provider = new TestExternalIdentityProvider(PROVIDER_ID);
        final ExternalIdentityProviders providers = new ExternalIdentityProviders(List.of(provider));

        assertThat(providers.getProvider(PROVIDER_ID)).isSameAs(provider);
    }

    @Test
    void rejectsUnsupportedProviderId() {
        final ExternalIdentityProviders providers = new ExternalIdentityProviders(List.of());

        assertThatThrownBy(() -> providers.getProvider(UNKNOWN_PROVIDER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    private record TestExternalIdentityProvider(String providerId) implements ExternalIdentityProvider {

        @Override
        public ExternalIdentity getIdentity(OAuth2User oauth2User) {
            return ExternalIdentity.minimal(providerId, "subject", "Display Name");
        }
    }
}
