package org.tubalabs.app.users.profile.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.tubalabs.app.users.settings.UserLanguageLocaleInterceptor;

@Configuration
@RequiredArgsConstructor
public class ProfileSetupWebConfig implements WebMvcConfigurer {

    private final @NonNull ProfileSetupInterceptor profileSetupInterceptor;
    private final @NonNull UserLanguageLocaleInterceptor userLanguageLocaleInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(userLanguageLocaleInterceptor);
        registry.addInterceptor(profileSetupInterceptor);
    }
}
