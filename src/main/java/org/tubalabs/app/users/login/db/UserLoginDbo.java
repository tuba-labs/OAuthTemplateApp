package org.tubalabs.app.users.login.db;

import lombok.Builder;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.UUID;

@Builder(toBuilder = true)
public record UserLoginDbo(
        @NonNull UUID id,
        @NonNull UUID userId,
        @NonNull Timestamp loginTime,
        @NonNull String providerId,
        @NonNull String subject,
        @NonNull String clientIp,
        @NonNull String userAgent) {
}
