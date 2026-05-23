package org.tubalabs.app.users.settings;

import lombok.Builder;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.UUID;

@Builder(toBuilder = true)
public record UserSettingsDbo(
        @NonNull UUID userId,
        Timestamp rememberLoginPromptAfter) {
}
