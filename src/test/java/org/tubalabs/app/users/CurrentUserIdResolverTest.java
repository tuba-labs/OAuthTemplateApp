package org.tubalabs.app.users;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProviders;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.password.LocalEmailNormalizer;
import org.tubalabs.app.users.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.password.db.UserPasswordCredentialRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CurrentUserIdResolverTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "person@example.com";
    private static final String MIXED_CASE_EMAIL = " Person@Example.COM ";
    private static final String PROVIDER_ID = "test-provider";
    private static final String SUBJECT = "provider-subject";
    private static final String DISPLAY_NAME = "Person";

    private final LocalEmailNormalizer emailNormalizer = new LocalEmailNormalizer();
    private final UserPasswordCredentialRepository userPasswordCredentialRepository =
            Mockito.mock(UserPasswordCredentialRepository.class);
    private final UserIdentityRepository userIdentityRepository = Mockito.mock(UserIdentityRepository.class);
    private final ExternalIdentityProviders externalIdentityProviders =
            new ExternalIdentityProviders(List.of(new TestExternalIdentityProvider()));
    private final CurrentUserIdResolver resolver = new CurrentUserIdResolver(
            emailNormalizer, userPasswordCredentialRepository, externalIdentityProviders, userIdentityRepository);

    @Test
    void resolvesLocalUserIdFromPasswordCredential() {
        when(userPasswordCredentialRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(new UserPasswordCredentialDbo(USER_ID, EMAIL, "hash")));
        final UserDetails userDetails = User.withUsername(MIXED_CASE_EMAIL)
                .password("hash")
                .authorities("ROLE_USER")
                .build();
        final Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());

        final UUID userId = resolver.requireUserId(authentication);

        assertThat(userId).isEqualTo(USER_ID);
    }

    @Test
    void resolvesOauth2UserIdFromProviderSubject() {
        when(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT))
                .thenReturn(Optional.of(UserIdentityDbo.builder()
                        .id(IDENTITY_ID)
                        .userId(USER_ID)
                        .providerId(PROVIDER_ID)
                        .subject(SUBJECT)
                        .displayName(DISPLAY_NAME)
                        .build()));
        final OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", SUBJECT),
                "sub");
        final Authentication authentication =
                new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), PROVIDER_ID);

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

    private record TestExternalIdentityProvider() implements ExternalIdentityProvider {

        @Override
        public String providerId() {
            return PROVIDER_ID;
        }

        @Override
        public ExternalIdentity getIdentity(OAuth2User oauth2User) {
            return ExternalIdentity.minimal(PROVIDER_ID, oauth2User.getAttribute("sub"), DISPLAY_NAME);
        }
    }
}
