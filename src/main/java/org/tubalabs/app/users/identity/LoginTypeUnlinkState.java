package org.tubalabs.app.users.identity;

import lombok.NonNull;
import org.tubalabs.app.localization.LocalizationKey;

public enum LoginTypeUnlinkState implements LocalizationKey {
    AVAILABLE("profile.login-types.status.available"),
    CURRENT_LOGIN("profile.login-types.status.current-login"),
    LAST_LOGIN_TYPE("profile.login-types.status.last-login-type"),
    UNKNOWN_CURRENT_LOGIN("profile.login-types.status.unknown-current-login");

    private final String localizationKey;

    LoginTypeUnlinkState(@NonNull String localizationKey) {
        this.localizationKey = localizationKey;
    }

    @Override
    public String getLocalizationKey() {
        return localizationKey;
    }
}
