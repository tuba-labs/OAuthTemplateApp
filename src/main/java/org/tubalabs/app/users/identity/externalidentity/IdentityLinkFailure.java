package org.tubalabs.app.users.identity.externalidentity;

import lombok.NonNull;
import org.tubalabs.app.localization.LocalizationKey;

public enum IdentityLinkFailure implements LocalizationKey {
    PROVIDER_MISMATCH("profile.login-types.error.provider-mismatch"),
    EXTERNAL_IDENTITY_USED("profile.login-types.error.external-identity-used"),
    PROVIDER_ALREADY_LINKED("profile.login-types.error.provider-already-linked");

    private final String localizationKey;

    IdentityLinkFailure(@NonNull String localizationKey) {
        this.localizationKey = localizationKey;
    }

    @Override
    public String getLocalizationKey() {
        return localizationKey;
    }
}
