package org.tubalabs.app.users.identity;

import lombok.NonNull;
import org.tubalabs.app.localization.LocalizationKey;

public enum LoginTypeUnlinkFailure implements LocalizationKey {
    CURRENT_LOGIN("profile.login-types.error.unlink-current-login"),
    LAST_LOGIN_TYPE("profile.login-types.error.unlink-last-login-type"),
    UNKNOWN_CURRENT_LOGIN("profile.login-types.error.unlink-unknown-current-login"),
    NOT_LINKED("profile.login-types.error.unlink-not-linked");

    private final String localizationKey;

    LoginTypeUnlinkFailure(@NonNull String localizationKey) {
        this.localizationKey = localizationKey;
    }

    @Override
    public String getLocalizationKey() {
        return localizationKey;
    }
}
