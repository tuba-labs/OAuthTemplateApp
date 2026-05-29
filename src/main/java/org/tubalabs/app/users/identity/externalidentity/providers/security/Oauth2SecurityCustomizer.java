package org.tubalabs.app.users.identity.externalidentity.providers.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.tubalabs.app.security.SecurityConfig;
import org.tubalabs.app.security.SecurityAllowedPaths;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.UserService;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession.PendingExternalIdentityLink;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkException;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkFailure;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProviders;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.ui.profile.logintypes.menusystem.ProfileLoginTypesPage;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePage;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;

import java.util.Objects;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class Oauth2SecurityCustomizer {

    private final UserService userService;
    private final ExternalIdentityLinkService externalIdentityLinkService;
    private final ExternalIdentityLinkSession externalIdentityLinkSession;
    private final ExternalIdentityProviders externalIdentityProviders;
    private final ProfileSetupSession profileSetupSession;
    private final ProfileSetupRequirementService profileSetupRequirementService;
    private final CurrentUserSession currentUserSession;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Bean
    public SecurityConfig.HttpSecurityCustomizer oauth2HttpSecurityCustomizer(
            @Qualifier("oauthAuthenticationSuccessHandler") @NonNull AuthenticationSuccessHandler handler) {
        return http -> http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityAllowedPaths.OAUTH2_PUBLIC_MATCHERS)
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage(SecurityAllowedPaths.LOGIN_PATH)
                        .successHandler(handler));

    }

    @Bean
    AuthenticationSuccessHandler oauthAuthenticationSuccessHandler() {

        return (request, response, authentication) -> {
            final OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) authentication;
            final OAuth2User oauth2User = authenticationToken.getPrincipal();
            final String providerId = authenticationToken.getAuthorizedClientRegistrationId();
            @Cleanup MDC.MDCCloseable ignore = MDC.putCloseable("providerId", providerId);
            final ExternalIdentityProvider provider = externalIdentityProviders.getProvider(providerId);
            final ExternalIdentity identity = provider.getIdentity(oauth2User);
            @Cleanup MDC.MDCCloseable ignore2 = MDC.putCloseable("subject", identity.subject());
            final String clientIp = clientIp(request);
            final String userAgent = userAgent(request);

            final Optional<PendingExternalIdentityLink> pendingLink = externalIdentityLinkSession.pending(request);
            if (pendingLink.isPresent()) {
                linkExternalIdentity(request, response, identity, providerId, clientIp, userAgent, pendingLink.orElseThrow());
                return;
            }

            final LoginResult result = userService.login(identity, clientIp, userAgent);

            log.info("Logged in user: {}", result);

            if (result.newUser()) {
                profileSetupSession.requireProfileSetup(request);
                currentUserSession.refresh(request, result.userId(), true);
                redirectTo(request, response, ProfilePage.RELATIVE_URL);
                return;
            }
            final boolean profileSetupRequired =
                    profileSetupRequirementService.requireSetupIfProfileIncomplete(request, result.userId());
            currentUserSession.refresh(request, result.userId(), profileSetupRequired);
            if (profileSetupRequired) {
                redirectTo(request, response, ProfilePage.RELATIVE_URL);
                return;
            }
            redirectTo(request, response, "/remember-login");
        };
    }

    private void linkExternalIdentity(HttpServletRequest request,
                                      HttpServletResponse response,
                                      ExternalIdentity identity,
                                      String providerId,
                                      String clientIp,
                                      String userAgent,
                                      PendingExternalIdentityLink pendingLink) throws java.io.IOException {
        if (!pendingLink.providerId().equals(providerId)) {
            restoreOriginalAuthentication(pendingLink.originalAuthentication(), request, response);
            externalIdentityLinkSession.fail(request, IdentityLinkFailure.PROVIDER_MISMATCH);
            redirectTo(request, response, ProfileLoginTypesPage.RELATIVE_URL);
            return;
        }

        try {
            externalIdentityLinkService.link(pendingLink.userId(), identity, clientIp, userAgent);
            externalIdentityLinkSession.complete(request);
            currentUserSession.refresh(request, pendingLink.userId(), profileSetupSession.isProfileSetupRequired(request));
            redirectTo(request, response, ProfileLoginTypesPage.RELATIVE_URL);
        } catch (IdentityLinkException exception) {
            restoreOriginalAuthentication(pendingLink.originalAuthentication(), request, response);
            externalIdentityLinkSession.fail(request, exception.reason());
            redirectTo(request, response, ProfileLoginTypesPage.RELATIVE_URL);
        } catch (RuntimeException exception) {
            restoreOriginalAuthentication(pendingLink.originalAuthentication(), request, response);
            throw exception;
        }
    }

    private void restoreOriginalAuthentication(Authentication authentication,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    private String clientIp(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }

    private void redirectTo(@NonNull HttpServletRequest request,
                            @NonNull HttpServletResponse response,
                            @NonNull String path) throws java.io.IOException {
        response.sendRedirect(request.getContextPath() + path);
    }
}
