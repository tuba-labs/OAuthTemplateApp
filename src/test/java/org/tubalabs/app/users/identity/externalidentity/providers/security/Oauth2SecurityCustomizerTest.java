package org.tubalabs.app.users.identity.externalidentity.providers.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.UserService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession.PendingExternalIdentityLink;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkException;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkFailure;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProviders;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Oauth2SecurityCustomizerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String DISPLAY_NAME = "Person";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final ExternalIdentity EXTERNAL_IDENTITY = ExternalIdentity.minimal(PROVIDER_ID, SUBJECT, DISPLAY_NAME);

    private final UserService userService = Mockito.mock(UserService.class);
    private final ExternalIdentityLinkService externalIdentityLinkService = Mockito.mock(ExternalIdentityLinkService.class);
    private final ExternalIdentityLinkSession externalIdentityLinkSession = Mockito.mock(ExternalIdentityLinkSession.class);
    private final ExternalIdentityProviders externalIdentityProviders = Mockito.mock(ExternalIdentityProviders.class);
    private final ExternalIdentityProvider externalIdentityProvider = Mockito.mock(ExternalIdentityProvider.class);
    private final ProfileSetupSession profileSetupSession = Mockito.mock(ProfileSetupSession.class);
    private final ProfileSetupRequirementService profileSetupRequirementService =
            Mockito.mock(ProfileSetupRequirementService.class);
    private final Oauth2SecurityCustomizer customizer = new Oauth2SecurityCustomizer(
            userService,
            externalIdentityLinkService,
            externalIdentityLinkSession,
            externalIdentityProviders,
            profileSetupSession,
            profileSetupRequirementService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void linksPendingExternalIdentityInsteadOfLoggingIn() throws Exception {
        final Authentication originalAuthentication =
                UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of());
        final MockHttpServletRequest request = request();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final OAuth2AuthenticationToken authentication = oauthAuthentication();
        final PendingExternalIdentityLink pendingLink =
                new PendingExternalIdentityLink(USER_ID, PROVIDER_ID, originalAuthentication);
        when(externalIdentityLinkSession.pending(request)).thenReturn(Optional.of(pendingLink));
        stubProvider(authentication);

        successHandler().onAuthenticationSuccess(request, response, authentication);

        verify(externalIdentityLinkService).link(USER_ID, EXTERNAL_IDENTITY, CLIENT_IP, USER_AGENT);
        verify(externalIdentityLinkSession).complete(request);
        verifyNoInteractions(userService);
        assertThat(response.getRedirectedUrl()).isEqualTo("/profile/login-types");
    }

    @Test
    void restoresOriginalAuthenticationWhenLinkFails() throws Exception {
        final Authentication originalAuthentication =
                UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of());
        final MockHttpServletRequest request = request();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final OAuth2AuthenticationToken authentication = oauthAuthentication();
        final PendingExternalIdentityLink pendingLink =
                new PendingExternalIdentityLink(USER_ID, PROVIDER_ID, originalAuthentication);
        when(externalIdentityLinkSession.pending(request)).thenReturn(Optional.of(pendingLink));
        stubProvider(authentication);
        Mockito.doThrow(new IdentityLinkException(IdentityLinkFailure.EXTERNAL_IDENTITY_USED))
                .when(externalIdentityLinkService)
                .link(USER_ID, EXTERNAL_IDENTITY, CLIENT_IP, USER_AGENT);

        successHandler().onAuthenticationSuccess(request, response, authentication);

        verify(externalIdentityLinkSession).fail(request, IdentityLinkFailure.EXTERNAL_IDENTITY_USED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(originalAuthentication);
        assertThat(response.getRedirectedUrl()).isEqualTo("/profile/login-types");
    }

    @Test
    void restoresOriginalAuthenticationWhenLinkedProviderDoesNotMatchPendingProvider() throws Exception {
        final Authentication originalAuthentication =
                UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of());
        final MockHttpServletRequest request = request();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final OAuth2AuthenticationToken authentication = oauthAuthentication();
        final PendingExternalIdentityLink pendingLink =
                new PendingExternalIdentityLink(USER_ID, "github", originalAuthentication);
        when(externalIdentityLinkSession.pending(request)).thenReturn(Optional.of(pendingLink));
        stubProvider(authentication);

        successHandler().onAuthenticationSuccess(request, response, authentication);

        verify(externalIdentityLinkSession).fail(request, IdentityLinkFailure.PROVIDER_MISMATCH);
        verifyNoInteractions(externalIdentityLinkService, userService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(originalAuthentication);
        assertThat(response.getRedirectedUrl()).isEqualTo("/profile/login-types");
    }

    @Test
    void keepsExistingLoginFlowWhenNoLinkIsPending() throws Exception {
        final MockHttpServletRequest request = request();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final OAuth2AuthenticationToken authentication = oauthAuthentication();
        final LoginResult loginResult = LoginResult.builder()
                .identityId(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .newUser(false)
                .build();
        when(externalIdentityLinkSession.pending(request)).thenReturn(Optional.empty());
        when(userService.login(EXTERNAL_IDENTITY, CLIENT_IP, USER_AGENT)).thenReturn(loginResult);
        stubProvider(authentication);

        successHandler().onAuthenticationSuccess(request, response, authentication);

        verify(profileSetupRequirementService).requireSetupIfProfileIncomplete(request, USER_ID);
        assertThat(response.getRedirectedUrl()).isEqualTo("/remember-login");
    }

    private AuthenticationSuccessHandler successHandler() {
        return customizer.oauthAuthenticationSuccessHandler();
    }

    private MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        request.addHeader("User-Agent", USER_AGENT);
        return request;
    }

    private OAuth2AuthenticationToken oauthAuthentication() {
        final OAuth2User oauth2User = Mockito.mock(OAuth2User.class);
        when(oauth2User.getName()).thenReturn(SUBJECT);
        return new OAuth2AuthenticationToken(oauth2User, List.of(), PROVIDER_ID);
    }

    private void stubProvider(OAuth2AuthenticationToken authentication) {
        when(externalIdentityProviders.getProvider(PROVIDER_ID)).thenReturn(externalIdentityProvider);
        when(externalIdentityProvider.getIdentity(authentication.getPrincipal())).thenReturn(EXTERNAL_IDENTITY);
    }
}
