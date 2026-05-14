package org.tubalabs.app.security.identity;

import lombok.NonNull;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface ExternalIdentityProvider {
    @NonNull
    String providerId();

    @NonNull
    ExternalIdentity getIdentity(@NonNull OAuth2User oauth2User);
}