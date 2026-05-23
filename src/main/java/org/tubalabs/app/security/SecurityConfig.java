package org.tubalabs.app.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {


    private final List<HttpSecurityCustomizer> customizers;

    public interface HttpSecurityCustomizer {
        void customize(HttpSecurity http);
    }

    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http) {
        final HttpSecurity sec = http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .logout(logout -> logout.logoutSuccessUrl("/login"));

        for (HttpSecurityCustomizer customizer : customizers) {
            customizer.customize(sec);
        }
        sec.authorizeHttpRequests(auth -> auth
                .anyRequest()
                .authenticated());
        return sec.build();
    }


}
