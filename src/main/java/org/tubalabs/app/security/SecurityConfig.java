package org.tubalabs.app.security;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.tubalabs.app.security.identity.ExternalIdentity;
import org.tubalabs.app.security.identity.ExternalIdentityProvider;
import org.tubalabs.app.security.identity.ExternalIdentityProviders;
import org.tubalabs.app.users.db.LoginResult;
import org.tubalabs.app.users.db.UserService;

import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final ExternalIdentityProviders externalIdentityProviders;

    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http) {

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(oauth -> oauth.successHandler(authenticationSuccessHandler()))
                .logout(Customizer.withDefaults())
                .build();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {

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