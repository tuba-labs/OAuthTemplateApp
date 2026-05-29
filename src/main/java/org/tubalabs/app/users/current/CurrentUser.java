package org.tubalabs.app.users.current;

import lombok.NonNull;
import org.tubalabs.app.users.settings.UserLanguage;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record CurrentUser(
        @NonNull UUID userId,
        String displayName,
        String pictureUrl,
        boolean profileSetupRequired,
        String localLoginName,
        boolean localLoginLinkAvailable,
        @NonNull String languageTag,
        boolean disableBackgroundAnimation) implements Serializable {

    public CurrentUser(@NonNull UUID userId,
                       String displayName,
                       String pictureUrl,
                       boolean profileSetupRequired,
                       String localLoginName,
                       boolean localLoginLinkAvailable) {
        this(
                userId,
                displayName,
                pictureUrl,
                profileSetupRequired,
                localLoginName,
                localLoginLinkAvailable,
                UserLanguage.defaultTag(),
                false);
    }

    public CurrentUser(@NonNull UUID userId,
                       String displayName,
                       String pictureUrl,
                       boolean profileSetupRequired,
                       String localLoginName,
                       boolean localLoginLinkAvailable,
                       @NonNull String languageTag) {
        this(
                userId,
                displayName,
                pictureUrl,
                profileSetupRequired,
                localLoginName,
                localLoginLinkAvailable,
                languageTag,
                false);
    }

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
        languageTag = UserLanguage.normalizedTagOrDefault(languageTag);
    }

    public boolean hasDisplayName() {
        return displayName != null && !displayName.isBlank();
    }

    public boolean localProfile() {
        return localLoginName != null && !localLoginName.isBlank();
    }

    public boolean passwordChangeAvailable() {
        return localProfile() && !profileSetupRequired;
    }

    public Locale locale() {
        return UserLanguage.fromTagOrDefault(languageTag).locale();
    }
}
