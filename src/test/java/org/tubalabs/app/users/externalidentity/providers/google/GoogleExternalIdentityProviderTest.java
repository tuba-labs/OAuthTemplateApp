package org.tubalabs.app.users.externalidentity.providers.google;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleExternalIdentityProviderTest {

    private static final String SUBJECT = "google-subject";
    private static final String NAME = "Person Name";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String TOKEN_VALUE = "token";
    private static final Instant ISSUED_AT = Instant.parse("2026-05-23T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-23T11:00:00Z");

    private final GoogleExternalIdentityProvider provider = new GoogleExternalIdentityProvider();

    @Test
    void mapsOidcUserToExternalIdentity() {
        final OidcUser oidcUser = googleUser();

        final ExternalIdentity identity = provider.getIdentity(oidcUser);

        assertThat(identity.providerId()).isEqualTo(GoogleExternalIdentityProvider.PROVIDER_ID);
        assertThat(identity.subject()).isEqualTo(SUBJECT);
        assertThat(identity.displayName()).isEqualTo(NAME);
        assertThat(identity.email()).isEqualTo(EMAIL);
        assertThat(identity.avatarUrl()).isEqualTo(PICTURE_URL);
    }

    private OidcUser googleUser() {
        final Map<String, Object> claims = Map.of(
                "sub", SUBJECT,
                "name", NAME,
                "email", EMAIL,
                "picture", PICTURE_URL);
        final OidcIdToken idToken = new OidcIdToken(TOKEN_VALUE, ISSUED_AT, EXPIRES_AT, claims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
    }
}
