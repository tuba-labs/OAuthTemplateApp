package org.tubalabs.app.users.password.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.tubalabs.app.security.SecurityConfig;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.password.LocalUserService;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PasswordSecurityCustomizer {

    private final LocalUserService localUserService;

    @Bean
    public SecurityConfig.HttpSecurityCustomizer passwordHttpSecurityCustomizer(
            @Qualifier("passwordAuthenticationSuccessHandler") @NonNull AuthenticationSuccessHandler handler) {
        return http -> http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/local", "/register", "/api/local-users", "/api/local-login")
                        .permitAll())
                .formLogin(form -> form
                        .loginPage("/login/local")
                        .loginProcessingUrl("/login/local")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(handler)
                        .failureUrl("/login/local?error")
                        .permitAll());
    }

    @Bean
    AuthenticationSuccessHandler passwordAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            final LoginResult result = localUserService.login(
                    authentication.getName(), clientIp(request), userAgent(request));
            log.info("Logged in user with password: {}", result);

            response.sendRedirect("/");
        };
    }

    private String clientIp(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }

}
