package org.tubalabs.app.users.settings;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class UserLanguageLocaleConfig {

    @Bean
    LocaleResolver localeResolver() {
        return new UserLanguageLocaleResolver();
    }
}
