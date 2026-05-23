package org.tubalabs.app.users.externalidentity.providers.google;

import lombok.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProvider;

@Component
public class GoogleExternalIdentityProvider implements ExternalIdentityProvider {

    public static final String PROVIDER_ID = "google";

    @NonNull
    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @NonNull
    @Override
    public ExternalIdentity getIdentity(@NonNull OAuth2User oauth2User) {

        final OidcUser oidcUser = (OidcUser) oauth2User;

        return ExternalIdentity.builder()
                .providerId(PROVIDER_ID)
                .subject(oidcUser.getSubject())
                .displayName(oidcUser.getFullName())
                .email(oidcUser.getEmail())
                .avatarUrl(oidcUser.getPicture())
                .build();
    }
}
