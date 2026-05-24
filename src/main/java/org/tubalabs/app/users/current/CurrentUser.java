package org.tubalabs.app.users.current;

import lombok.NonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record CurrentUser(
        @NonNull UUID userId,
        String displayName,
        String pictureUrl,
        boolean profileSetupRequired,
        String localLoginName,
        boolean localLoginLinkAvailable) implements Serializable {

    private static final String DEFAULT_DISPLAY_NAME = "Profile";

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
    }

    public String navigationDisplayName() {
        if (displayName == null || displayName.isBlank()) {
            return DEFAULT_DISPLAY_NAME;
        }
        return displayName;
    }

    public boolean localProfile() {
        return localLoginName != null && !localLoginName.isBlank();
    }

    public boolean passwordChangeAvailable() {
        return localProfile() && !profileSetupRequired;
    }
}
