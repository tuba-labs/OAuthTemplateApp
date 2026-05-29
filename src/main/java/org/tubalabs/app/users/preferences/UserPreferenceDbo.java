package org.tubalabs.app.users.preferences;

import lombok.NonNull;

import java.util.UUID;

public record UserPreferenceDbo(
        @NonNull UUID userId,
        @NonNull String preferenceKey,
        @NonNull String preferenceValue) {
}
