package org.tubalabs.app.events;

import lombok.Builder;
import lombok.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record EventLog(
        @NonNull String eventType,
        UUID actorUserId,
        String subjectType,
        String subjectId,
        String clientIp,
        String userAgent,
        @NonNull Map<String, Object> details) {

    public EventLog {
        Objects.requireNonNull(eventType, "eventType");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        userAgent = userAgent == null ? "" : userAgent;
        details = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(details, "details")));
    }
}
