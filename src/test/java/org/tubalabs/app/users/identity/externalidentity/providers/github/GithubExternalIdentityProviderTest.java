package org.tubalabs.app.users.identity.externalidentity.providers.github;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.externalidentity.providers.github.GithubExternalIdentityProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GithubExternalIdentityProviderTest {

    private static final String SUBJECT = "12345";
    private static final String NAME = "Person Name";
    private static final String LOGIN = "person-login";
    private static final String EMAIL = "person@example.com";
    private static final String AVATAR_URL = "https://example.com/avatar.png";

    private final GithubExternalIdentityProvider provider = new GithubExternalIdentityProvider();

    @Test
    void mapsGithubAttributesToExternalIdentity() {
        final OAuth2User oauth2User = githubUser(Map.of(
                "id", SUBJECT,
                "name", NAME,
                "login", LOGIN,
                "email", EMAIL,
                "avatar_url", AVATAR_URL));

        final ExternalIdentity identity = provider.getIdentity(oauth2User);

        assertThat(identity.providerId()).isEqualTo(GithubExternalIdentityProvider.PROVIDER_ID);
        assertThat(identity.subject()).isEqualTo(SUBJECT);
        assertThat(identity.displayName()).isEqualTo(NAME);
        assertThat(identity.email()).isEqualTo(EMAIL);
        assertThat(identity.avatarUrl()).isEqualTo(AVATAR_URL);
    }

    @Test
    void fallsBackToLoginWhenNameIsBlank() {
        final OAuth2User oauth2User = githubUser(Map.of(
                "id", SUBJECT,
                "name", " ",
                "login", LOGIN));

        final ExternalIdentity identity = provider.getIdentity(oauth2User);

        assertThat(identity.displayName()).isEqualTo(LOGIN);
    }

    private OAuth2User githubUser(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id");
    }
}
