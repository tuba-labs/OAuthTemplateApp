package org.tubalabs.app.users.externalidentity.providers.security;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.tubalabs.app.security.SecurityConfig;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.UserService;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProviders;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class Oauth2SecurityCustomizer {

    private final UserService userService;
    private final ExternalIdentityProviders externalIdentityProviders;

    @Bean
    public SecurityConfig.HttpSecurityCustomizer oauth2HttpSecurityCustomizer(
            @Qualifier("oauthAuthenticationSuccessHandler") @NonNull AuthenticationSuccessHandler handler) {
        return http -> http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/authorization/**")
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
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
            final String clientIp = request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
            final String userAgent = Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
            final LoginResult result = userService.login(identity, clientIp, userAgent);

            log.info("Logged in user: {}", result);

            response.sendRedirect("/");
        };
    }
}
