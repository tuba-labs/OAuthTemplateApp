package org.tubalabs.app.users.settings;

import lombok.Builder;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.UUID;

@Builder(toBuilder = true)
public record UserSettingsDbo(
        @NonNull UUID userId,
        Timestamp rememberLoginPromptAfter,
        @NonNull String languageTag) {

    public UserSettingsDbo(@NonNull UUID userId, Timestamp rememberLoginPromptAfter) {
        this(userId, rememberLoginPromptAfter, UserLanguage.defaultTag());
    }

    public UserSettingsDbo {
        languageTag = UserLanguage.normalizedTagOrDefault(languageTag);
    }
}
