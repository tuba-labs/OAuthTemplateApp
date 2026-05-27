package org.tubalabs.app.events.db;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record EventLogDbo(
        @NonNull UUID id,
        @NonNull String eventType,
        UUID actorUserId,
        String subjectType,
        String subjectId,
        String clientIp,
        @NonNull String userAgent,
        @NonNull String details) {
}
