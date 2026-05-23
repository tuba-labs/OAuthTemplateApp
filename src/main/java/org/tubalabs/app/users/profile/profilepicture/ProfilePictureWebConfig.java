package org.tubalabs.app.users.profile.profilepicture;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ProfilePictureWebConfig implements WebMvcConfigurer {

    private final @NonNull ProfilePictureStorageProperties profilePictureStorageProperties;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ProfilePictureStorageService.PUBLIC_URL_PATTERN)
                .addResourceLocations(profilePictureStorageProperties.resourceLocation());
    }
}
