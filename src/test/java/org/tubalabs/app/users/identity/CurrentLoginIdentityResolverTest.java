package org.tubalabs.app.users.identity;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.tubalabs.app.security.remember.RememberedLoginName;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProviders;
import org.tubalabs.app.users.identity.password.LocalEmailNormalizer;
import org.tubalabs.app.users.identity.password.LocalUserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CurrentLoginIdentityResolverTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "person@example.com";
    private static final String MIXED_CASE_EMAIL = " Person@Example.COM ";
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String DISPLAY_NAME = "Person";
    private static final String REMEMBER_ME_KEY = "remember-me-key";

    private final UserIdentityRepository userIdentityRepository = Mockito.mock(UserIdentityRepository.class);
    private final ExternalIdentityProviders externalIdentityProviders =
            new ExternalIdentityProviders(List.of(new TestExternalIdentityProvider()));
    private final CurrentLoginIdentityResolver resolver = new CurrentLoginIdentityResolver(
            new LocalEmailNormalizer(), userIdentityRepository, externalIdentityProviders);

    @Test
    void resolvesLocalIdentityFromEmail() {
        final UserIdentityDbo identity = identity(LocalUserService.LOCAL_PROVIDER_ID, EMAIL);
        when(userIdentityRepository.findByProviderAndSubject(LocalUserService.LOCAL_PROVIDER_ID, EMAIL))
                .thenReturn(Optional.of(identity));
        final UserDetails userDetails = User.withUsername(MIXED_CASE_EMAIL)
                .password("hash")
                .authorities("ROLE_USER")
                .build();
        final Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());

        assertThat(resolver.identity(authentication)).contains(identity);
    }

    @Test
    void resolvesOauthIdentityFromProviderSubject() {
        final UserIdentityDbo identity = identity(PROVIDER_ID, SUBJECT);
        when(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT))
                .thenReturn(Optional.of(identity));
        final OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", SUBJECT),
                "sub");
        final Authentication authentication =
                new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), PROVIDER_ID);

        assertThat(resolver.identity(authentication)).contains(identity);
    }

    @Test
    void resolvesRememberedIdentityFromIdentityId() {
        final UserIdentityDbo identity = identity(PROVIDER_ID, SUBJECT);
        when(userIdentityRepository.findById(IDENTITY_ID)).thenReturn(Optional.of(identity));
        final UserDetails userDetails = User.withUsername(RememberedLoginName.username(IDENTITY_ID))
                .password("remembered")
                .authorities("ROLE_USER")
                .build();
        final Authentication authentication = new RememberMeAuthenticationToken(
                REMEMBER_ME_KEY, userDetails, userDetails.getAuthorities());

        assertThat(resolver.identity(authentication)).contains(identity);
    }

    @Test
    void rejectsAnonymousAuthentication() {
        final Authentication authentication = new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(resolver.identity(authentication)).isEmpty();
    }

    private UserIdentityDbo identity(String providerId, String subject) {
        return UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(providerId)
                .subject(subject)
                .displayName(DISPLAY_NAME)
                .build();
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
