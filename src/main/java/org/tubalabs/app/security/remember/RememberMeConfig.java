package org.tubalabs.app.security.remember;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
public class RememberMeConfig {

    public static final String REMEMBER_ME_PARAMETER = "remember-me";
    private static final String REMEMBER_ME_COOKIE_NAME = "remember-me";

    @Bean
    RememberMeServices rememberMeServices(@NonNull RememberedUserDetailsService rememberedUserDetailsService,
                                          @NonNull PersistentTokenRepository persistentTokenRepository,
                                          @NonNull RememberLoginProperties rememberLoginProperties) {
        final PersistentTokenBasedRememberMeServices rememberMeServices =
                new PersistentTokenBasedRememberMeServices(
                        rememberLoginProperties.rememberMeKey(),
                        rememberedUserDetailsService::loadUserByUsername,
                        persistentTokenRepository);
        rememberMeServices.setParameter(REMEMBER_ME_PARAMETER);
        rememberMeServices.setCookieName(REMEMBER_ME_COOKIE_NAME);
        rememberMeServices.setTokenValiditySeconds(rememberLoginProperties.tokenValiditySeconds());
        return rememberMeServices;
    }
}
