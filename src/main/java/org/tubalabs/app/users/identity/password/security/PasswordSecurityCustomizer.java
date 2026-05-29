package org.tubalabs.app.users.identity.password.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.web.util.UriComponentsBuilder;
import org.tubalabs.app.security.SecurityConfig;
import org.tubalabs.app.security.SecurityAllowedPaths;
import org.tubalabs.app.security.remember.RememberLoginProperties;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PasswordSecurityCustomizer {

    private static final String EMAIL_PARAMETER = "email";
    private static final String ERROR_PARAMETER = "error";

    private final LocalUserService localUserService;
    private final RememberMeServices rememberMeServices;
    private final RememberLoginProperties rememberLoginProperties;
    private final ProfileSetupRequirementService profileSetupRequirementService;
    private final CurrentUserSession currentUserSession;

    @Bean
    public SecurityConfig.HttpSecurityCustomizer passwordHttpSecurityCustomizer(
            @Qualifier("passwordAuthenticationSuccessHandler") @NonNull AuthenticationSuccessHandler successHandler,
            @Qualifier("passwordAuthenticationFailureHandler") @NonNull AuthenticationFailureHandler failureHandler) {
        return http -> http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityAllowedPaths.PASSWORD_PUBLIC_MATCHERS)
                        .permitAll())
                .formLogin(form -> form
                        .loginPage(SecurityAllowedPaths.LOCAL_LOGIN_PATH)
                        .loginProcessingUrl(SecurityAllowedPaths.LOCAL_LOGIN_PATH)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .rememberMe(rememberMe -> rememberMe
                        .key(rememberLoginProperties.rememberMeKey())
                        .rememberMeServices(rememberMeServices));
    }

    @Bean
    AuthenticationSuccessHandler passwordAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            final LoginResult result = localUserService.login(
                    authentication.getName(), clientIp(request), userAgent(request));
            log.info("Logged in user with password: {}", result);

            final boolean profileSetupRequired =
                    profileSetupRequirementService.requireSetupIfProfileIncomplete(request, result.userId());
            currentUserSession.refresh(request, result.userId(), profileSetupRequired);
            if (profileSetupRequired) {
                response.sendRedirect(request.getContextPath() + "/profile");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/remember-login");
        };
    }

    @Bean
    AuthenticationFailureHandler passwordAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            final UriComponentsBuilder redirectBuilder = UriComponentsBuilder
                    .fromPath(request.getContextPath() + SecurityAllowedPaths.LOCAL_LOGIN_PATH)
                    .queryParam(ERROR_PARAMETER, true);
            final String email = request.getParameter(EMAIL_PARAMETER);
            if (email != null && !email.isBlank()) {
                redirectBuilder.queryParam(EMAIL_PARAMETER, email);
            }
            response.sendRedirect(response.encodeRedirectURL(redirectBuilder.build().encode().toUriString()));
        };
    }

    private String clientIp(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }

}
