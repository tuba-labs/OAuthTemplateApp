package org.tubalabs.app.users.profile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ProfileSetupWebConfig implements WebMvcConfigurer {

    private final @NonNull ProfileSetupInterceptor profileSetupInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(profileSetupInterceptor);
    }
}
