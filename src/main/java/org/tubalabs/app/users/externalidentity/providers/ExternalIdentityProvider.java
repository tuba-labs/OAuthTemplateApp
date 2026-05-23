package org.tubalabs.app.users.externalidentity.providers;

import lombok.NonNull;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;

public interface ExternalIdentityProvider {
    @NonNull
    String providerId();

    @NonNull
    ExternalIdentity getIdentity(@NonNull OAuth2User oauth2User);
}