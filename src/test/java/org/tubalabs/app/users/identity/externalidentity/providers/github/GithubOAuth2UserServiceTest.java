package org.tubalabs.app.users.identity.externalidentity.providers.github;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubOAuth2UserServiceTest {

    private static final String GITHUB_PROVIDER_ID = "github";
    private static final String GOOGLE_PROVIDER_ID = "google";
    private static final String SUBJECT = "12345";
    private static final String EMAIL = "person@example.com";
    private static final String PUBLIC_EMAIL = "public@example.com";
    private static final String TOKEN_VALUE = "token";
    private static final Instant ISSUED_AT = Instant.parse("2026-05-29T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-29T11:00:00Z");

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = mockOAuth2UserService();
    private final GithubEmailClient githubEmailClient = Mockito.mock(GithubEmailClient.class);
    private final GithubOAuth2UserService githubOAuth2UserService =
            new GithubOAuth2UserService(delegate, githubEmailClient);

    @Test
    void addsPrivateGithubEmailWhenProfileEmailIsMissing() {
        final OAuth2UserRequest userRequest = userRequest(GITHUB_PROVIDER_ID);
        final OAuth2User oauth2User = oauth2User(Map.of("id", SUBJECT));
        when(delegate.loadUser(userRequest)).thenReturn(oauth2User);
        when(githubEmailClient.primaryEmail(TOKEN_VALUE)).thenReturn(Optional.of(EMAIL));

        final OAuth2User loadedUser = githubOAuth2UserService.loadUser(userRequest);

        assertThat(loadedUser.getAttributes().get("email")).isEqualTo(EMAIL);
    }

    @Test
    void keepsPublicGithubEmailWhenProfileEmailExists() {
        final OAuth2UserRequest userRequest = userRequest(GITHUB_PROVIDER_ID);
        final OAuth2User oauth2User = oauth2User(Map.of(
                "id", SUBJECT,
                "email", PUBLIC_EMAIL));
        when(delegate.loadUser(userRequest)).thenReturn(oauth2User);

        final OAuth2User loadedUser = githubOAuth2UserService.loadUser(userRequest);

        assertThat(loadedUser).isSameAs(oauth2User);
        verify(githubEmailClient, never()).primaryEmail(TOKEN_VALUE);
    }

    @Test
    void ignoresNonGithubProviders() {
        final OAuth2UserRequest userRequest = userRequest(GOOGLE_PROVIDER_ID);
        final OAuth2User oauth2User = oauth2User(Map.of("id", SUBJECT));
        when(delegate.loadUser(userRequest)).thenReturn(oauth2User);

        final OAuth2User loadedUser = githubOAuth2UserService.loadUser(userRequest);

        assertThat(loadedUser).isSameAs(oauth2User);
        verify(githubEmailClient, never()).primaryEmail(TOKEN_VALUE);
    }

    private OAuth2UserRequest userRequest(String registrationId) {
        return new OAuth2UserRequest(
                clientRegistration(registrationId),
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        TOKEN_VALUE,
                        ISSUED_AT,
                        EXPIRES_AT));
    }

    private ClientRegistration clientRegistration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientName(registrationId)
                .clientId("client-" + registrationId)
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/" + registrationId)
                .authorizationUri("https://example.com/" + registrationId + "/authorize")
                .tokenUri("https://example.com/" + registrationId + "/token")
                .userInfoUri("https://example.com/" + registrationId + "/user")
                .userNameAttributeName("id")
                .build();
    }

    private OAuth2User oauth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id");
    }

    @SuppressWarnings("unchecked")
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> mockOAuth2UserService() {
        return Mockito.mock(OAuth2UserService.class);
    }
}
