package org.tubalabs.app.users.externalidentity.providers.github;

import lombok.NonNull;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProvider;

@Component
public class GithubExternalIdentityProvider implements ExternalIdentityProvider {

    public static final String PROVIDER_ID = "github";

    @NonNull
    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @NonNull
    @Override
    public ExternalIdentity getIdentity(@NonNull OAuth2User oauth2User) {
        return ExternalIdentity.builder()
                .providerId(PROVIDER_ID)
                .subject(readString(oauth2User, "id"))
                .displayName(resolveDisplayName(oauth2User))
                .email(readString(oauth2User, "email"))
                .avatarUrl(readString(oauth2User, "avatar_url"))
                .build();
    }

    private String resolveDisplayName(OAuth2User oauth2User) {
        final String name = readString(oauth2User, "name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        return readString(oauth2User, "login");
    }

    private String readString(OAuth2User oauth2User, String attributeName) {
        final Object value = oauth2User.getAttributes().get(attributeName);
        return value == null ? null : value.toString();
    }
}